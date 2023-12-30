package de.rccookie.http.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Arguments;
import de.rccookie.util.Console;
import de.rccookie.util.Future;
import de.rccookie.util.ThreadedFutureImpl;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.Nullable;

class SendableHttpResponse implements HttpResponse.Sendable {

    final HttpExchange connection;
    final HttpRequest request;
    State state = State.EDITABLE;
    ResponseCode code;
    Body body = null;
    Header header = new StatefulHeader(this);

    SendableHttpResponse(HttpExchange connection, @Nullable HttpRequest request, ResponseCode code) {
        this.connection = Arguments.checkNull(connection, "connection");
        this.request = request;
        this.code = Arguments.checkNull(code, "code");
    }

    @Override
    public String toString() {
        return code.toString();
    }

    @Override
    public Sendable setCode(ResponseCode code) {
        checkState();
        this.code = Arguments.checkNull(code, "code");
        return this;
    }

    @Override
    public Sendable setBody(Body body) {
        checkState();
        this.body = body;
        return this;
    }

    @Override
    public ResponseCode code() {
        return code;
    }

    @Override
    public Header header() {
        return header;
    }

    @Override
    public Body body() {
        return body;
    }

    @Override
    public boolean isHttps() {
        return connection.getProtocol().toLowerCase().startsWith("https");
    }

    @Override
    public String version() {
        String protocol = connection.getProtocol();
        return protocol.substring(protocol.indexOf('/'));
    }

    @Override
    public InetSocketAddress client() {
        return connection.getRemoteAddress();
    }

    @Override
    public InetSocketAddress server() {
        return connection.getLocalAddress();
    }

    @Override
    public State state() {
        synchronized(this) {
            return state;
        }
    }

    @SuppressWarnings("NullableProblems") // Don't annotate with @NotNull, IntelliJ causes an exception then if the method still returns null, which is internally possible
    @Override
    public HttpRequest request() {
        return request;
    }

    @Override
    public Future<Void> sendAsync() {
        // Throw exception on calling thread
        checkSend();
        return new ThreadedFutureImpl<>(this::sendBlocking0, connection.getHttpContext().getServer().getExecutor()) { };
    }

    @Override
    public void send() {
        checkSend();
        sendBlocking0();
    }

    private void checkSend() {
        checkState();
        state = State.SENT;
    }

    private Void sendBlocking0() throws HttpSendException {
        connection.getResponseHeaders().putAll(header);
        try(Body body = this.body) { // Always close stream
            if(request != null && request.body() != null)
                request.body().close();

            String method = connection.getRequestMethod(); // Don't use enum because it could be an illegal method

            long length;
            if(body == null || method.equalsIgnoreCase("HEAD") || code == ResponseCode.NO_CONTENT || code == ResponseCode.NOT_MODIFIED)
                length = -1;
            else {
                length = body.contentLength();
                if(length == -1) length = 0;
                else if(length == 0) length = -1;
            }
            connection.sendResponseHeaders(code.code(), length);

            if(body != null && code.type() != ResponseCode.Type.INFORMATIONAL && !method.equalsIgnoreCase("HEAD") && code != ResponseCode.NOT_MODIFIED) {
                try(OutputStream out = connection.getResponseBody()) {
                    body.stream().transferTo(out);
                }
            }
        } catch(Exception e) {
            if(e instanceof IOException && e.getMessage() != null && (
                    e.getMessage().contains("Eine bestehende Verbindung wurde softwaregesteuert") ||
                    e.getMessage().contains("An existing connection was forcibly closed") ||
                    e.getMessage().contains("Broken pipe") ||
                    e.getMessage().contains("closed before"))) {
                Console.warn("Connection closed during send: " + e.getMessage());
            }
            else {
                HttpServer.logResponse(this, true);
                throw new HttpSendException(e);
            }
        }
        HttpServer.logResponse(this, false);
        return null;
    }

    void checkState() {
        synchronized(this) {
            if(state == State.SENT)
                throw new IllegalStateException("Response has already been sent");
            if(state == State.INVALID)
                throw new IllegalStateException("Response has been invalidated because a new response has been requested");
        }
    }

    void invalidate() {
        synchronized(this) {
            if(state == State.SENT)
                throw new IllegalStateException("Response has already been sent");
            state = State.INVALID;
            try {
                if(body != null) body.close();
            } catch(Exception e) {
                throw Utils.rethrow(e);
            }
        }
    }
}

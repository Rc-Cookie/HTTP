package de.rccookie.http.server.raw;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.http.server.HttpSendException;
import de.rccookie.util.Arguments;
import de.rccookie.util.Console;
import de.rccookie.util.Future;
import de.rccookie.util.ThreadedFutureImpl;
import de.rccookie.util.UncheckedException;
import de.rccookie.util.Utils;

class HttpResponseImpl implements HttpResponse.Sendable {

    private static final DateTimeFormatter HTTP_TIME_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT"));

    private final RawHttpServer httpServer;
    final HttpRequest.Received request;
    private final String version;
    private final InetSocketAddress server, client;
    private final Socket socket;
    State state = State.EDITABLE;
    ResponseCode code;
    Body body = Body.EMPTY;
    StatefulHeader header = new StatefulHeader(this);

    HttpResponseImpl(HttpRequestImpl request, ResponseCode code) {
        this(request.httpServer, request, request.httpVersion(), request.server(), request.client(), request.socket, code);
    }

    HttpResponseImpl(RawHttpServer httpServer, HttpRequest.Received request, String version, InetSocketAddress server, InetSocketAddress client, Socket socket, ResponseCode code) {
        this.httpServer = httpServer;
        this.request = request;
        this.version = version;
        this.server = server;
        this.client = client;
        this.socket = socket;
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
        this.body = body != null ? body : Body.EMPTY;
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
        return false;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public InetSocketAddress client() {
        return client;
    }

    @Override
    public InetSocketAddress server() {
        return server;
    }

    @Override
    public synchronized State state() {
        return state;
    }

    @SuppressWarnings("NullableProblems") // Don't annotate with @NotNull, IntelliJ causes an exception then if the method still returns null, which is internally possible
    @Override
    public HttpRequest.Received request() {
        return request;
    }

    @Override
    public Future<Void> sendAsync() {
        // Throw exceptions on calling thread
        beforeSend();
        return new ThreadedFutureImpl<>(this::sendBlocking0, httpServer.executor) { };
    }

    @Override
    public void send() {
        beforeSend();
        sendBlocking0();
    }

    private void beforeSend() {
        checkState();
        request.getResponseConfigurators().accept(this);
        synchronized(this) {
            checkState();
            state = State.SENT;
        }
    }

    private Void sendBlocking0() throws HttpSendException {
        boolean close = true;

        try(Body body = this.body) {

            header.locked = false;
            if(httpServer.name != null)
                header.putIfAbsent("Server", httpServer.name);
            header.putIfAbsent("Date", HTTP_TIME_FORMATTER.format(OffsetDateTime.now()));

            if(version().equals("1.1")) {
                Boolean keepAlive = header.getKeepAlive();
                if(keepAlive != null)
                    close = keepAlive;
                else {
                    close = request == null || request.header().getKeepAlive() == Boolean.FALSE;
                    header.setKeepAlive(!close);
                }
            }

            long length = body.contentLength();
            if(length >= 0)
                header.putIfAbsent("Content-Length", length+"");
            else if(header.containsKey("Transfer-Encoding"))
                header.set("Transfer-Encoding", "chunked, " + header.getString("Transfer-Encoding"));
            else header.set("Transfer-Encoding", "chunked");
            header.locked = true;

            OutputStream rawOut = socket.getOutputStream();

            Console.map("Version", version);

            PrintWriter out = new PrintWriter(rawOut);
            out.write("HTTP/");
            out.write(version());
            out.write(' ');
            out.print(code.code());
            out.print(' ');
            out.print(code.httpName());
            out.print('\r');
            out.print('\n');
            header.forEach((n,vs) -> {
                for(String v : vs) {
                    out.print(n);
                    out.print(':');
                    out.print(' ');
                    out.print(v);
                    out.print('\r');
                    out.print('\n');
                }
            });
            out.print('\r');
            out.print('\n');
            out.flush();

            if(length != 0) {
                try(OutputStream outProxy = length > 0 ? new FixedLengthOutputStream(rawOut, length) : new ChunkedOutputStream(rawOut)) {
                    body.writeTo(outProxy);
                    out.flush();
                } catch(IOException | UncheckedIOException e) {
                    close = true;
                    throw e;
                } catch(RuntimeException e) {
                    close = true;
                    if(e instanceof UncheckedException && e.getCause() instanceof IOException)
                        throw (IOException) e.getCause();
                    throw new RuntimeException("Exception while writing response body to stream", e);
                }
            }

            // Close here if successful to catch potential errors in the main catch clause
            if(close) {
                socket.close();
                close = false;
            }
            else httpServer.handleAsync(socket);

        } catch(Exception e) {
            if(e instanceof IOException && e.getMessage() != null && (
                    e.getMessage().contains("Eine bestehende Verbindung wurde softwaregesteuert") ||
                    e.getMessage().contains("An existing connection was forcibly closed") ||
                    e.getMessage().contains("Broken pipe") ||
                    e.getMessage().contains("closed before"))) {
                Console.warn("Connection closed during send: " + e.getMessage());
            }
            else {
                close = true;
                try {
                    httpServer.logResponse(this, true);
                } catch(Exception f) {
                    Console.error("Error in response logger:");
                    Console.error(f);
                }
                throw new HttpSendException(e);
            }
        } finally {
            header.locked = true;
            if(close) {
                try {
                    socket.close();
                } catch(IOException e) {
                    Console.error("Error closing stream:", e);
                }
            }
        }
        try {
            httpServer.logResponse(this, false);
        } catch(Exception e) {
            Console.error("Error in response logger:");
            Console.error(e);
        }
        return null;
    }

    synchronized void checkState() {
        if(state == State.SENT)
            throw new IllegalStateException("Response has already been sent");
        if(state == State.INVALID)
            throw new IllegalStateException("Response has been invalidated because a new response has been requested");
    }

    synchronized void invalidate() {
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

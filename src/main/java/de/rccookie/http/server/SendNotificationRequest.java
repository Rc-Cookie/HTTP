package de.rccookie.http.server;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.function.Consumer;

import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Arguments;
import de.rccookie.util.Future;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class SendNotificationRequest implements HttpRequest.Received {

    private final Received request;
    private final Consumer<? super HttpResponse.Sendable> beforeSend;

    private Resp response;
    private boolean sent = false;

    SendNotificationRequest(Received request, Consumer<? super HttpResponse.Sendable> beforeSend) {
        this.beforeSend = beforeSend;
        this.request = request;
    }

    @Override
    public String toString() {
        return request.toString();
    }

    @Override
    public URL url() {
        return request.url();
    }

    @Override
    public String httpVersion() {
        return request.httpVersion();
    }

    @Override
    public Method method() {
        return request.method();
    }

    @Override
    public Header header() {
        return request.header();
    }

    @Override
    public Body body() {
        return request.body();
    }

    @Override
    public InetSocketAddress client() {
        return request.client();
    }

    @Override
    public InetSocketAddress server() {
        return request.server();
    }

    @Override
    public @Nullable HttpResponse.Sendable getResponse() {
        return response;
    }

    @Override
    public HttpResponse.Sendable respond(ResponseCode code) {
        return response = new Resp(this, request.respond(code));
    }

    private class Resp implements HttpResponse.Sendable {

        private final SendNotificationRequest request;
        private final Sendable response;

        Resp(SendNotificationRequest request, Sendable response) {
            this.request = request;
            this.response = Arguments.checkNull(response, "response");
        }

        @Override
        public String toString() {
            return response.toString();
        }

        @Override
        public ResponseCode code() {
            return response.code();
        }

        @Override
        public Header header() {
            return response.header();
        }

        @Override
        public Body body() {
            return response.body();
        }

        @Override
        public boolean isHttps() {
            return response.isHttps();
        }

        @Override
        public String version() {
            return response.version();
        }

        @Override
        public InetSocketAddress client() {
            return response.client();
        }

        @Override
        public InetSocketAddress server() {
            return response.server();
        }

        @Override
        public State state() {
            return response.state();
        }

        @Override
        public @NotNull HttpRequest request() {
            return request;
        }

        @Override
        public Sendable setCode(ResponseCode code) {
            response.setCode(code);
            return this;
        }

        @Override
        public Sendable setBody(Body body) {
            response.setBody(body);
            return this;
        }

        @Override
        public Future<Void> sendAsync() {
            synchronized(this) {
                if(sent) throw new IllegalStateException("Response has already been sent");
                sent = true;
            }
            if(response.state() != State.EDITABLE)
                response.sendAsync(); // Force exception
            beforeSend.accept(this);
            return response.sendAsync();
        }

        @Override
        public void send() {
            synchronized(this) {
                if(sent) throw new IllegalStateException("Response has already been sent");
                sent = true;
            }
            if(response.state() != State.EDITABLE)
                response.send(); // Force exception
            beforeSend.accept(this);
            response.send();
        }
    }
}

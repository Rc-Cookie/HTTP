package de.rccookie.http.client;

import java.net.InetSocketAddress;

import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;

final class ReceivedHttpResponse implements HttpResponse {

    private final HttpRequest request;
    private final InetSocketAddress server;
    private final ResponseCode code;
    private final Header header;
    private final Body body;
    private final boolean isHttps;

    ReceivedHttpResponse(HttpRequest request, InetSocketAddress server, ResponseCode code, Header header, Body body, boolean isHttps) {
        this.request = request;
        this.server = Arguments.checkNull(server, "server");
        this.code = Arguments.checkNull(code, "code");
        this.header = Arguments.checkNull(header, "header");
        this.body = body;
        this.isHttps = isHttps;
    }

    @Override
    public String toString() {
        return code.toString();
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
        return isHttps;
    }

    @Override
    public String version() {
        return "1.1";
    }

    @Override
    public InetSocketAddress client() {
        return new InetSocketAddress(0);
    }

    @Override
    public InetSocketAddress server() {
        return server;
    }

    @Override
    public State state() {
        return State.SENT;
    }

    @Override
    public @NotNull HttpRequest request() {
        return request;
    }
}

package de.rccookie.http.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.UnknownHostException;

import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Arguments;
import de.rccookie.util.UncheckedException;
import org.jetbrains.annotations.NotNull;

final class ReceivedHttpResponse implements HttpResponse {

    private final HttpRequest request;
    private final URL url;
    private final ResponseCode code;
    private final Header header;
    private final Body body;
    private final boolean isHttps;

    ReceivedHttpResponse(HttpRequest request, URL url, ResponseCode code, Header header, Body body, boolean isHttps) {
        this.request = request;
        this.url = Arguments.checkNull(url, "url");
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
        try {
            return new InetSocketAddress(InetAddress.getByName(url.getHost()), url.getPort() != -1 ? url.getPort() : url.getDefaultPort());
        } catch(UnknownHostException e) {
            throw new UncheckedException(e);
        }
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

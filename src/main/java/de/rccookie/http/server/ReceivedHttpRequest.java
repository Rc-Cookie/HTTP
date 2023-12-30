package de.rccookie.http.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import com.sun.net.httpserver.HttpExchange;
import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Console;
import de.rccookie.util.URLBuilder;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.Nullable;

class ReceivedHttpRequest implements HttpRequest.Received {

    private final HttpServer server;
    private final HttpExchange connection;
    private final URL url;
    private final String version;
    private final Method method;
    private final Header header;
    private final Body body;
    private final InetSocketAddress self;
    private final InetSocketAddress client;

    SendableHttpResponse response = null;

    public ReceivedHttpRequest(HttpServer server, HttpExchange connection, boolean processPath)  {
        this.server = server;
        this.connection = connection;
        try {
            method = Method.valueOf(connection.getRequestMethod());
        } catch(IllegalArgumentException e) {
            Console.warn("Client sent illegal request method:", connection.getRequestMethod());
            throw new HttpRequestFailure(ResponseCode.METHOD_NOT_ALLOWED, "Illegal HTTP method: "+connection.getRequestMethod(), null, e);
        }
        header = Header.of(connection.getRequestHeaders());
        self = connection.getLocalAddress();
        this.body = Body.of(connection.getRequestBody());

        String protocol = connection.getProtocol();
        int slashIndex = protocol.indexOf('/');
        version = connection.getProtocol().substring(slashIndex+1);

        String host = header.getStringOrDefault("Host", self.toString().substring(1));
        boolean https = protocol.toLowerCase().startsWith("https");
        if(https && host.endsWith(":443"))
            host = host.substring(0, host.length() - 4);
        else if(!https && host.endsWith(":80"))
            host = host.substring(0, host.length() - 3);

        URL url;
        try {
            url = new URL((https ? "https" : "http") + "://" + host + connection.getRequestURI().toASCIIString());//connection.getRequestURI().toURL();
        } catch(MalformedURLException e) {
            throw new HttpSyntaxException("Unable to parse request url", e);
        }
        if(processPath) try {
            this.url = new URLBuilder(url).path(server.pathPreprocessor.apply(url.getPath())).toURL();
        } catch(Exception e) {
            throw Utils.rethrow(e);
        }
        else this.url = url;

        InetSocketAddress client = connection.getRemoteAddress();
        try {
            if(header.containsKey("X-Forwarded-For"))
                client = new InetSocketAddress(InetAddress.getByName((header.get("X-Forwarded-For").get(0))), 0);
            else if(header.containsKey("CF-Connecting-IP"))
                client = new InetSocketAddress(InetAddress.getByName((header.get("CF-Connecting-IP").get(0))), 0);
            else if(header.containsKey("X-Real-IP"))
                client = new InetSocketAddress(InetAddress.getByName((header.get("X-Real-IP").get(0))), 0);
        } catch (UnknownHostException e) {
            Console.warn("Failed to parse 'real ip' header:", e);
        }
        this.client = client;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof HttpRequest)) return false;
        HttpRequest r = (HttpRequest) obj;
        return method == r.method() && url.toString().equals(r.url().toString()) && header.equals(r.header());
    }

    @Override
    public String toString() {
        return method + " " + url;
    }

    @Override
    public URL url() {
        return url;
    }

    @Override
    public String httpVersion() {
        return version;
    }

    @Override
    public Method method() {
        return method;
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
    public InetSocketAddress client() {
        return client;
    }

    @Override
    public InetSocketAddress server() {
        return self;
    }

    @Nullable
    @Override
    public HttpResponse.Sendable getResponse() {
        return response;
    }

    @Override
    public HttpResponse.Sendable respond(ResponseCode code) {
        synchronized(this) {
            if(response != null)
                response.invalidate();
            return response = new SendableHttpResponse(connection, this, code);
        }
    }
}

package de.rccookie.http.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpExchange;
import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.Method;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Arguments;
import de.rccookie.util.Console;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ReceivedHttpRequest implements HttpRequest.Respondable {

    private final RawHttpServer server;
    private final HttpExchange connection;
    private final URL url;
    private final String version;
    private final Method method;
    private final Header header;
    private final Body body;
    private final InetSocketAddress self;
    private final InetSocketAddress client;


    SendableHttpResponse response = null;
    Consumer<HttpResponse.Editable> configurators = null;
    private Map<Class<?>, Object> optionalParams = null;

    public ReceivedHttpRequest(RawHttpServer server, HttpExchange connection)  {
        this.server = server;
        this.connection = connection;
        try {
            method = Method.valueOf(connection.getRequestMethod().toUpperCase());
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

        try {
            this.url = new URL((https ? "https" : "http") + "://" + host + connection.getRequestURI().toASCIIString());//connection.getRequestURI().toURL();
        } catch(MalformedURLException e) {
            throw new HttpSyntaxException("Unable to parse request url", e);
        }

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

    @Override
    public Object serverObject() {
        return server;
    }

    @Nullable
    @Override
    public HttpResponse.Sendable getResponse() {
        return response;
    }

    @Override
    public synchronized HttpResponse.Sendable respond(ResponseCode code) {
        if(response != null)
            response.invalidate();
        return response = new SendableHttpResponse(server, connection, this, code);
    }

    @Override
    public HttpRequest.Respondable invalidateResponse() {
        if(response != null) {
            response.invalidate();
            response = null;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public HttpRequest.Respondable addResponseConfigurator(Consumer<? super HttpResponse.Editable> configurator) {
        Arguments.checkNull(configurator, "configurator");
        synchronized(this) {
            if(configurators == null)
                configurators = (Consumer<HttpResponse.Editable>) configurator;
            else configurators = configurators.andThen(configurator);
        }
        return this;
    }

    @Override
    public @NotNull Consumer<? super HttpResponse.Editable> getResponseConfigurators() {
        return configurators != null ? configurators : r -> { };
    }

    @Override
    public Respondable clearResponseConfigurators() {
        synchronized(this) {
            configurators = null;
        }
        return this;
    }

    @Override
    public <T> Respondable bindOptionalParam(Class<T> type, T value) {
        Arguments.checkNull(type, "type").cast(value);
        synchronized(this) {
            if(optionalParams == null)
                optionalParams = Collections.synchronizedMap(new HashMap<>());
        }
        optionalParams.put(type, value);
        return this;
    }

    @Override
    public <T> T getOptionalParam(Class<T> type) throws NoSuchElementException {
        if(optionalParams == null)
            throw new NoSuchElementException("No optional parameter of type "+type);
        if(!optionalParams.containsKey(type))
            throw new NoSuchElementException("No optional parameter of type "+type);
        return type.cast(optionalParams.get(type));
    }

    @Override
    public boolean hasOptionalParam(Class<?> type) {
        return optionalParams != null && optionalParams.containsKey(type);
    }
}

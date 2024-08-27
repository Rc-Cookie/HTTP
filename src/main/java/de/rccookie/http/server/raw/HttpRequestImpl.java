package de.rccookie.http.server.raw;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.Method;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class HttpRequestImpl implements HttpRequest.Respondable {

    final RawHttpServer httpServer;
    final Socket socket;
    private final URL url;
    private final String version;
    private final Method method;
    private final Header header;
    private final Body body;
    private final InetSocketAddress server;
    private final InetSocketAddress client;


    HttpResponseImpl response = null;
    Consumer<HttpResponse.Editable> configurators = null;
    private Map<Class<?>, Object> optionalParams = null;

    HttpRequestImpl(RawHttpServer httpServer, Socket socket, URL url, String version, Method method, Header header, Body body, InetSocketAddress server, InetSocketAddress client) {
        this.httpServer = httpServer;
        this.socket = socket;
        this.url = url;
        this.version = version;
        this.method = method;
        this.header = header;
        this.body = body;
        this.server = server;
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
        return server;
    }

    @Override
    public Object serverObject() {
        return httpServer;
    }

    @Nullable
    @Override
    public HttpResponse.Sendable getResponse() {
        return response;
    }

    @Override
    public synchronized HttpResponse.Sendable respond(ResponseCode code) {
        Arguments.checkNull(code, "code");
        if(response != null)
            response.invalidate();
        return response = new HttpResponseImpl(this, code);
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

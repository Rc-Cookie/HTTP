package de.rccookie.http.client;

import java.net.URL;
import java.util.function.Function;

import de.rccookie.http.HttpRequest;
import de.rccookie.json.Json;
import de.rccookie.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractHttpClient implements HttpClient {

    static {
        Json.registerDeserializer(AbstractHttpClient.class, json -> (AbstractHttpClient) json.as(HttpClient.class));
    }

    private final HttpClient client;

    public AbstractHttpClient(@Nullable HttpClient client) {
        this.client = client;
    }

    protected abstract HttpRequest.Unsent create(URL url, Function<URL, HttpRequest.Unsent> parentClient);

    protected abstract HttpRequest.Unsent create(String url, Function<String, HttpRequest.Unsent> parentClient);

    @NotNull
    private HttpClient client() {
        if(client != null)
            return client;
        HttpClient def = HttpClient.getDefault();
        if(def == this)
            throw new IllegalStateException("Stack overflow detected: Default http client attempts to delegate to itself");
        return def;
    }

    @Override
    public Object toJson() {
        JsonObject json = new JsonObject("class", getClass(), "client", client());
        toJson(json);
        return json;
    }

    protected abstract void toJson(JsonObject json);

    @Override
    public HttpRequest.Unsent get(URL url) {
        return create(url, client()::get);
    }

    @Override
    public HttpRequest.Unsent get(String url) {
        return create(url, client()::get);
    }

    @Override
    public HttpRequest.Unsent post(String url) {
        return create(url, client()::post);
    }

    @Override
    public HttpRequest.Unsent post(URL url) {
        return create(url, client()::post);
    }

    @Override
    public HttpRequest.Unsent put(String url) {
        return create(url, client()::put);
    }

    @Override
    public HttpRequest.Unsent put(URL url) {
        return create(url, client()::put);
    }

    @Override
    public HttpRequest.Unsent head(String url) {
        return create(url, client()::head);
    }

    @Override
    public HttpRequest.Unsent head(URL url) {
        return create(url, client()::head);
    }

    @Override
    public HttpRequest.Unsent delete(String url) {
        return create(url, client()::delete);
    }

    @Override
    public HttpRequest.Unsent delete(URL url) {
        return create(url, client()::delete);
    }

    @Override
    public HttpRequest.Unsent patch(String url) {
        return create(url, client()::patch);
    }

    @Override
    public HttpRequest.Unsent patch(URL url) {
        return create(url, client()::patch);
    }

    @Override
    public HttpRequest.Unsent options(String url) {
        return create(url, client()::options);
    }

    @Override
    public HttpRequest.Unsent options(URL url) {
        return create(url, client()::options);
    }

    @Override
    public HttpRequest.Unsent trace(String url) {
        return create(url, client()::trace);
    }

    @Override
    public HttpRequest.Unsent trace(URL url) {
        return create(url, client()::trace);
    }

    @Override
    public HttpRequest.Unsent connect(String url) {
        return create(url, client()::connect);
    }

    @Override
    public HttpRequest.Unsent connect(URL url) {
        return create(url, client()::connect);
    }
}

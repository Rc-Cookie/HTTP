package de.rccookie.http.client;

import java.net.URL;
import java.util.function.Function;

import de.rccookie.http.HttpRequest;
import de.rccookie.json.Json;
import de.rccookie.json.JsonObject;
import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A simple http client implementation that wraps an existing client (or
 * the default client) and runs a preprocessor on each created request.
 */
public class SimpleHttpClient extends AbstractHttpClient {

    static {
        Json.registerDeserializer(SimpleHttpClient.class, json -> new SimpleHttpClient(json.get("client").as(HttpClient.class), r -> r));
    }

    private final Function<? super HttpRequest.Unsent, ? extends HttpRequest.Unsent> preprocessor;

    /**
     * Creates a new simple http client.
     *
     * @param client The client to use to create requests. If <code>null</code>,
     *               {@link HttpClient#getDefault()} will used. If the default client
     *               changes later on, this client will adapt to using the new client.
     * @param preprocessor The preprocessor to run on every created request
     */
    public SimpleHttpClient(@Nullable HttpClient client, @NotNull Function<? super HttpRequest.Unsent, ? extends HttpRequest.Unsent> preprocessor) {
        super(client);
        this.preprocessor = Arguments.checkNull(preprocessor, "preprocessor");
    }

    /**
     * Creates a new simple http client using the default http client to create requests.
     * If the default client changes later on, this client will adapt to using the new
     * default client.
     *
     * @param preprocessor The preprocessor to run on every created request
     */
    public SimpleHttpClient(@NotNull Function<? super HttpRequest.Unsent, ? extends HttpRequest.Unsent> preprocessor) {
        this(null, preprocessor);
    }

    @Override
    protected HttpRequest.Unsent create(URL url, Function<URL, HttpRequest.Unsent> parentClient) {
        return preprocessor.apply(parentClient.apply(url));
    }

    @Override
    protected HttpRequest.Unsent create(String url, Function<String, HttpRequest.Unsent> parentClient) {
        return preprocessor.apply(parentClient.apply(url));
    }

    @Override
    protected void toJson(JsonObject json) {
        throw new UnsupportedOperationException("Cannot serialize SimpleHttpClient");
    }
}

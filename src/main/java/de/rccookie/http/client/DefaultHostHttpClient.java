package de.rccookie.http.client;

import java.net.URL;
import java.util.function.Function;

import de.rccookie.http.HttpRequest;
import de.rccookie.json.Json;
import de.rccookie.json.JsonObject;
import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultHostHttpClient extends AbstractHttpClient {

    static {
        Json.registerDeserializer(DefaultHostHttpClient.class, json -> new DefaultHostHttpClient(
                json.get("client").as(HttpClient.class),
                json.getString("urlPrefix")
        ));
    }

    private String urlPrefix;

    public DefaultHostHttpClient(@Nullable HttpClient client, @NotNull String urlPrefix) {
        super(client);
        setUrlPrefix(urlPrefix);
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(@NotNull String urlPrefix) {
        this.urlPrefix = Arguments.checkNull(urlPrefix, "urlPrefix");
    }

    @Override
    protected HttpRequest.Unsent create(URL url, Function<URL, HttpRequest.Unsent> parentClient) {
        return parentClient.apply(url);
    }

    @Override
    protected HttpRequest.Unsent create(String url, Function<String, HttpRequest.Unsent> parentClient) {
        if(url.startsWith("http://") || url.startsWith("https://"))
            return parentClient.apply(url);
        return parentClient.apply(urlPrefix + url);
    }

    @Override
    protected void toJson(JsonObject json) {
        json.put("urlPrefix", urlPrefix);
    }
}

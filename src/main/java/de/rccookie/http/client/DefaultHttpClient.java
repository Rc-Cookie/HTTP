package de.rccookie.http.client;

import java.net.URL;

import de.rccookie.http.HttpRequest;
import de.rccookie.json.Json;
import de.rccookie.json.JsonObject;

/**
 * A default http client implementation using the <code>java.net</code> library.
 */
public class DefaultHttpClient implements HttpClient {

    static {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        Json.registerDeserializer(HttpClient.class, json -> {
            try {
                //noinspection unchecked
                return json.as((Class<? extends HttpClient>) Class.forName(json.getString("class")));
            } catch(ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        Json.registerDeserializer(DefaultHttpClient.class, json -> new DefaultHttpClient());
    }

    public DefaultHttpClient() { }

    @Override
    public HttpRequest.Unsent get(URL url) {
        return new URLConnectionHttpRequest(url);
    }

    @Override
    public Object toJson() {
        return new JsonObject("class", getClass());
    }
}

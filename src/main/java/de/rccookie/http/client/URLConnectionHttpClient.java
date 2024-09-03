package de.rccookie.http.client;

import java.net.URL;

import de.rccookie.http.HttpRequest;
import de.rccookie.json.Json;
import de.rccookie.json.JsonObject;

/**
 * A default http client implementation using the <code>java.net</code> library.
 */
final class URLConnectionHttpClient implements HttpClient {

    static {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        Json.registerDeserializer(HttpClient.class, json -> {
            try {
                String clsName = json.getString("class");
                if(clsName.equals("DefaultHttpClient"))
                    clsName = URLConnectionHttpClient.class.getName();
                //noinspection unchecked
                return json.as((Class<? extends HttpClient>) Class.forName(clsName));
            } catch(ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        Json.registerDeserializer(URLConnectionHttpClient.class, json -> (URLConnectionHttpClient) STD_NET);
    }

    public URLConnectionHttpClient() { }

    @Override
    public HttpRequest.Unsent get(URL url) {
        return new URLConnectionHttpRequest(url);
    }

    @Override
    public Object toJson() {
        return new JsonObject("class", getClass());
    }
}

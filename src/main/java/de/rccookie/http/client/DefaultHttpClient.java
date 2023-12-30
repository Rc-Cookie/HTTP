package de.rccookie.http.client;

import java.net.URL;

import de.rccookie.http.HttpClient;
import de.rccookie.http.HttpRequest;

/**
 * A default http client implementation using the <code>java.net</code> library.
 */
public class DefaultHttpClient implements HttpClient {
    @Override
    public HttpRequest.Unsent get(URL url) {
        return new SendableHttpRequest(url);
    }
}

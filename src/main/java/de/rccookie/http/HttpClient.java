package de.rccookie.http;

import java.net.MalformedURLException;
import java.net.URL;

import de.rccookie.http.client.DefaultHttpClient;
import de.rccookie.util.UncheckedException;

/**
 * A http client is a factory for unsent http requests.
 */
public interface HttpClient {

    /**
     * The default http client for sending requests via the <code>java.net</code> library.
     */
    HttpClient STD_NET = new DefaultHttpClient();



    /**
     * Creates a new, unsent GET request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    HttpRequest.Unsent get(URL url);

    /**
     * Creates a new, unsent GET request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent get(String url) {
        try {
            return get(new URL(url));
        } catch(MalformedURLException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Creates a new, unsent POST request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent post(String url) {
        return get(url).setMethod(HttpRequest.Method.POST);
    }

    /**
     * Creates a new, unsent POST request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent post(URL url) {
        return get(url).setMethod(HttpRequest.Method.POST);
    }

    /**
     * Creates a new, unsent PUT request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent put(String url) {
        return get(url).setMethod(HttpRequest.Method.PUT);
    }

    /**
     * Creates a new, unsent PUT request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent put(URL url) {
        return get(url).setMethod(HttpRequest.Method.PUT);
    }

    /**
     * Creates a new, unsent HEAD request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent head(String url) {
        return get(url).setMethod(HttpRequest.Method.HEAD);
    }

    /**
     * Creates a new, unsent HEAD request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent head(URL url) {
        return get(url).setMethod(HttpRequest.Method.HEAD);
    }

    /**
     * Creates a new, unsent DELETE request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent delete(String url) {
        return get(url).setMethod(HttpRequest.Method.DELETE);
    }

    /**
     * Creates a new, unsent DELETE request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent delete(URL url) {
        return get(url).setMethod(HttpRequest.Method.DELETE);
    }

    /**
     * Creates a new, unsent PATCH request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent patch(String url) {
        return get(url).setMethod(HttpRequest.Method.PATCH);
    }

    /**
     * Creates a new, unsent PATCH request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent patch(URL url) {
        return get(url).setMethod(HttpRequest.Method.PATCH);
    }

    /**
     * Creates a new, unsent OPTIONS request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent options(String url) {
        return get(url).setMethod(HttpRequest.Method.OPTIONS);
    }

    /**
     * Creates a new, unsent OPTIONS request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent options(URL url) {
        return get(url).setMethod(HttpRequest.Method.OPTIONS);
    }

    /**
     * Creates a new, unsent TRACE request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent trace(String url) {
        return get(url).setMethod(HttpRequest.Method.TRACE);
    }

    /**
     * Creates a new, unsent TRACE request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent trace(URL url) {
        return get(url).setMethod(HttpRequest.Method.TRACE);
    }

    /**
     * Creates a new, unsent CONNECT request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent connect(String url) {
        return get(url).setMethod(HttpRequest.Method.CONNECT);
    }

    /**
     * Creates a new, unsent CONNECT request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    default HttpRequest.Unsent connect(URL url) {
        return get(url).setMethod(HttpRequest.Method.CONNECT);
    }



    /**
     * Returns the default http client which is used by the static <code>HttpRequest.get()</code>
     * and similar methods. Can be set using {@link #setDefault(HttpClient)}.
     *
     * @return The current default http client
     */
    static HttpClient getDefault() {
        return HttpClientContainer.getDefault();
    }

    /**
     * Sets the default http client which is used by the static <code>HttpRequest.get()</code>
     * and similar methods.
     *
     * @param client The http client to set as default
     */
    static void setDefault(HttpClient client) {
        HttpClientContainer.setDefault(client);
    }
}

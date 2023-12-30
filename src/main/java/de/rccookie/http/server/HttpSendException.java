package de.rccookie.http.server;

/**
 * Thrown if the http server was unable to send a http response.
 */
public class HttpSendException extends RuntimeException {
    public HttpSendException(Throwable cause) {
        super(cause);
    }
}

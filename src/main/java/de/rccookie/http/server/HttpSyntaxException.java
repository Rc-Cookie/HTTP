package de.rccookie.http.server;

/**
 * Thrown if an incoming http request could not be parsed.
 */
class HttpSyntaxException extends RuntimeException {
    public HttpSyntaxException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

package de.rccookie.http.server;

public class IllegalHttpRequestListenerException extends RuntimeException {
    public IllegalHttpRequestListenerException(String msg) {
        super(msg);
    }
}

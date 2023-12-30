package de.rccookie.http.server;

/**
 * Exceptions deriving from this class will <i>not</i> be handled as internal
 * server error when thrown, but instead are used for control flow, for example
 * using {@link HttpRedirect} or {@link HttpRequestFailure}.
 */
public class HttpControlFlowException extends RuntimeException {
    HttpControlFlowException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

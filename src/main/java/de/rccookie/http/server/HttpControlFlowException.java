package de.rccookie.http.server;

import de.rccookie.http.HttpResponse;

/**
 * Exceptions deriving from this class will <i>not</i> be handled as internal
 * server error when thrown, but instead are used for control flow, for example
 * using {@link HttpRedirect} or {@link HttpRequestFailure}.
 */
public abstract class HttpControlFlowException extends RuntimeException {
    HttpControlFlowException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public abstract void format(HttpResponse.Editable response);
}

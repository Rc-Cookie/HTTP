package de.rccookie.http.server;

public class IllegalHttpProcessorException extends RuntimeException {

    public IllegalHttpProcessorException() {
        super();
    }

    public IllegalHttpProcessorException(String message) {
        super(message);
    }

    public IllegalHttpProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalHttpProcessorException(Throwable cause) {
        super(cause);
    }

    protected IllegalHttpProcessorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

package de.rccookie.http.server;

/**
 * Thrown to indicate that a {@link Parser} does not have a parameterless constructor.
 */
public class BadHttpParserConstructor extends RuntimeException {

    public BadHttpParserConstructor(String message) {
        super(message);
    }

    public BadHttpParserConstructor(String message, Throwable cause) {
        super(message, cause);
    }
}

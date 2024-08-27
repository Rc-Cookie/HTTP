package de.rccookie.http;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;

public class UnexpectedResponseCodeException extends RuntimeException {

    public final HttpResponse response;

    public UnexpectedResponseCodeException(@NotNull HttpResponse response) {
        super("Server returned unexpected response code: " + Arguments.checkNull(response, "response").code() + ":\n" + response.text());
        this.response = response;
    }
}

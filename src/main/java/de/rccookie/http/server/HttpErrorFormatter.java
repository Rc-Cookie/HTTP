package de.rccookie.http.server;

import de.rccookie.http.HttpResponse;
import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;

/**
 * An error formatter generates an appropriate response message for a given
 * {@link HttpRequestFailure}.
 */
public interface HttpErrorFormatter {

    /**
     * A default formatter which returns a json or xml error information.
     */
    HttpErrorFormatter DEFAULT = DefaultErrorFormatter.INSTANCE;

    /**
     * Sets the response to the default error message format, but <b>does not</b>
     * send the response.
     *
     * @param response The response to be formatted to an error response
     * @param failure The error details to be included in the response
     */
    void format(HttpResponse.Editable response, @NotNull HttpRequestFailure failure);

    /**
     * Returns an error formatter that first applies this formatter, and then the given
     * formatter.
     *
     * @param formatter The formatter to apply after this one
     * @return A formatter performing first this and then the given formatter
     */
    default HttpErrorFormatter and(HttpErrorFormatter formatter) {
        Arguments.checkNull(formatter, "formatter");
        return (r,f) -> {
            format(r,f);
            formatter.format(r,f);
        };
    }
}

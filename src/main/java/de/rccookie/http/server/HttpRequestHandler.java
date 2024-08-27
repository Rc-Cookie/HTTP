package de.rccookie.http.server;

import de.rccookie.http.HttpRequest;

/**
 * A http request handler handles incoming http requests for specific paths and
 * request methods.
 */
public interface HttpRequestHandler {

    /**
     * Responds to the given http request synchronously, that is, it configures the
     * response for the request using one of the {@link HttpRequest.Received#respond()}
     * methods.
     *
     * @param request The request to respond to
     * @throws HttpRequestFailure A failure which will be used to create and send an error
     *                            response automatically, using the configured error response
     *                            formatter. The default formatter returns a json response
     *                            with the response code, message and detail but without
     *                            the exception and exception stacktrace.
     * @throws HttpRedirect If a redirect response should be sent
     * @throws Exception Other exceptions will be caught and handled by the configured error
     *                   handler. By default, this causes an {@link HttpRequestFailure} to be
     *                   thrown with the error code <code>500 INTERNAL SERVER ERROR</code> and
     *                   the exception as cause, which then gets handled by the configured
     *                   error response handler (the default formatter does not include the
     *                   causing exception in the response).
     */
    void respond(HttpRequest.Received request) throws Exception;
}

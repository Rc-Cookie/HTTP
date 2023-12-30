package de.rccookie.http.server;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;

/**
 * A http request handler handles incoming http requests for specific paths and
 * request methods.
 */
public interface HttpRequestHandler {

    /**
     * Responds to the given http request synchronously, that is the response needs to be ready
     * for or have started to be transferred to the client when the method returns. The handler
     * <i>can, but does not have to</i> call the {@link HttpResponse.Sendable#send()} or
     * {@link HttpResponse.Sendable#sendAsync()} method itself, either way it must have called
     * {@link HttpRequest.Received#respond()} and configured the response to a way that should
     * be sent. If the handler does not call <code>send()</code> or <code>sendAsync()</code>
     * itself, it will be done automatically after calling the handler.
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

    /**
     * Responds to the given http request, synchronously or asynchronously. The handler is
     * responsible for calling {@link HttpResponse.Sendable#send()} or {@link HttpResponse.Sendable#sendAsync()}
     * eventually, otherwise the request will not be answered.
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
    default void respondAsync(HttpRequest.Received request) throws Exception {

        respond(request);

        HttpResponse.Sendable response = request.getResponse();
        if(response == null)
            throw new IllegalStateException("Non-async handler didn't write response");
        if(response.state() == HttpResponse.State.EDITABLE)
            response.send();
    }


    interface Async extends HttpRequestHandler {
        @Override
        default void respond(HttpRequest.Received request) throws Exception {
            respondAsync(new SendNotificationRequest(request, $ -> notifyAll()));
            synchronized(this) {
                while(request.getResponse() == null || request.getResponse().state() != HttpResponse.State.SENT)
                    wait();
            }
        }

        @Override
        void respondAsync(HttpRequest.Received request) throws Exception;
    }
}

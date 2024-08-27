package de.rccookie.http.server;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;

/**
 * Represents one step in processing an http request. Multiple processors can run "on top of"
 * each other, meaning the first calls the second, the second calls the third etc. Each processor
 * can perform an action before the main http handler responds to the request, and after it responded
 * to the request, possibly with an exception. Note that the http processors are usually not intended
 * to perform the response to the request, that is the task of the http handler. The processor is
 * mostly indented for pre-checking (e.g. login) or modifications to the configured response, e.g.
 * adding some additional headers.
 */
public interface HttpProcessor {

    /**
     * Called before the http handler is executed. If this method throws an exception, the
     * handler will not be executed.
     * <p>The default implementation does nothing.</p>
     *
     * @param request The request to pre-process
     * @throws Exception Same rules as for a http handler: instances of {@link HttpControlFlowException}
     *                   will result in specific responses for them, all other exceptions will
     *                   cause a 500 Internal Server Error response.
     */
    default void preprocess(HttpRequest.Received request) throws Exception { }

    /**
     * Called after the http handler is executed. If this method throws an exception, the previous
     * http processor (if any) may process this exception. Otherwise, the configured response may
     * be discarded.
     * <p>The default implementation does nothing.</p>
     *
     * @param response The response configured by the http handler to post-process
     * @throws Exception Same rules as for a http handler: instances of {@link HttpControlFlowException}
     *                   will result in specific responses for them, all other exceptions will
     *                   cause a 500 Internal Server Error response.
     */
    default void postprocess(HttpResponse.Editable response) throws Exception { }

    /**
     * Called if the http handler throws an {@link HttpControlFlowException}, e.g. to perform a redirect.
     * Exceptions thrown in the {@link #preprocess(HttpRequest.Received)} method will not be caught by
     * this method! If this method catches the exception and does not rethrow it, it must then configure
     * the response to the request.
     * <p>The default implementation simply rethrows the given exception.</p>
     *
     * @param request The request for which the http request handler threw a control flow exception
     * @param flow The control flow exception thrown by the handler
     * @throws Exception Same rules as for a http handler: instances of {@link HttpControlFlowException}
     *                   will result in specific responses for them, all other exceptions will
     *                   cause a 500 Internal Server Error response.
     */
    default void processControlFlow(HttpRequest.Received request, HttpControlFlowException flow) throws Exception {
        throw flow;
    }

    /**
     * Called if the http handler throws an exception that is not a control flow exception, i.e. an
     * internal error occurred. Exceptions thrown in the {@link #preprocess(HttpRequest.Received)}
     * method will not be caught by this method! If this method catches the exception and does not
     * rethrow it, it must then configure the response to the request.
     * <p>The default implementation simply rethrows the given exception.</p>
     *
     * @param request The request for which the http request handler threw an exception
     * @param exception The exception thrown by the handler
     * @throws Exception Same rules as for a http handler: instances of {@link HttpControlFlowException}
     *                   will result in specific responses for them, all other exceptions will
     *                   cause a 500 Internal Server Error response.
     */
    default void processError(HttpRequest.Received request, Exception exception) throws Exception {
        throw exception;
    }

    /**
     * Completely processes the given request, performing and preprocessor and/or postprocessing in
     * the process. The method must call the given runnable to execute the actual http handler. If
     * the method returns normally (not throwing some kind of exception), it must ensure the response
     * is configured.
     * <p>The default implementation calls the other methods of this interface according to their
     * described rules, and in the process executes the http handler. If a class overrides this method,
     * the other methods will not be called anymore, except called explicitly or using the super method.</p>
     *
     * @param request The request to process
     * @param runHandler By calling this function, the main http handler (and any other http processors)
     *                   will be executed, possibly throwing a control flow exception or regular exception.
     *                   This should usually be executed exactly once. Iff this function returns normally
     *                   (without throwing some kind of exception), the response of the request will be
     *                   configured (but may still be modified or replaced).
     * @throws Exception Same rules as for a http handler: instances of {@link HttpControlFlowException}
     *                   will result in specific responses for them, all other exceptions will
     *                   cause a 500 Internal Server Error response.
     */
    default void process(HttpRequest.Received request, ThrowingRunnable runHandler) throws Exception {
        preprocess(request);
        try {
            runHandler.run();
        } catch(HttpControlFlowException flow) {
            processControlFlow(request, flow);
            return;
        } catch(Exception e) {
            processError(request, e);
            return;
        }
        postprocess(request.getResponse());
    }
}

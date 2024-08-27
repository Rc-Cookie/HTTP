package de.rccookie.http.server;

import de.rccookie.http.HttpRequest;
import de.rccookie.util.Arguments;
import de.rccookie.util.Console;

final class HandlerProcessor implements HttpProcessor {

    private final HttpRequestHandler handler;

    public HandlerProcessor(HttpRequestHandler handler) {
        this.handler = Arguments.checkNull(handler, "handler");
    }

    @Override
    public void process(HttpRequest.Received request, ThrowingRunnable runHandler) throws Exception {
        try {
            handler.respond(request);
            if(request.getResponse() == null)
                throw new IllegalStateException("Http handler did not write response");
        } catch(Exception e) {
            if(request.getResponse() != null && e instanceof HttpControlFlowException)
                Console.warn("Http handler configured response but threw control flow exception. Response will be discarded");
            request.invalidateResponse();
            throw e;
        }
    }
}

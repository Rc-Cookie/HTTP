package de.rccookie.http;

import de.rccookie.http.server.HttpRequestHandler;
import de.rccookie.util.Arguments;

class RequestHandlerWrapper {

    private final HttpRequestHandler handler;

    RequestHandlerWrapper(HttpRequestHandler handler) {
        this.handler = Arguments.checkNull(handler, "handler");
    }

    public void respondAsync(HttpRequest.Received request) {

    }
}

package de.rccookie.http.server;

import java.util.ArrayDeque;
import java.util.Deque;

import de.rccookie.http.HttpRequest;

public final class CurrentHttpServerContext {

    private CurrentHttpServerContext() { }


    private static final ThreadLocal<Deque<HttpRequest.Received>> currentRequest = ThreadLocal.withInitial(ArrayDeque::new);


    public static HttpRequest.Received request() {
        Deque<HttpRequest.Received> current = currentRequest.get();
        if(current.isEmpty())
            throw new IllegalStateException("No http request in process");
        HttpRequest.Received request = current.getFirst();
        if(request == null)
            throw new IllegalStateException("Current http request is not valid during async http requests");
        return current.getFirst();
    }

    static void pushRequest(HttpRequest.Received request) {
        currentRequest.get().push(request);
    }

    static void popRequest() {
        currentRequest.get().pop();
    }
}

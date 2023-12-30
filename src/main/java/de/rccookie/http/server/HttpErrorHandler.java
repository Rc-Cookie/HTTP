package de.rccookie.http.server;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.util.Console;

public interface HttpErrorHandler {

    HttpErrorHandler DEFAULT = (request, exception) -> {
        throw HttpRequestFailure.internal(exception);
    };

    void respondToError(HttpRequest.Received request, Exception exception);


    default void respondToErrorAsync(HttpRequest.Received request, Exception exception) {

        respondToError(request, exception);

        HttpResponse.Sendable response = request.getResponse();
        if(response == null)
            throw new IllegalStateException("Non-async exception handler didn't write response");
        if(response.state() == HttpResponse.State.EDITABLE)
            response.send();
    }


    interface Async extends HttpErrorHandler {
        @Override
        default void respondToError(HttpRequest.Received request, Exception exception) {
            respondToErrorAsync(new SendNotificationRequest(request, $ -> notifyAll()), exception);
            synchronized(this) {
                while(request.getResponse() == null || request.getResponse().state() != HttpResponse.State.SENT) try {
                    wait();
                } catch(InterruptedException e) {
                    Console.error(e);
                }
            }
        }

        @Override
        void respondToErrorAsync(HttpRequest.Received request, Exception exception);
    }
}

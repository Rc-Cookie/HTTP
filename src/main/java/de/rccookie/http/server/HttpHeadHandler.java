package de.rccookie.http.server;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.Method;

public interface HttpHeadHandler {

    HttpHeadHandler DEFAULT = gh -> r -> gh.respond(new SendNotificationRequest((HttpRequest.Respondable) r, resp -> resp.setBody(null)) {
        @Override
        public Method method() {
            return Method.GET;
        }
    });

    HttpRequestHandler getHandler(HttpRequestHandler getHandler);

    default boolean listHeadWithGet() {
        return true;
    }
}

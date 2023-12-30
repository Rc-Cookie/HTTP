package de.rccookie.http.server;

public interface HttpHeadHandler {

    HttpHeadHandler DEFAULT = gh -> r -> gh.respond(new SendNotificationRequest(r, resp -> resp.setBody(null)) {
        @Override
        public Method method() {
            return Method.GET;
        }
    });

    HttpRequestHandler getHandler(HttpRequestHandler getHandler);
}

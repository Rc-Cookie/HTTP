package de.rccookie.http.server.session;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import org.jetbrains.annotations.Nullable;

public interface Authenticator<T> {

    @Nullable
    T getSessionFromAuth(HttpRequest.Received request);

    void configure401(HttpResponse.Editable response);
}

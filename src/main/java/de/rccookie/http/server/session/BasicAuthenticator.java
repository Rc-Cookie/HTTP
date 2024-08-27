package de.rccookie.http.server.session;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.auth.BasicChallenge;
import de.rccookie.util.login.Login;
import org.jetbrains.annotations.Nullable;

public abstract class BasicAuthenticator<T> implements Authenticator<T> {

    private final String realm;

    public BasicAuthenticator(@Nullable String realm) {
        this.realm = realm;
    }

    public BasicAuthenticator() {
        this(null);
    }

    @Override
    public T getSessionFromAuth(HttpRequest.Received request) {
         Login credentials = request.header().getBasicAuth();
         return credentials != null ? getSession(credentials.username, credentials.password) : null;
    }

    protected abstract T getSession(String username, String password);

    @Override
    public void configure401(HttpResponse.Editable response) {
        response.header().addAuthenticate(new BasicChallenge(realm, null));
    }
}

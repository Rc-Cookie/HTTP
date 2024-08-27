package de.rccookie.http.server.session;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import de.rccookie.http.Cookie;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleSessionManager<T> extends AbstractSessionManager<UUID, T> {

    protected final String cookieName;
    protected final Function<? super HttpRequest.Received, ? extends T> sessionCreator;
    private boolean subDomains = false;


    public SimpleSessionManager(SessionStorage<UUID, T> storage, String cookieName, Function<? super HttpRequest.Received, ? extends T> sessionCreator) {
        super(storage);
        this.cookieName = Arguments.checkNull(cookieName, "cookieName");
        this.sessionCreator = Arguments.checkNull(sessionCreator, "sessionCreator");
    }

    public SimpleSessionManager(SessionStorage<UUID, T> storage, String cookieName, Supplier<? extends T> sessionCreator) {
        this(storage, cookieName, r -> sessionCreator.get());
        Arguments.checkNull(sessionCreator, "sessionCreator");
    }

    public SimpleSessionManager(SessionStorage<UUID, T> storage, Function<? super HttpRequest.Received, ? extends T> sessionCreator) {
        this(storage, "session", sessionCreator);
    }

    public SimpleSessionManager(SessionStorage<UUID, T> storage, Supplier<? extends T> sessionCreator) {
        this(storage, "session", sessionCreator);
    }


    public boolean isSubDomains() {
        return subDomains;
    }

    public void setSubDomains(boolean subDomains) {
        this.subDomains = subDomains;
    }


    @Override
    protected T newSession(HttpRequest.Received request) {
        return sessionCreator.apply(request);
    }

    @Override
    protected @Nullable UUID readKey(HttpRequest.Received request) {
        Cookie cookie = request.cookies().get(cookieName);
        if(cookie != null) try {
            return UUID.fromString(cookie.value());
        } catch(IllegalArgumentException ignored) { }
        return null;
    }

    @Override
    protected void writeKey(HttpResponse.Editable response, UUID key) {
        Cookie.Builder cookie = Cookie.create(cookieName, key.toString())
                .httpOnly(true)
                .secure(response.isHttps())
                .domain(subDomains ? "."+response.request().host() : null);
        if(storage.ttl() < Long.MAX_VALUE)
            cookie.maxAge(storage.ttl());
        response.addCookie(cookie);
    }

    @Override
    protected @NotNull UUID generateKey(T session) {
        return UUID.randomUUID();
    }
}

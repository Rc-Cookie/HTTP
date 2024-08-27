package de.rccookie.http.server.session;

import java.net.URL;
import java.util.UUID;
import java.util.function.Function;

final class NoTypeSpecified extends LoginSessionManager<Object> {
    private NoTypeSpecified(SessionStorage<UUID, Object> storage, String cookieName, Function<? super URL, ? extends String> redirect) {
        super(storage, cookieName, redirect);
    }
}

package de.rccookie.http.server.session;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.server.CurrentHttpServerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SessionManager<T> {

    @NotNull
    T getSession(HttpRequest.Received request);

    default T currentSession() {
        return getSession(CurrentHttpServerContext.request());
    }

    @Nullable
    T getSessionIfPresent(HttpRequest.Received request);

    @Nullable
    default T currentSessionIfPresent() {
        return getSessionIfPresent(CurrentHttpServerContext.request());
    }

    @NotNull
    T createSession(@NotNull T session, HttpRequest.Received context);

    @NotNull
    default T createSession(@NotNull T session) {
        return createSession(session, CurrentHttpServerContext.request());
    }

    void deleteSession(HttpRequest.Received request);

    default void deleteCurrentSession() {
        deleteSession(CurrentHttpServerContext.request());
    }
}

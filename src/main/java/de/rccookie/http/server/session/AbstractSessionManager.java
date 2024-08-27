package de.rccookie.http.server.session;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.util.BoolWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSessionManager<K,T> implements SessionManager<T> {

    protected final SessionStorage<K,T> storage;

    public AbstractSessionManager(SessionStorage<K,T> storage) {
        this.storage = storage;
    }

    @Override
    public @NotNull T getSession(HttpRequest.Received request) {
        K key = readKey(request);
        if(key == null)
            return createSession(newSession(request), request);
        BoolWrapper created = new BoolWrapper(false);
        T session = storage.getOrAdd(key, k -> {
            created.value = true;
            return createSession(newSession(request), request);
        });
        if(!created.value)
            request.addResponseConfigurator(r -> writeKey(r, key));
        return session;
    }

    @Override
    public @Nullable T getSessionIfPresent(HttpRequest.Received request) {
        K key = readKey(request);
        if(key == null) return null;
        T session = storage.get(key);
        if(session == null) return null;
        request.addResponseConfigurator(r -> writeKey(r, key));
        return session;
    }

    @Override
    public @NotNull T createSession(@NotNull T session, HttpRequest.Received context) {
        K key = generateKey(session);
        storage.add(key, session);
        context.addResponseConfigurator(r -> writeKey(r, key));
        return session;
    }

    protected abstract T newSession(HttpRequest.Received request);

    @Nullable
    protected abstract K readKey(HttpRequest.Received request);

    protected abstract void writeKey(HttpResponse.Editable response, K key);

    @NotNull
    protected abstract K generateKey(T session);

    @Override
    public void deleteSession(HttpRequest.Received request) {
        K key = readKey(request);
        if(key != null)
            storage.delete(key);
    }
}

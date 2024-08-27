package de.rccookie.http.server.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MemorySessionStorage<K,S> implements SessionStorage<K,S> {

    private final Map<K,S> storage = Collections.synchronizedMap(new HashMap<>());

    @Override
    @NotNull
    public S getOrAdd(@NotNull K key, Function<K, S> sessionGenerator) {
        return storage.computeIfAbsent(Arguments.checkNull(key, "key"), sessionGenerator);
    }

    @Override
    public S get(@Nullable K key) {
        return storage.get(key);
    }

    @Override
    public void add(@NotNull K key, S session) {
        Arguments.checkNull(key, "key");
        synchronized(storage) {
            if(storage.containsKey(key))
                throw new IllegalStateException("Key already exists");
            storage.put(key, session);
        }
    }

    @Override
    public boolean contains(K key) {
        return storage.containsKey(key);
    }

    @Override
    public S delete(K key) {
        return storage.remove(key);
    }

    @Override
    public long ttl() {
        return Long.MAX_VALUE;
    }
}

package de.rccookie.http.server.session;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SessionStorage<K,S> {

    @NotNull
    S getOrAdd(@NotNull K key, Function<K,S> sessionGenerator);

    S get(@Nullable K key);

    void add(@NotNull K key, S session);

    boolean contains(K key);

    S delete(K key);

    /**
     * Returns the time to live for a storage entry in seconds, or {@link Long#MAX_VALUE}.
     *
     * @return The TTL in seconds
     */
    long ttl();
}

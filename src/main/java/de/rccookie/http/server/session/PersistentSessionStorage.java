package de.rccookie.http.server.session;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import de.rccookie.json.TypeBuilder;
import de.rccookie.util.Arguments;
import de.rccookie.util.Console;
import de.rccookie.util.persistent.JsonPersistentMap;
import de.rccookie.util.persistent.PersistentMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PersistentSessionStorage<K,S> implements SessionStorage<K,S> {

    private final PersistentMap<Object, Entry<S>> data;
    private final Function<? super K, ?> keySerializer;
    private final long ttl;
    private final boolean updateTTLOnRead;
    private final Consumer<? super S> deleteListener;


    private PersistentSessionStorage(Path file, Type keyType, Type sessionType, Function<? super K, ?> keySerializer, long ttl, boolean updateTTLOnRead, long cleanupInterval, Consumer<? super S> deleteListener) {
        this.data = new JsonPersistentMap<>(file, keyType, TypeBuilder.generic(Entry.class, sessionType));
        this.keySerializer = Arguments.checkNull(keySerializer, "keySerializer");
        this.ttl = ttl;
        this.updateTTLOnRead = updateTTLOnRead;
        this.deleteListener = Arguments.checkNull(deleteListener, "deleteListener");
        if(ttl != Long.MAX_VALUE) {
            Thread daemon = new Thread(() -> {
                while(true) {
                    try {
                        //noinspection BusyWait
                        Thread.sleep(cleanupInterval);
                    } catch(InterruptedException e) {
                        Console.error(e);
                    }
                    try {
                        ttlCleanup();
                    } catch(Exception e) {
                        Console.error("Error in TTL cleanup:");
                        Console.error(e);
                    }
                }
            }, file+" TTL cleanup daemon");
            daemon.setDaemon(true);
            daemon.start();
        }
        ttlCleanup();
    }

    private Object serialize(K key) {
        return key != null ? keySerializer.apply(key) : null;
    }

    private void runDeleteListener(S deleted) {
        try {
            deleteListener.accept(deleted);
        } catch(RuntimeException e) {
            Console.error("Error in session delete listener:");
            Console.error(e);
        }
    }

    @Override
    public @NotNull S getOrAdd(@NotNull K key, Function<K,S> sessionGenerator) {
        Object serialKey = Arguments.checkNull(serialize(Arguments.checkNull(key, "key")), "<serialized key>");
        if(ttl == Long.MAX_VALUE)
            return data.computeIfAbsent(serialKey, k -> new Entry<>(Objects.requireNonNull(sessionGenerator.apply(key), "Session generator returned null"))).value;
        return data.compute(serialKey, (k,e) -> {
            if(e == null)
                return new Entry<>(Objects.requireNonNull(sessionGenerator.apply(key), "Session generator returned null"));
            if(System.currentTimeMillis() - e.lastUpdate > ttl) {
                runDeleteListener(e.value);
                return new Entry<>(e.value);
            }
            if(updateTTLOnRead)
                e.lastUpdate = System.currentTimeMillis();
            return e;
        }).value;
    }

    public boolean ttlCleanup() {
        return ttl != Long.MAX_VALUE && data.values().removeIf(entry -> {
            if(System.currentTimeMillis() - entry.lastUpdate <= ttl) return false;
            runDeleteListener(entry.value);
            return true;
        });
    }

    @Override
    public S get(@Nullable K key) {
        Object serialKey = serialize(key);
        if(serialKey == null) return null;
        if(ttl == Long.MAX_VALUE) {
            Entry<S> entry = data.get(serialKey);
            return entry != null ? entry.value : null;
        }
        return data.writeLocked(d -> {
            Entry<S> entry = d.get(serialKey);
            if(entry == null) return null;
            if(System.currentTimeMillis() - entry.lastUpdate > ttl) {
                d.remove(serialKey);
                runDeleteListener(entry.value);
                return null;
            }
            if(updateTTLOnRead)
                entry.lastUpdate = System.currentTimeMillis();
            return entry.value;
        });
    }

    @Override
    public void add(@NotNull K key, S session) {
        Object serialKey = Arguments.checkNull(serialize(key), "key");
        data.merge(serialKey, new Entry<>(session), (k,v) -> { throw new IllegalStateException("Key already exists"); });
    }

    @Override
    public boolean contains(K key) {
        Object serialKey = serialize(key);
        return serialKey != null && data.containsKey(serialKey);
    }

    @Override
    public S delete(K key) {
        Object serialKey = serialize(key);
        if(serialKey == null) return null;
        Entry<S> old = data.remove(serialKey);
        if(old == null) return null;
        runDeleteListener(old.value);
        return System.currentTimeMillis() - old.lastUpdate > ttl ? null : old.value;
    }

    @Override
    public long ttl() {
        return ttl / 1000;
    }

    public static <K,S> Builder<K,S> builder(Class<S> sessionType) {
        return new Builder<>(sessionType);
    }

    public static <K,S> Builder<K,S> builder(Type sessionType) {
        return new Builder<>(sessionType);
    }

    public static class Builder<K,S> {
        private final Type sessionType;
        private Type keyType = String.class;
        private Function<? super K, ?> keySerializer = Object::toString;
        private long ttl = Long.MAX_VALUE;
        private boolean updateTTLOnRead = true;
        private long cleanupInterval = 1000 * 60;
        private Path file = Path.of("sessions");
        private Consumer<? super S> deleteListener = $ -> { };

        public Builder(Type sessionType) {
            this.sessionType = Arguments.checkNull(sessionType, "sessionType");
        }

        @SuppressWarnings("TypeParameterHidesVisibleType")
        public <K> Builder<K,S> keyType(Class<K> keyType) {
            return keyType((Type) keyType);
        }

        @SuppressWarnings({"unchecked", "TypeParameterHidesVisibleType"})
        public <K> Builder<K,S> keyType(Type keyType) {
            this.keyType = Arguments.checkNull(keyType, "keyType");
            keySerializer = k -> k;
            return (Builder<K,S>) this;
        }

        public Builder<K,S> keySerializer(Function<? super K, ? extends String> keySerializer) {
            this.keySerializer = Arguments.checkNull(keySerializer, "keySerializer");
            keyType = String.class;
            return this;
        }

        public Builder<K,S> ttl(int seconds) {
            this.ttl = seconds > 0 ? seconds * 1000L : Long.MAX_VALUE;
            return this;
        }

        public Builder<K,S> updateTTLOnRead(boolean update) {
            this.updateTTLOnRead = update;
            return this;
        }

        public Builder<K,S> cleanupInterval(int seconds) {
            this.cleanupInterval = Arguments.checkRange(seconds, 1, null) * 1000L;
            return this;
        }

        public Builder<K,S> file(Path file) {
            this.file = Arguments.checkNull(file, "file");
            return this;
        }

        public Builder<K,S> file(String file) {
            return file(Path.of(file));
        }

        public Builder<K,S> deleteListener(Consumer<? super K> listener) {
            this.deleteListener = deleteListener != null ? deleteListener : $ -> { };
            return this;
        }

        public PersistentSessionStorage<K,S> build() {
            return new PersistentSessionStorage<>(
                    file,
                    keyType,
                    sessionType,
                    keySerializer,
                    ttl,
                    updateTTLOnRead,
                    cleanupInterval,
                    deleteListener
            );
        }
    }

    private static final class Entry<T> {
        T value;
        long lastUpdate;
        Entry(T value) {
            this.value = value;
            lastUpdate = System.currentTimeMillis();
        }
    }
}

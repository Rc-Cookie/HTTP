package de.rccookie.http.server;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;

public class ServerLocal<T> {

    public final Function<Object, ? extends T> defaultValue;

    private final Map<Object, T> values = Collections.synchronizedMap(new WeakHashMap<>());


    public ServerLocal(Function<@NotNull Object, ? extends T> defaultValue) {
        this.defaultValue = Arguments.checkNull(defaultValue, "defaultValue");
    }

    public <S> ServerLocal(Class<S> serverType, Function<? super @NotNull S, ? extends T> defaultValue) {
        this(o -> {
            if(!serverType.isInstance(o))
                throw new UnsupportedOperationException("Unsupported server type: "+o.getClass());
            return defaultValue.apply(serverType.cast(o));
        });
    }

    public ServerLocal(Supplier<? extends T> defaultValue) {
        this(s -> defaultValue.get());
    }

    public ServerLocal() {
        this(s -> null);
    }


    public T get(Object server) {
        return values.computeIfAbsent(server, defaultValue);
    }

    public T get() {
        return get(CurrentHttpServerContext.request().serverObject());
    }


    public void set(Object server, T value) {
        values.put(server, value);
    }

    public void set(T value) {
        set(CurrentHttpServerContext.request().serverObject(), value);
    }


    public void remove(Object server) {
        values.remove(server);
    }

    public void remove() {
        remove(CurrentHttpServerContext.request().serverObject());
    }
}

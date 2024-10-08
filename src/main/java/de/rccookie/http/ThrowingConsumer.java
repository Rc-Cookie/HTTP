package de.rccookie.http;

public interface ThrowingConsumer<T,E extends Throwable> {

    void accept(T t) throws E;
}

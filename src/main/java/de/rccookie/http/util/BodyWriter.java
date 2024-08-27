package de.rccookie.http.util;

import java.io.IOException;

public interface BodyWriter extends AutoCloseable {

    void write(HttpStream out) throws IOException, InterruptedException;

    @Override
    default void close() throws Exception { }

    default void buffer() { }

    default int contentLength() {
        return -1;
    }
}

package de.rccookie.http.server.raw;

import java.io.IOException;
import java.io.OutputStream;

import org.jetbrains.annotations.NotNull;

class FixedLengthOutputStream extends OutputStream {

    private final OutputStream dst;
    private long length;

    FixedLengthOutputStream(OutputStream dst, long length) {
        this.dst = dst;
        this.length = length;
    }

    @Override
    public void write(int b) throws IOException {
        checkState();
        if(length == 0)
            throw new IOException("Too many bytes written");
        length--;
        dst.write(b);
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws IOException {
        checkState();
        if(length < len) {
            len = (int) length;
            length = 0;
            dst.write(b, off, len);
            throw new IOException("Too many bytes written");
        }
        length -= len;
        dst.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkState();
        dst.flush();
    }

    @Override
    public void close() throws IOException {
        if(length > 0) {
            length = -1;
            dst.close();
        }
        else length = -1;
    }

    private void checkState() {
        if(length < 0)
            throw new IllegalStateException("Stream closed");
    }
}

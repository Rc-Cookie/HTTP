package de.rccookie.http.server.raw;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.NotNull;

class ChunkedOutputStream extends OutputStream {

    private static final byte[] LAST_CHUNK = { '0', '\r', '\n', '\r', '\n' };

    private final OutputStream dst;
    private final byte[] buf;
    private int bufLen = 0;
    private boolean closed = false;

    public ChunkedOutputStream(OutputStream dst, int bufferLength) {
        this.dst = dst;
        this.buf = new byte[bufferLength];
    }

    public ChunkedOutputStream(OutputStream dst) {
        this(dst, 4096);
    }

    @Override
    public void write(int b) throws IOException {
        checkState();
        buf[bufLen++] = (byte) b;
        if(bufLen == buf.length)
            flush(false);
    }

    @Override
    public void write(byte @NotNull [] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws IOException {
        checkState();
        if(bufLen + len < buf.length) {
            System.arraycopy(b, off, buf, bufLen, len);
            bufLen += len;
        }
        else {
            flush();
            writeChunk(b, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        checkState();
        flush(true);
    }

    private void checkState() {
        if(closed)
            throw new IllegalStateException("Stream closed");
    }

    private void flush(boolean recursive) throws IOException {
        if(bufLen != 0) {
            writeChunk(buf, 0, bufLen);
            bufLen = 0;
        }
        if(recursive)
            dst.flush();
    }

    private void writeChunk(byte[] b, int off, int len) throws IOException {
        dst.write(Integer.toHexString(len).toUpperCase().getBytes(StandardCharsets.US_ASCII));
        dst.write('\r');
        dst.write('\n');
        dst.write(b, off, len);
        dst.write('\r');
        dst.write('\n');
    }

    @Override
    public void close() throws IOException {
        if(closed) return;
        closed = true;
        flush(false);
        dst.write(LAST_CHUNK);
        dst.flush();
    }
}

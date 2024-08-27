package de.rccookie.http.server.raw;

import java.io.IOException;
import java.io.InputStream;

class FixedLengthInputStream extends InputStream {

    private final InputStream src;
    private long length;

    public FixedLengthInputStream(InputStream src, long length) {
        this.src = src;
        this.length = length;
    }

    @Override
    public int read() throws IOException {
        if(length <= 0)
            return -1;
        length--;
        return src.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if(length <= 0)
            return -1;
        if(len > length)
            len = (int) length;
        int read = src.read(b, off, len);
        length -= read;
        return read;
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        if(len > length)
            len = (int) length;
        byte[] bytes = src.readNBytes(len);
        length -= bytes.length;
        return bytes;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        if(length <= 0)
            return 0;
        if(len > length)
            len = (int) length;
        int read = src.readNBytes(b, off, len);
        length -= read;
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = src.skip(Math.min(n, length));
        length -= skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(src.available(), length);
    }

    @Override
    public void close() throws IOException {
        if(length != 0) {
            length = 0;
            src.close();
        }
    }

    @Override
    public synchronized void mark(int readLimit) {
        src.mark((int) Math.min(readLimit, length));
    }

    @Override
    public synchronized void reset() throws IOException {
        src.reset();
    }

    @Override
    public boolean markSupported() {
        return src.markSupported();
    }
}

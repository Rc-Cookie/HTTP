package de.rccookie.http.server.raw;

import java.io.IOException;
import java.io.InputStream;

class ChunkedInputStream extends InputStream {

    private final InputStream src;
    private long currentChunk = 0;
    private boolean start = true;
    private boolean end = false;

    public ChunkedInputStream(InputStream src) {
        this.src = src;
    }

    private boolean nextChunkIfNeeded() throws IOException {
        if(end) return true;
        if(currentChunk > 0) return false;

        int c;
        if(!start) {
            c = src.read();
            if(c == -1)
                return end = true;
            else if(c == '\r') {
                if((c = src.read()) == -1)
                    return end = true;
                else if(c != '\n')
                    throw new IOException("Invalid chunked stream: \\r not followed by \\n");
            } else if(c != '\n')
                throw new IOException("Invalid chunked stream: chunk not followed by \\r\\n");
        }
        else start = false;

        currentChunk = 0;
        while((c = src.read()) != '\r' && c != '\n' && c != ';' && c != -1) {
            if(currentChunk >= 'a')
                currentChunk -= 'a' - 10;
            else if(currentChunk >= 'A')
                currentChunk -= 'A' - 10;
            currentChunk = 10 * currentChunk + c - '0';
        }
        if(c == ';')
            while((c = src.read()) != '\r' && c != '\n' && c != -1);

        if(c == -1)
            return end = true;
        if(c == '\r') {
            if((c = src.read()) == -1)
                return end = true;
            else if(c != '\n')
                throw new IOException("Invalid chunked stream: chunk length not followed by \\r\\n");
        }

        if(currentChunk == 0) {
            end = true;
            while((c = src.read()) != '\n' && c != -1);
        }
        return end;
    }

    @Override
    public int read() throws IOException {
        if(nextChunkIfNeeded())
            return -1;

        int b = src.read();
        if(b == -1)
            end = true;
        else currentChunk--;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if(nextChunkIfNeeded())
            return -1;

        int read = src.read(b, off, (int) Math.min(len, currentChunk));
        if(read == -1)
            end = true;
        else currentChunk -= read;
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        if(n < 0) return 0;
        long skipped = src.skip(Math.min(currentChunk, n));
        currentChunk -= skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(currentChunk, src.available());
    }

    @Override
    public void close() throws IOException {
        if(!end) {
            currentChunk = 0;
            end = true;
            src.close();
        }
    }
}

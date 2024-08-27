package de.rccookie.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Function;

import de.rccookie.json.JsonObject;
import de.rccookie.util.Arguments;
import de.rccookie.util.ListStream;
import de.rccookie.util.Utils;
import de.rccookie.util.Wrapper;

class BufferableBody implements Body {

    private final Body body;
    private final String jsonKey;
    private final Function<? super Body, ?> jsonValue;

    boolean consumed = false;
    boolean buffered = false;
    byte[] buffer = null;

    BufferableBody(String jsonKey, Function<? super Body, ?> jsonValue, Body body) {
        this.body = Arguments.checkNull(body, "body");
        this.jsonKey = Arguments.checkNull(jsonKey, "jsonKey");
        this.jsonValue = Arguments.checkNull(jsonValue, "jsonValue");
    }

    @Override
    public long contentLength() {
        if(consumed)
            throw new IllegalStateException("Body already (partially) consumed, cannot determine size");
        if(buffered)
            return getBuffer().length;
        return body.contentLength();
    }

    @Override
    public InputStream stream() {
        if(buffered)
            return new ByteArrayInputStream(getBuffer());
        consumed = true;
        return body.stream();
    }

    @Override
    public byte[] data() {
        return buffered ? getBuffer() : body.data();
    }

    @Override
    public String text() {
        return buffered ? new String(getBuffer()) : body.text();
    }

    @Override
    public void close() throws Exception {
        if(buffered) return;
        consumed = true;
        body.close();
    }

    @Override
    public void buffer() {
        if(consumed)
            throw new IllegalStateException("Body already (partially) consumed, cannot buffer anymore");
        buffered = true;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, InterruptedException {
        if(buffered)
            out.write(getBuffer());
        else {
            consumed = true;
            body.writeTo(out);
        }
    }

    private byte[] getBuffer() {
        assert buffered;
        if(buffer == null) try {
            buffer = body.stream().readAllBytes();
        } catch(IOException e) {
            throw Utils.rethrow(e);
        }
        return buffer;
    }

    @Override
    public Object toJson() {
        buffer();
        return new JsonObject(jsonKey, jsonValue.apply(this));
    }

    static class Multipart extends BufferableBody implements Body.Multipart {
        final Wrapper<String> boundary;
        final List<Part> parts;
        Multipart(String boundary, List<Part> parts) {
            this(new Wrapper<>(Arguments.checkNull(boundary, "boundary")), parts);
        }
        private Multipart(Wrapper<String> boundary, List<Part> parts) {
            super("", b -> null, new Body.Multipart() {
                @Override
                public String boundary() {
                    return boundary.value;
                }
                @Override
                public ListStream<Part> parts() {
                    return ListStream.of(parts);
                }
                @Override
                public void buffer() {
                    throw new UnsupportedOperationException();
                }
            });
            this.boundary = Arguments.checkNull(boundary, "boundary");
            this.parts = Arguments.checkNull(parts, "parts");
        }
        Multipart(List<Part> parts) {
            this(Long.toHexString(System.currentTimeMillis()) + Long.toHexString(System.nanoTime()), parts);
        }

        @Override
        public String boundary() {
            return boundary.value;
        }
        @Override
        public ListStream<Part> parts() {
            return ListStream.of(parts);
        }

        @Override
        public Object toJson() {
            return Body.Multipart.super.toJson();
        }
    }
}

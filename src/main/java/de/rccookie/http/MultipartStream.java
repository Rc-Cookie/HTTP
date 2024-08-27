package de.rccookie.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;

import de.rccookie.json.Json;
import de.rccookie.util.Arguments;
import de.rccookie.util.EmptyIteratorException;
import de.rccookie.util.UncheckedException;

final class MultipartStream implements Iterator<Body.Multipart.Part> {

    private final InputStream in;
    private final Charset charset;
    private final String boundaryStr;
    private final byte[] boundary;
    private final byte[] startOfPart;
    Boolean hasNext = null;

    MultipartStream(InputStream in, Charset charset) {
        this.in = Arguments.checkNull(in, "in");
        this.charset = Arguments.checkNull(charset, "charset");
        try {
            readUntil("--");
            byte[] boundary = readUntil("\r\n");
            this.boundaryStr = new String(boundary);
            byte[] prefix = "\r\n--".getBytes(charset);
            this.boundary = new byte[prefix.length + boundary.length];
            System.arraycopy(prefix, 0, this.boundary, 0, prefix.length);
            System.arraycopy(boundary, 0, this.boundary, prefix.length, boundary.length);
            startOfPart = "Content-Disposition: form-data; ".getBytes(charset);
        } catch(IOException e) {
            throw new UncheckedException(e);
        }
    }

    public String boundary() {
        return boundaryStr;
    }


    @Override
    public boolean hasNext() {
        if(hasNext != null) return hasNext;
        try {
            return hasNext = Arrays.equals(in.readNBytes(startOfPart.length), startOfPart);
        } catch(IOException e) {
            throw new UncheckedException(e);
        }
    }

    @Override
    public Body.Multipart.Part next() {
        if(!hasNext()) throw new EmptyIteratorException(this);
        hasNext = null;
        try {
            String header = new String(readUntil("\r\n\r\n"));
            int index = header.indexOf("\r\n");
            String contentType = null;
            String name = null;
            String filename = null;
            if(index != -1) {
                contentType = header.substring(index + "\r\nContent-Type: ".length());
                header = header.substring(0, index);
            }
            while(!header.isBlank()) {
                index = header.indexOf('=');
                String key = header.substring(0, index);
                header = header.substring(index + 1);
                String value = Json.getParser(header).next().asString();
                header = header.substring(Json.toString(value).length());
                if(header.startsWith("; ")) header = header.substring(2);
                if(key.equals("name"))
                    name = value;
                else if(key.equals("filename"))
                    filename = value;
                else throw new MultipartSyntaxException("Unexpected header key: '"+key+"'");
            }
            if(name == null) throw new MultipartSyntaxException("name key missing");

            byte[] data = readUntil(boundary, "boundary");
            // skip \r\n or --
            //noinspection ResultOfMethodCallIgnored
            in.skip(2);
            return Body.Multipart.Part.of(name, filename, contentType != null ? ContentType.of(contentType) : null, Body.of(data));

        } catch(IOException e) {
            throw new UncheckedException(e);
        }
    }


    private byte[] readUntil(String end) throws IOException {
        return readUntil(end.getBytes(charset), Json.toString(end));
    }

    private byte[] readUntil(byte[] end, String endDesc) throws IOException {
        ExposedByteArrayOutputStream read = new ExposedByteArrayOutputStream();

        while(read.size() < end.length || !Arrays.equals(end, 0, end.length, read.buf(), read.size() - end.length, read.size())) {
            int b = in.read();
            if(b == -1) throw new MultipartSyntaxException("Expected "+endDesc+", found EOF");
            read.write(b);
        }

        byte[] between = new byte[read.size() - end.length];
        System.arraycopy(read.buf(), 0, between, 0, between.length);
        return between;
    }

    private static class ExposedByteArrayOutputStream extends ByteArrayOutputStream {
        public byte[] buf() {
            return buf;
        }
    }
}

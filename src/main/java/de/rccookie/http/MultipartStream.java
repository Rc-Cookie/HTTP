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
    private final byte[] boundary;
    private final byte[] startOfPart;
    Boolean hasNext = null;

    MultipartStream(InputStream in, Charset charset) {
        this.in = Arguments.checkNull(in, "in");
        this.charset = Arguments.checkNull(charset, "charset");
        try {
            readUntil("--");
            byte[] boundary = readUntil("\r\n");
            byte[] prefix = "\r\n--".getBytes(charset);
            this.boundary = new byte[prefix.length + boundary.length];
            System.arraycopy(prefix, 0, this.boundary, 0, prefix.length);
            System.arraycopy(boundary, 0, this.boundary, 0, boundary.length);
            startOfPart = "Content-Disposition: form-data; ".getBytes(charset);
        } catch(IOException e) {
            throw new UncheckedException(e);
        }
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
            return Body.Multipart.Part.of(name, filename, ContentType.of(contentType), Body.of(data));

        } catch(IOException e) {
            throw new UncheckedException(e);
        }
    }


    private byte[] readUntil(String end) throws IOException {
        return readUntil(end.getBytes(charset), Json.toString(end));
    }
    private byte[] readUntil(byte[] end, String endDesc) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int i = 0;
        while(i < end.length) {
            int b = in.read();
            if(b == -1) throw new MultipartSyntaxException("Expected "+endDesc+", found EOF");
            if(b == end[i]) i++;
            else {
                int startI = i;
                while(i > 0) {
                    out.write(end[0]);
                    i--;
                    if(Arrays.equals(end, 0, i, end, startI - i, startI) && b == end[i]) break;
                }
                if(b == end[i]) i++;
                else out.write(end);
            }
        }

        return out.toByteArray();
    }
}

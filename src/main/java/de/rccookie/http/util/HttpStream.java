package de.rccookie.http.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import de.rccookie.http.Body;
import de.rccookie.json.Json;
import de.rccookie.util.Utils;
import de.rccookie.xml.Document;
import de.rccookie.xml.Node;
import de.rccookie.xml.XML;
import org.jetbrains.annotations.NotNull;

public abstract class HttpStream extends OutputStream {

    @Override
    public abstract void write(int b) throws IOException;

    @Override
    public abstract void write(byte @NotNull [] b) throws IOException;

    @Override
    public abstract void write(byte @NotNull [] b, int off, int len) throws IOException;

    @Override
    public abstract void flush() throws IOException;

    @Override
    public abstract void close() throws IOException;

    public abstract void write(Body body);

    public abstract void writeData(byte[] bytes);

    public abstract void writeText(String text);

    public abstract void writeJson(Object json, boolean formatted);

    public abstract void writeJson(Object json);

    public abstract void writeXML(Node xml, long options);

    public abstract void writeXML(Node xml);

    public abstract void writeHTML(Document html);

    public abstract void writeFile(Path file);

    public abstract void writeFile(File file);

    public abstract void writeFile(String file);

    public abstract void writeStream(InputStream in);

    public static HttpStream of(OutputStream out) {
        return new HttpStream() {
            @Override
            public String toString() {
                return "HttpStream["+out+"]";
            }

            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public void write(byte @NotNull [] b) throws IOException {
                out.write(b);
            }

            @Override
            public void write(byte @NotNull [] b, int off, int len) throws IOException {
                out.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                out.close();
            }

            @Override
            public void write(Body body) {
                try {
                    body.writeTo(out);
                    flush();
                } catch(Exception e) {
                    throw Utils.rethrow(e);
                }
            }

            @Override
            public void writeData(byte[] bytes) {
                try {
                    out.write(bytes);
                    out.flush();
                } catch(IOException e) {
                    throw Utils.rethrow(e);
                }
            }

            @Override
            public void writeText(String text) {
                writeData(text.getBytes());
            }

            @Override
            public void writeJson(Object json, boolean formatted) {
                Json.write(json, out, formatted);
            }

            @Override
            public void writeJson(Object json) {
                writeJson(json, Body.DEFAULT_JSON_FORMATTED.value);
            }

            @Override
            public void writeXML(Node xml, long options) {
                XML.write(xml, out, options);
            }

            @Override
            public void writeXML(Node xml) {
                XML.write(xml, out, false);
            }

            @Override
            public void writeHTML(Document html) {
                XML.write(html, out, true);
            }

            @Override
            public void writeFile(Path file) {
                try(InputStream in = Files.newInputStream(file)) {
                    in.transferTo(out);
                    out.flush();
                } catch(IOException e) {
                    throw Utils.rethrow(e);
                }
            }

            @Override
            public void writeFile(File file) {
                writeFile(file.toPath());
            }

            @Override
            public void writeFile(String file) {
                writeFile(Path.of(file));
            }

            @Override
            public void writeStream(InputStream in) {
                try {
                    in.transferTo(out);
                    out.flush();
                } catch(IOException e) {
                    throw Utils.rethrow(e);
                }
            }
        };
    }
}

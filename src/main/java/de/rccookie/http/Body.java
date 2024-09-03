package de.rccookie.http;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.net.http.HttpRequest;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.rccookie.http.util.BodyWriter;
import de.rccookie.http.util.HttpStream;
import de.rccookie.json.Json;
import de.rccookie.json.JsonElement;
import de.rccookie.json.JsonObject;
import de.rccookie.json.JsonSerializable;
import de.rccookie.util.Arguments;
import de.rccookie.util.BoolWrapper;
import de.rccookie.util.ListStream;
import de.rccookie.util.Pipe;
import de.rccookie.util.StringInputStream;
import de.rccookie.util.URLBuilder;
import de.rccookie.util.UncheckedException;
import de.rccookie.util.Utils;
import de.rccookie.xml.Document;
import de.rccookie.xml.FormData;
import de.rccookie.xml.Node;
import de.rccookie.xml.XML;
import de.rccookie.xml.XMLParseException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The content of an http request or response. Some body implementations
 * may the data to only be read once.
 */
public interface Body extends AutoCloseable, JsonSerializable {

    /**
     * Whether json output is formatted by default. This is enabled by default
     * iff a debugger is detected, or if the property <code>rccookie.http.formatted</code>
     * or <code>rccookie.http.formatted.json</code> is set. Value can be edited.
     */
    BoolWrapper DEFAULT_JSON_FORMATTED = new BoolWrapper(System.getProperty("intellij.debug.agent") != null || System.getProperty("rccookie.http.formatted") != null || System.getProperty("rccookie.http.formatted.json") != null);

    /**
     * Whether xml output (including html etc.) is formatted by default. This is
     * enabled by default iff a debugger is detected, or if the property
     * <code>rccookie.http.formatted</code> or <code>rccookie.http.formatted.xml</code>
     * is set. Value can be edited.
     */
    BoolWrapper DEFAULT_XML_FORMATTED = new BoolWrapper(System.getProperty("intellij.debug.agent") != null || System.getProperty("rccookie.http.formatted") != null || System.getProperty("rccookie.http.formatted.xml") != null);

    /**
     * A body with no content (an empty string).
     */
    Body EMPTY = new Body() {

        {
            Json.registerDeserializer(Body.class, json -> {
                if(json.containsKey("text"))
                    return Body.of(json.getString("text"));
                if(json.containsKey("bytes"))
                    return Body.of(json.get("bytes").as(byte[].class));
                if(json.containsKey("base64"))
                    return Body.of(Utils.bytesFromBase64(json.getString("base64")));
                if(json.containsKey("json"))
                    return Body.ofJson(json.get("json"));
                if(json.containsKey("parts"))
                    return json.as(Multipart.class);
                return EMPTY;
            });
        }

        @Override
        public long contentLength() {
            return 0;
        }

        @Override
        public InputStream stream() {
            return InputStream.nullInputStream();
        }

        @Override
        public HttpRequest.BodyPublisher toBodyPublisher() {
            return HttpRequest.BodyPublishers.noBody();
        }

        @Override
        public void close() throws Exception {
            stream().close();
        }

        @Override
        public byte[] data() {
            return new byte[0];
        }

        @Override
        public String text() {
            return "";
        }

        @Override
        public JsonElement json() {
            return Json.parse("");
        }

        @Override
        public Document xml(long options) {
            return XML.parse("", options);
        }

        @Override
        public Multipart asMultipart() {
            throw new MultipartSyntaxException("Reached EOF while parsing");
        }

        @Override
        public void buffer() { }

        @Override
        public void writeTo(OutputStream out) { }

        @Override
        public Object toJson() {
            return new JsonObject("text", "");
        }
    };

    /**
     * Returns the length of this body as the number of bytes it contains, or -1 if unknown.
     *
     * @return The length of this body
     */
    long contentLength();

    /**
     * Returns an input stream over the content. Note that calling this method
     * multiple times may result in the same stream possibly partially read if
     * the body is not buffered, or in new streams returned every time starting
     * from the beginning of the content again, if the body is buffered.
     *
     * @return An input stream over the data
     */
    InputStream stream();

    /**
     * Returns a body publisher with the contents of this body.
     *
     * @return A body publisher of this body
     */
    HttpRequest.BodyPublisher toBodyPublisher();

    /**
     * Returns the contents as raw bytes.
     *
     * @return The raw content
     */
    default byte[] data() {
        try(InputStream in = stream()) {
            return in.readAllBytes();
        } catch(IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Returns the contents as string.
     *
     * @return The content
     */
    default String text() {
        return new String(data());
    }

    /**
     * Returns the content parsed as url-encoded parameters.
     *
     * @return The contents as url parameters
     */
    default Query params() {
        return Query.of(new URLBuilder("http", "a").queryString(text()));
    }

    /**
     * Returns the contents deserialized as json.
     *
     * @return The contents parsed from json
     */
    default JsonElement json() {
        return Json.parse(stream());
    }

    /**
     * Returns the contents deserialized as xml.
     *
     * @return The contents parsed from xml
     */
    default Document xml() {
        return XML.parse(stream());
    }

    /**
     * Returns the contents deserialized as xml.
     *
     * @param options Parsing options
     * @return The contents parsed from xml
     */
    default Document xml(long options) {
        return XML.parse(stream(), options);
    }

    /**
     * Returns the contents deserialized as html.
     *
     * @return The contents parsed from html
     */
    default Document html() {
        return xml(XML.HTML);
    }

    /**
     * Returns this body (lazily) parsed as multipart body (or the body
     * itself if it already is a multipart).
     *
     * @return The body parsed as <code>multipart/formdata</code>
     */
    Multipart asMultipart();

    /**
     * Marks the body to buffer its contents such that they can be
     * read multiple times. Closing the body will clear the buffer.
     * If the body was already (partially) consumed or closed, this
     * method may throw an exception.
     */
    void buffer();

    /**
     * Writes the contents of the body to the given output stream. This
     * does not close the given output stream.
     *
     * @param out The stream to write this body's contents to
     * @throws IOException If an I/O exception occurs
     */
    void writeTo(OutputStream out) throws IOException, InterruptedException;


    /**
     * Represents a multipart http body.
     */
    interface Multipart extends Body, Iterable<Multipart.Part> {

        @ApiStatus.Internal
        int _init = init();
        private static int init() {
            Json.registerDeserializer(Multipart.class, json -> {
                if(!json.containsKey("parts"))
                    return parse(json.as(Body.class));
                Multipart.Editable m = Body.multipart();
                for(Part p : json.get("parts").asList(Part.class))
                    m.add(p);
                return m;
            });
            return 0;
        }

        @Override
        default long contentLength() {
            int boundary = ("--" + boundary()).getBytes().length;
            int newline = "\r\n".getBytes().length;

            long length = 0;
            for(Part part : parts()) {
                long l = part.body().contentLength();
                if(l < 0) return -1;
                length += l + boundary + 4L * newline;

                StringBuilder header = new StringBuilder("Content-Disposition: form-data; name=");
                header.append(Json.toString(part.name())); // Encode and enquote string
                if(part.filename() != null)
                    header.append("; filename=").append(Json.toString(part.filename()));
                if(part.contentType() != null)
                    header.append("\r\nContent-Type: ").append(part.contentType());
                length += header.toString().getBytes().length;
            }
            return length + boundary + "--".getBytes().length;
        }

        /**
         * Returns the boundary string used in the multipart body.
         *
         * @return The boundary used
         */
        String boundary();

        /**
         * Returns the body parts in proper order.
         *
         * @return The multipart parts
         */
        ListStream<Part> parts();

        /**
         * Collects the body parts into a sorted map, each part being mapped
         * to its respective name. The parts may all have distinct names, otherwise
         * an {@link IllegalStateException} will be thrown.
         *
         * @return The multipart parts
         */
        default Map<String, Part> partsAsMap() {
            return parts().collect(Collectors.toMap(Part::name, p -> p, (a,b) -> { throw new IllegalStateException("Multipart contains multiple parts with same name"); }, LinkedHashMap::new));
        }

        @NotNull
        @Override
        default Iterator<Part> iterator() {
            return parts().iterator();
        }

        @SuppressWarnings("DuplicatedCode")
        @Override
        default InputStream stream() {
            List<InputStream> output = new ArrayList<>();

            String boundary = "--" + boundary();
            StringBuilder str = new StringBuilder();
            for(Part part : parts()) {
                str.append(boundary).append("\r\n");
                str.append("Content-Disposition: form-data; name=");
                str.append(Json.toString(part.name())); // Encode and enquote string
                if(part.filename() != null)
                    str.append("; filename=").append(Json.toString(part.filename()));
                if(part.contentType() != null)
                    str.append("\r\nContent-Type: ").append(part.contentType());
                str.append("\r\n\n\n");
                output.add(new StringInputStream(str.toString()));
                str = new StringBuilder();

                output.add(part.body().stream());
                str.append("\r\n");
            }
            str.append(boundary).append("--");
            output.add(new StringInputStream(str.toString()));

            return new SequenceInputStream(Collections.enumeration(output));
        }

        @Override
        default HttpRequest.BodyPublisher toBodyPublisher() {
            return HttpRequest.BodyPublishers.ofInputStream(this::stream);
        }

        @Override
        default void writeTo(OutputStream out) throws IOException, InterruptedException {
            byte[] boundary = ("--" + boundary()).getBytes();
            byte[] crlf = "\r\n".getBytes();
            byte[] contentDispositionFormDataNameEquals = "Content-Disposition: form-data; name=".getBytes();
            byte[] semiFilenameEquals = "; filename=".getBytes();
            byte[] contentTypeColon = "Content-Type: ".getBytes();
            for(Part part : parts()) {
                out.write(boundary);
                out.write(crlf);
                out.write(contentDispositionFormDataNameEquals);
                Json.write(part.name(), out); // Encode and enquote string
                if(part.filename() != null) {
                    out.write(semiFilenameEquals);
                    Json.write(part.filename(), out);
                }
                if(part.contentType() != null) {
                    out.write(crlf);
                    out.write(contentTypeColon);
                    out.write(part.contentType().toString().getBytes());
                }
                out.write(crlf);
                out.write(crlf);
                part.body().writeTo(out);
                out.write(crlf);
            }
            out.write(boundary);
            out.write("--".getBytes());
            out.flush();
        }

        @Override
        default byte[] data() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                writeTo(out);
            } catch(Exception e) {
                throw Utils.rethrow(e);
            }
            return out.toByteArray();
        }

        @SuppressWarnings("DuplicatedCode")
        @Override
        default String text() {
            return new String(data());
        }

        @Override
        default JsonElement json() {
            throw new RuntimeException("Data is a multipart body, not json");
        }

        @Override
        default Document xml() {
            throw new XMLParseException("Data is a multipart body, not xml");
        }

        @Override
        default Document xml(long options) {
            throw new XMLParseException("Data is a multipart body, not xml");
        }

        @Override
        default Document html() {
            throw new XMLParseException("Data is a multipart body, not html");
        }

        /**
         * Returns itself.
         *
         * @return This multipart body
         */
        @Override
        default Multipart asMultipart() {
            return this;
        }

        @Override
        default void close() throws Exception {
            Exception ex = null;
            for(Part part : this) try {
                part.body().close();
            } catch(Exception e) {
                if(ex == null) ex = e;
                else ex.addSuppressed(e);
            }
            if(ex != null) throw ex;
        }

        @Override
        default Object toJson() {
            return new JsonObject("boundary", boundary(), "parts", parts());
        }

        /**
         * Represents a single part of a multipart body.
         */
        interface Part extends JsonSerializable {

            @ApiStatus.Internal
            int _init = Part.init();
            private static int init() {
                Json.registerDeserializer(Part.class, json -> Part.of(
                        json.getString("name"),
                        json.getString("filename"),
                        json.get("contentType").as(ContentType.class),
                        json.get("body").as(Body.class)
                ));
                return 0;
            }

            /**
             * The content type of this part. May be null.
             *
             * @return The content type, if known
             */
            ContentType contentType();

            /**
             * The name of this part.
             *
             * @return The name of this part
             */
            @NotNull String name();

            /**
             * The filename of this part, if any.
             *
             * @return The filename of this part
             */
            String filename();

            /**
             * The data of this part.
             *
             * @return The content
             */
            @NotNull Body body();

            @Override
            default Object toJson() {
                return new JsonObject(
                        "name", name(),
                        "filename", filename(),
                        "contentType", contentType(),
                        "body", body()
                );
            }

            /**
             * Creates a part object from the given data.
             *
             * @param name The name of the multipart part
             * @param contentType The mime type of the content, if known
             * @param filename The filename of the part, if any
             * @param body The data of the part
             * @return A multipart part with the given values
             */
            static Part of(String name, @Nullable String filename, @Nullable ContentType contentType, Body body) {
                Arguments.checkNull(name, "name");
                Arguments.checkNull(body, "body");
                return new Part() {
                    @Override
                    public ContentType contentType() {
                        return contentType;
                    }
                    @Override
                    public @NotNull String name() {
                        return name;
                    }
                    @Override
                    public String filename() {
                        return filename;
                    }
                    @Override
                    public @NotNull Body body() {
                        return body;
                    }
                };
            }

            /**
             * Creates a part object from the given data.
             *
             * @param name The name of the part
             * @param filename The filename of the part, if any
             * @param contentType The content type of this part, if known
             * @param data The data of this part
             * @return A multipart part with the given values
             */
            static Part of(String name, @Nullable String filename, @Nullable ContentType contentType, String data) {
                return of(name, filename, contentType, Body.of(data));
            }

            /**
             * Creates a part object from the given data.
             *
             * @param name The name of the part
             * @param data The data of this part
             * @return A multipart part with the given values
             */
            static Part of(String name, Body data) {
                return of(name, null, null, data);
            }

            /**
             * Creates a part object from the given data.
             *
             * @param name The name of the part
             * @param data The data of this part
             * @return A multipart part with the given values
             */
            static Part of(String name, String data) {
                return of(name, null, null, data);
            }

            /**
             * Converts the given {@link FormData.Entry} to a {@link Multipart.Part}.
             *
             * @param data The form data entry to convert
             * @return A part with the same name and body as the form data
             */
            static Part of(FormData.Entry data) {
                return of(data.name(), Body.of(data.rawValue()));
            }

            /**
             * Creates a part object from the given data.
             *
             * @param name The name of the part
             * @param filename The filename of the part, if any
             * @param json The json data of this part, must be json-serializable
             * @return A multipart part with the given values
             */
            static Part ofJson(String name, @Nullable String filename, Object json) {
                return of(name, filename, ContentType.JSON, Body.ofJson(json));
            }

            /**
             * Creates a part object from the given data.
             *
             * @param name The name of the part
             * @param json The json data of this part, must be json-serializable
             * @return A multipart part with the given values
             */
            static Part ofJson(String name, String json) {
                return ofJson(name, null, json);
            }

            /**
             * Creates a part object from the given data.
             *
             * @param name The name of the part
             * @param filename The filename of the part, if any
             * @param xml The xml data of this part
             * @return A multipart part with the given values
             */
            static Part ofXML(String name, @Nullable String filename, Document xml) {
                return of(name, filename, ContentType.XML, Body.ofXML(xml));
            }

            /**
             * Creates a part object from the given data.
             *
             * @param name The name of the part
             * @param xml The xml data of this part
             * @return A multipart part with the given values
             */
            static Part ofXML(String name, Document xml) {
                return ofXML(name, null, xml);
            }

            /**
             * Creates a part object from the given data.
             *
             * @param name The name of the part
             * @param filename The filename of the part, if any
             * @param html The html data of this part
             * @return A multipart part with the given values
             */
            static Part ofHTML(String name, @Nullable String filename, Document html) {
                return of(name, filename, ContentType.HTML, Body.ofXML(html));
            }

            /**
             * Creates a part object from the given data.
             *
             * @param name The name of the part
             * @param html The html data of this part
             * @return A multipart part with the given values
             */
            static Part ofHTML(String name, Document html) {
                return ofHTML(name, null, html);
            }
        }

        /**
         * Represents an editable multipart http body.
         */
        interface Editable extends Multipart {

            /**
             * Sets the boundary for the multipart body. Usually this is not required, as a boundary random
             * boundary will be chosen by default.
             *
             * @param boundary the boundary to set
             * @return This multipart
             */
            Editable setBoundary(String boundary);

            /**
             * Adds the given part to this multipart body.
             *
             * @param part The part to add
             * @return This multipart
             */
            Editable add(Part part);

            /**
             * Adds the given part to this multipart body.
             *
             * @param name The name of the part
             * @param filename The filename of the part, if any
             * @param contentType The content type of this part, if known
             * @param data The data of this part
             * @return This multipart
             */
            default Editable add(String name, @Nullable String filename, @Nullable ContentType contentType, Body data) {
                return add(Part.of(name, filename, contentType, data));
            }

            /**
             * Adds the given part to this multipart body.
             *
             * @param name The name of the part
             * @param data The data of this part
             * @return This multipart
             */
            default Editable add(String name, Body data) {
                return add(name, null, null, data);
            }

            /**
             * Adds the given part to this multipart body.
             *
             * @param name The name of the part
             * @param filename The filename of the part, if any
             * @param contentType The content type of this part, if known
             * @param data The data of this part
             * @return This multipart
             */
            default Editable add(String name, @Nullable String filename, @Nullable ContentType contentType, String data) {
                return add(name, filename, contentType, Body.of(data));
            }

            /**
             * Adds the given part to this multipart body.
             *
             * @param name The name of the part
             * @param data The data of this part
             * @return This multipart
             */
            default Editable add(String name, String data) {
                return add(name, null, null, data);
            }

            /**
             * Adds the given part to this multipart body.
             *
             * @param name The name of the part
             * @param filename The filename of the part, if any
             * @param json The json data of this part, must be json-serializable
             * @return This multipart
             */
            default Editable addJson(String name, @Nullable String filename, Object json) {
                return add(name, filename, ContentType.JSON, Body.ofJson(json));
            }

            /**
             * Adds the given part to this multipart body.
             *
             * @param name The name of the part
             * @param json The json data of this part, must be json-serializable
             * @return This multipart
             */
            default Editable addJson(String name, String json) {
                return addJson(name, null, json);
            }

            /**
             * Adds the given part to this multipart body.
             *
             * @param name The name of the part
             * @param filename The filename of the part, if any
             * @param xml The xml data of this part
             * @return This multipart
             */
            default Editable addXML(String name, @Nullable String filename, Document xml) {
                return add(name, filename, ContentType.XML, Body.ofXML(xml));
            }

            /**
             * Adds the given part to this multipart body.
             *
             * @param name The name of the part
             * @param xml The xml data of this part
             * @return This multipart
             */
            default Editable addXML(String name, Document xml) {
                return addXML(name, null, xml);
            }

            /**
             * Adds the given part to this multipart body.
             *
             * @param name The name of the part
             * @param filename The filename of the part, if any
             * @param html The html data of this part
             * @return This multipart
             */
            default Editable addHTML(String name, @Nullable String filename, Document html) {
                return add(name, filename, ContentType.HTML, Body.ofXML(html));
            }

            /**
             * Adds the given part to this multipart body.
             *
             * @param name The name of the part
             * @param html The html data of this part
             * @return This multipart
             */
            default Editable addHTML(String name, Document html) {
                return addHTML(name, null, html);
            }
        }

        /**
         * Parses the given body as a multipart body. This consumes the stream of the given body, but
         * the returned body can of course be read.
         *
         * <p>In general, calling {@link Body#asMultipart()} should be the preferred way of parsing a
         * body as a multipart.</p>
         *
         * @param data The data to parse. If this already is a multipart it will be returned directly
         * @return A multipart parsed from the given data
         * @see Body#asMultipart()
         * @apiNote The data may not be parsed immediately, at least not completely, so parsing exceptions
         * may occur after this method terminates
         */
        static Multipart parse(Body data) {
            if(Arguments.checkNull(data, "data") instanceof Multipart)
                return (Multipart) data;

            MultipartStream parts = new MultipartStream(data.stream(), Charset.defaultCharset());
            return new BufferableBody.Multipart(parts.boundary(), ListStream.of(parts));
        }

        /**
         * Converts the given form data to a multipart body, assigning it an arbitrary boundary.
         *
         * @param data The form data to convert to an http body
         * @return A multipart with the same parts as the form data had entries
         */
        static Multipart of(FormData data) {
            Arguments.checkNull(data, "data");
            return new BufferableBody.Multipart(data.stream().map(Part::of).collect(Collectors.toList()));
        }
    }


    /**
     * Returns a body over the given input stream.
     *
     * @param stream The stream containing the data.
     * @return A body with the stream as data source
     */
    static Body of(@Nullable InputStream stream) {
        if(stream == null) return EMPTY;
        return new BufferableBody("base64", b -> Utils.toBase64(b.data()), new Body() {

            @Override
            public long contentLength() {
                try {
                    if(stream instanceof ByteArrayInputStream)
                        return stream.available();
                    if(stream instanceof FileInputStream)
                        return ((FileInputStream) stream).getChannel().size();
                    if(stream.getClass().getName().equals("sun.nio.ch.ChannelInputStream")) try {
                        return ((FileChannel) Utils.getField(stream, "ch")).size();
                    } catch(Exception ignored) { }
                } catch(Exception e) {
                    throw Utils.rethrow(e);
                }
                return -1;
            }

            @Override
            public InputStream stream() {
                return stream;
            }

            @Override
            public HttpRequest.BodyPublisher toBodyPublisher() {
                return HttpRequest.BodyPublishers.ofInputStream(() -> stream);
            }

            @Override
            public Multipart asMultipart() {
                if(stream instanceof BufferedInputStream)
                    return Multipart.parse(this);
                // LeftOverInputStream seems to have a bug where it hangs up when reading
                // past the end of the stream, or trying to close it, after having parsed
                // it, but reading it directly (e.g. with data()) works fine. This seems
                // to fix it.
                return of(new BufferedInputStream(stream)).asMultipart();
            }

            @Override
            public void close() throws Exception {
                stream.close();
            }

            @Override
            public void buffer() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void writeTo(OutputStream out) throws IOException {
                stream.transferTo(out);
                out.flush();
            }

            @Override
            public String toString() {
                return super.toString();
            }

            @Override
            public Object toJson() {
                throw new UnsupportedOperationException();
            }
        });
    }

    /**
     * Returns a body with the contents of the given file as content. This should be preferred
     * over <code>of(new FileInputStream(file))</code> and especially over <code>of(Files.newInputStream(file))</code>
     * as for the latter it will be impossible to determine the file size in advance (for the
     * Content-Length header).
     *
     * @param file The file who's content to use, must exist and be a readable file
     * @return A body with the contents of the file
     */
    static Body of(@NotNull Path file) {
        Arguments.checkNull(file, "file");
        long size;
        InputStream stream;
        try {
            size = Files.size(file);
            //noinspection resource
            stream = Files.newInputStream(file);
        } catch(IOException e) {
            throw Utils.rethrow(e);
        }
        return new BufferableBody("base64", b -> Utils.toBase64(b.data()), new Body() {
            @Override
            public long contentLength() {
                return size;
            }

            @Override
            public InputStream stream() {
                return stream;
            }

            @Override
            public HttpRequest.BodyPublisher toBodyPublisher() {
                try {
                    return HttpRequest.BodyPublishers.ofFile(file);
                } catch (FileNotFoundException e) {
                    throw Utils.rethrow(e);
                }
            }

            @Override
            public Multipart asMultipart() {
                return Multipart.parse(this);
            }

            @Override
            public void buffer() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void writeTo(OutputStream out) throws IOException {
                stream.transferTo(out);
                out.flush();
            }

            @Override
            public void close() throws Exception {
                stream.close();
            }

            @Override
            public Object toJson() {
                throw new UnsupportedOperationException();
            }
        });
    }

    /**
     * Returns a body with the contents of the given file as content. This should be preferred
     * over <code>of(new FileInputStream(file))</code> and especially over <code>of(Files.newInputStream(file))</code>
     * as for the latter it will be impossible to determine the file size in advance (for the
     * Content-Length header).
     *
     * @param file The file who's content to use, must exist and be a readable file
     * @return A body with the contents of the file
     */
    static Body of(@NotNull File file) {
        return of(Arguments.checkNull(file, "file").toPath());
    }

    /**
     * Returns a body with the given bytes as content.
     *
     * @param bytes The content of the body
     * @return A body with the bytes as data
     */
    static Body of(byte[] bytes) {
        Arguments.checkNull(bytes, "bytes");
        return new OfData(bytes, false);
    }

    /**
     * Returns a body with the given string as content.
     *
     * @param text The content of the body
     * @return A body with the string as data
     */
    static Body of(String text) {
        Arguments.checkNull(text, "text");
        return new Body() {
            String str = text;
            byte[] bytes = null;

            @Override
            public long contentLength() {
                return data().length;
            }
            @Override
            public byte[] data() {
                if(bytes == null) {
                    if(str == null)
                        throw new IllegalStateException("Body has been closed");
                    bytes = str.getBytes();
                    str = null;
                }
                return bytes;
            }
            @Override
            public synchronized String text() {
                return str != null ? str : new String(data());
            }

            @Override
            public Multipart asMultipart() {
                return Multipart.parse(this);
            }

            @Override
            public InputStream stream() {
                return str != null ? new StringInputStream(str) {
                    @Override
                    public void close() throws IOException {
                        str = null;
                        bytes = null;
                        super.close();
                    }
                } : new ByteArrayInputStream(bytes) {
                    @Override
                    public void close() throws IOException {
                        str = null;
                        bytes = null;
                        super.close();
                    }
                };
            }

            @Override
            public HttpRequest.BodyPublisher toBodyPublisher() {
                if(str != null)
                    return HttpRequest.BodyPublishers.ofString(str);
                else if(bytes != null)
                    return HttpRequest.BodyPublishers.ofByteArray(bytes);
                else throw new IllegalStateException("Body already closed, cannot read anymore");
            }

            @Override
            public void close() {
                str = null;
                bytes = null;
            }
            @Override
            public void buffer() {
                if(str == null && bytes == null)
                    throw new IllegalStateException("Body already closed, cannot buffer anymore");
            }

            @Override
            public void writeTo(OutputStream out) throws IOException {
                if(str == null && bytes == null)
                    throw new IllegalStateException("Body already closed, cannot be written anymore");
                out.write(data());
                out.flush();
            }

            @Override
            public Object toJson() {
                if(str == null && bytes == null) throw new IllegalStateException("Body has been closed");
                return new JsonObject("text", text());
            }
        };
    }

    /**
     * Converts the given form data to a multipart body, assigning it an arbitrary boundary.
     *
     * @param data The form data to convert to an http body
     * @return A multipart with the same parts as the form data had entries
     */
    static Multipart of(FormData data) {
        return Multipart.of(data);
    }

    /**
     * Returns a body with the given json as content
     *
     * @param json The content of the body, must be json-serializable
     * @return A body with the json as data
     */
    static Body ofJson(Object json) {
        return ofJson(json, DEFAULT_JSON_FORMATTED.value);
    }

    /**
     * Returns a body with the given json as content
     *
     * @param json The content of the body, must be json-serializable
     * @param formatted Whether to format the serialized json string
     * @return A body with the json as data
     */
    static Body ofJson(Object json, boolean formatted) {
        return new OfJson(json, formatted);
    }

    /**
     * Returns a body with the given html as content.
     *
     * @param html The content of the body
     * @return A body with the html as data
     */
    static Body ofHTML(Document html) {
        return ofXML(Arguments.checkNull(html, "html"), XML.HTML);
    }

    /**
     * Returns a body with the given xml as content.
     *
     * @param xml The content of the body
     * @return A body with the xml as data
     */
    static Body ofXML(Node xml) {
        return ofXML(xml, XML.XML);
    }

    /**
     * Returns a body with the given xml as content.
     *
     * @param xml The content of the body
     * @param options Determines how to serialize the xml to a string
     * @return A body with the xml as data
     */
    static Body ofXML(Node xml, long options) {
        return ofXML(xml, options, DEFAULT_XML_FORMATTED.value);
    }

    /**
     * Returns a body with the given xml as content.
     *
     * @param xml The content of the body
     * @param options Determines how to serialize the xml to a string
     * @param formatted If true, the {@link XML#FORMATTED} flag will be added
     *                  to the given options
     * @return A body with the xml as data
     */
    static Body ofXML(Node xml, long options, boolean formatted) {
        Arguments.checkNull(xml, "xml");
        return new OfData(xml.toXML(options | (formatted ? XML.FORMATTED : 0)).getBytes(), true) {
            @Override
            public JsonElement json() {
                throw new RuntimeException("Data in in XML format");
            }
        };
    }

    static Body ofWriter(BodyWriter writer) {
        Arguments.checkNull(writer, "writer");
        return new Body() {
            @Override
            public long contentLength() {
                return writer.contentLength();
            }

            @Override
            public InputStream stream() {
                try {
                    Pipe pipe = new Pipe();
                    Thread parsingThread = Thread.currentThread();
                    new Thread(() -> {
                        try {
                            writeTo(pipe.out());
                        } catch(IOException e) {
                            parsingThread.interrupt();
                            throw Utils.rethrow(e);
                        } catch(InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }, "Body -> InputStream pipe").start();
                    return pipe;
                } catch(IOException e) {
                    throw Utils.rethrow(e);
                }
            }

            @Override
            public HttpRequest.BodyPublisher toBodyPublisher() {
                return HttpRequest.BodyPublishers.ofInputStream(this::stream);
            }

            @Override
            public byte[] data() {
                return bytes().toByteArray();
            }

            @Override
            public String text() {
                return bytes().toString();
            }

            private ByteArrayOutputStream bytes() {
                long len = contentLength();
                if(len > Integer.MAX_VALUE)
                    throw new UnsupportedOperationException("Content does not fit into an array");
                ByteArrayOutputStream data = new ByteArrayOutputStream(len >= 0 ? (int) len : 1024);
                try {
                    writeTo(data);
                } catch(IOException e) {
                    throw Utils.rethrow(e);
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return data;
            }

            @Override
            public void buffer() {
                writer.buffer();
            }

            @Override
            public void writeTo(OutputStream out) throws IOException, InterruptedException {
                writer.write(HttpStream.of(out));
                out.flush();
            }

            @Override
            public JsonElement json() {
                return Json.parse(stream());
            }

            @Override
            public Multipart asMultipart() {
                return Multipart.parse(this);
            }

            @Override
            public Object toJson() {
                return new JsonObject("base64", Utils.toBase64(data()));
            }

            @Override
            public void close() throws Exception {
                writer.close();
            }
        };
    }

    /**
     * Returns an empty, editable multipart body.
     *
     * @return A multipart body which parts may be added to
     */
    static Multipart.Editable multipart() {
        return new EditableMultipart();
    }

    class OfJson implements Body {

        private final boolean formatted;
        private boolean closed = false;
        private Object json;
        private byte[] jsonStringBytes;

        public OfJson(Object json, boolean formatted) {
            this.json = Json.serialize(json);
            this.formatted = formatted;
        }

        @Override
        public long contentLength() {
            return data().length;
        }

        @Override
        public synchronized byte[] data() {
            if(closed)
                throw new IllegalStateException("Body has been closed");
            if(jsonStringBytes == null) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                Json.write(json, bytes, formatted);
                jsonStringBytes = bytes.toByteArray();
            }
            return jsonStringBytes;
        }

        @Override
        public synchronized InputStream stream() {
            return new ByteArrayInputStream(data()) {
                @Override
                public void close() throws IOException {
                    closed = true;
                    super.close();
                }
            };
        }

        @Override
        public HttpRequest.BodyPublisher toBodyPublisher() {
            return HttpRequest.BodyPublishers.ofByteArray(data());
        }

        @Override
        public void close() {
            closed = true;
            json = null;
            jsonStringBytes = null;
        }

        @Override
        public void buffer() {
            if(closed)
                throw new IllegalStateException("Body has already been closed, cannot buffer anymore");
        }

        @Override
        public void writeTo(OutputStream out) {
            if(closed)
                throw new IllegalStateException("Body has already been closed, cannot be written anymore");
            Json.write(json, out, formatted);
        }

        @Override
        public Object toJson() {
            if(closed) throw new IllegalStateException("Body has been closed");
            return new JsonObject("json", json);
        }

        @Override
        public JsonElement json() {
            if(closed) throw new IllegalStateException("Body has been closed");
            return JsonElement.wrap(json);
        }

        @Override
        public Document xml() {
            throw new XMLParseException("Data is in JSON format");
        }

        @Override
        public Document xml(long options) {
            throw new XMLParseException("Data is in JSON format");
        }

        @Override
        public Multipart asMultipart() {
            throw new MultipartSyntaxException("Data is in JSON format");
        }
    }

    class OfData implements Body {
        byte[] data;
        private final boolean jsonAsString;
        public OfData(byte[] bytes, boolean jsonAsString) {
            data = Arguments.checkNull(bytes, "bytes");
            this.jsonAsString = jsonAsString;
        }
        @Override
        public long contentLength() {
            return data().length;
        }

        @Override
        public synchronized byte[] data() {
            if(data == null)
                throw new IllegalStateException("Body has been closed");
            return data;
        }

        @Override
        public Multipart asMultipart() {
            return Multipart.parse(this);
        }

        @Override
        public synchronized InputStream stream() {
            return new ByteArrayInputStream(data()) {
                @Override
                public void close() throws IOException {
                    data = null;
                    super.close();
                }
            };
        }

        @Override
        public HttpRequest.BodyPublisher toBodyPublisher() {
            return HttpRequest.BodyPublishers.ofByteArray(data());
        }

        @Override
        public void close() {
            data = null;
        }

        @Override
        public void buffer() {
            if(data == null)
                throw new IllegalStateException("Body has already been closed, cannot buffer anymore");
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            if(data == null)
                throw new IllegalStateException("Body has already been closed, cannot be written anymore");
            out.write(data);
            out.flush();
        }

        @Override
        public Object toJson() {
            if(data == null) throw new IllegalStateException("Body has been closed");
            return new JsonObject(jsonAsString ? "text" : "base64", jsonAsString ? text() : Utils.toBase64(data()));
        }
    }
}

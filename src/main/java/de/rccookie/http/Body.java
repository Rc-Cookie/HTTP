package de.rccookie.http;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.rccookie.json.Json;
import de.rccookie.json.JsonElement;
import de.rccookie.util.Arguments;
import de.rccookie.util.Lazy;
import de.rccookie.util.MappingIterator;
import de.rccookie.util.StringInputStream;
import de.rccookie.util.UncheckedException;
import de.rccookie.util.Utils;
import de.rccookie.xml.Document;
import de.rccookie.xml.Node;
import de.rccookie.xml.XML;
import de.rccookie.xml.XMLParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The content of an http request or response. Some body implementations
 * may the data to only be read once.
 */
public interface Body extends AutoCloseable {

    /**
     * A body with no content (an empty string).
     */
    Body EMPTY = new Body() {
        @Override
        public long contentLength() {
            return 0;
        }

        @Override
        public InputStream stream() {
            return InputStream.nullInputStream();
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
    };

    /**
     * Returns the length of this body as the number of bytes it contains, or -1 if unknown.
     *
     * @return The length of this body
     */
    long contentLength();

    /**
     * Returns an input stream over the content.
     *
     * @return An input stream over the data
     */
    InputStream stream();

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
     * Represents a multipart http body.
     */
    interface Multipart extends Body, Iterable<Multipart.Part> {

        @Override
        default long contentLength() {
            int boundary = ("--" + boundary()).getBytes().length;
            int newline = "\r\n".getBytes().length;

            long length = 0;
            Collection<Part> parts = parts().values();
            for(Part part : parts) {
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
         * Returns the body parts mapped to their names, in proper order.
         *
         * @return The multipart parts
         */
        Map<String, Part> parts();

        @NotNull
        @Override
        default Iterator<Part> iterator() {
            return parts().values().iterator();
        }

        @SuppressWarnings("DuplicatedCode")
        @Override
        default InputStream stream() {
            List<InputStream> output = new ArrayList<>();

            String boundary = "--" + boundary();
            StringBuilder str = new StringBuilder();
            for(Part part : parts().values()) {
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
        default byte[] data() {
            return text().getBytes();
        }

        @SuppressWarnings("DuplicatedCode")
        @Override
        default String text() {
            String boundary = "--" + boundary();
            StringBuilder str = new StringBuilder();
            for(Part part : parts().values()) {
                str.append(boundary).append("\r\n");
                str.append("Content-Disposition: form-data; name=");
                str.append(Json.toString(part.name())); // Encode and enquote string
                if(part.filename() != null)
                    str.append("; filename=").append(Json.toString(part.filename()));
                if(part.contentType() != null)
                    str.append("\r\nContent-Type: ").append(part.contentType());
                str.append("\r\n\r\n");
                str.append(part.body().text());
                str.append("\r\n");
            }
            str.append(boundary).append("--");
            return str.toString();
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

        /**
         * Represents a single part of a multipart body.
         */
        interface Part {
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
         * @param data The data to parse. If this already is a multipart it will be returned directly
         * @return A multipart parsed from the given data
         * @apiNote The data may not be parsed immediately, at least not completely, so parsing exceptions
         * may occur after this method terminates
         */
        static Multipart parse(Body data) {
            if(Arguments.checkNull(data, "data") instanceof Multipart)
                return (Multipart) data;

            String boundary = Long.toHexString(System.currentTimeMillis()) + Long.toHexString(System.nanoTime());
            Map<String,Part> parts = Lazy.orderedMap(new MappingIterator<>(new MultipartStream(data.stream(), Charset.defaultCharset()), p -> Map.entry(p.name(), p)));
            return new Multipart() {
                @Override
                public String boundary() {
                    return boundary;
                }
                @Override
                public Map<String,Part> parts() {
                    return parts;
                }
            };
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
        return new Body() {
            @Override
            public long contentLength() {
                try {
                    if(stream instanceof ByteArrayInputStream)
                        return stream.available();
                    if(stream instanceof FileInputStream)
                        return ((FileInputStream) stream).getChannel().size();
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
            public void close() throws Exception {
                stream.close();
            }
        };
    }

    /**
     * Returns a body with the given bytes as content.
     *
     * @param bytes The content of the body
     * @return A body with the bytes as data
     */
    static Body of(byte[] bytes) {
        Arguments.checkNull(bytes, "bytes");
        return new OfData(bytes);
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
            public void close() {
                str = null;
                bytes = null;
            }
        };
    }

    /**
     * Returns a body with the given json as content
     *
     * @param json The content of the body, must be json-serializable
     * @return A body with the json as data
     */
    static Body ofJson(Object json) {
        return new OfData(Json.toString(json).getBytes()) {
            @Override
            public Document xml() {
                throw new XMLParseException("Data is in JSON format");
            }
            @Override
            public Document xml(long options) {
                throw new XMLParseException("Data is in JSON format");
            }
        };
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
        return ofXML(xml, 0);
    }

    /**
     * Returns a body with the given xml as content.
     *
     * @param xml The content of the body
     * @param options Determines how to serialize the xml to a string
     * @return A body with the xml as data
     */
    static Body ofXML(Node xml, long options) {
        Arguments.checkNull(xml, "xml");
        return new OfData(xml.toXML(options).getBytes()) {
            @Override
            public JsonElement json() {
                throw new RuntimeException("Data in in XML format");
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

    class OfData implements Body {
        byte[] data;
        public OfData(byte[] bytes) {
            data = Arguments.checkNull(bytes, "bytes");
        }
        @Override
        public long contentLength() {
            return data().length;
        }

        @Override
        public synchronized byte[] data() {
            if(data == null) throw new IllegalStateException("Body has been closed");
            return data;
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
        public void close() {
            data = null;
        }
    }
}

package de.rccookie.http.server;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

import de.rccookie.http.Body;
import de.rccookie.http.util.BodyWriter;
import de.rccookie.http.ContentType;
import de.rccookie.http.HttpResponse;
import de.rccookie.xml.Document;
import de.rccookie.xml.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Serializer<T> {

    Class<? extends Serializer<?>> JSON = Json.class;
    Class<? extends Serializer<?>> XML = Serializer.XML.class;
    Class<? extends Serializer<?>> HTML = Serializer.HTML.class;
    Class<? extends Serializer<?>> XHTML = Serializer.XHTML.class;
    Class<? extends Serializer<?>> DEFAULT = Default.class;
    Class<? extends Serializer<?>> PLAIN = Plain.class;
    Class<? extends Serializer<?>> BINARY = Binary.class;



    void write(HttpResponse.Editable response, @NotNull T value);

    Class<? extends T>[] requiredTypes();

    @Nullable
    ContentType contentType(@Nullable T value);


    final class Json implements Serializer<Object> {
        @Override
        public void write(HttpResponse.Editable response, @NotNull Object value) {
            response.setJson(value);
        }

        @Override
        public Class<?>[] requiredTypes() {
            return new Class<?>[0];
        }

        @Override
        public ContentType contentType(@Nullable Object value) {
            return ContentType.JSON;
        }
    }

    final class XML implements Serializer<Node> {
        private final long options;

        public XML(long options) {
            this.options = options;
        }
        public XML() {
            this(de.rccookie.xml.XML.XML);
        }

        @Override
        public void write(HttpResponse.Editable response, @NotNull Node value) {
            response.setXML(value, options);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends Node>[] requiredTypes() {
            return new Class[] { Node.class };
        }

        @Override
        public ContentType contentType(@Nullable Node value) {
            return ContentType.XML;
        }
    }

    final class HTML implements Serializer<Document> {
        @Override
        public void write(HttpResponse.Editable response, @NotNull Document value) {
            response.setHTML(value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends Document>[] requiredTypes() {
            return new Class[] { Document.class };
        }

        @Override
        public ContentType contentType(@Nullable Document value) {
            return ContentType.HTML;
        }
    }

    final class XHTML implements Serializer<Document> {
        @Override
        public void write(HttpResponse.Editable response, @NotNull Document value) {
            response.setXML(value, de.rccookie.xml.XML.XHTML);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends Document>[] requiredTypes() {
            return new Class[] { Document.class };
        }

        @Override
        public ContentType contentType(@Nullable Document value) {
            return ContentType.HTML;
        }
    }

    final class Default implements Serializer<Object> {

        @Override
        public void write(HttpResponse.Editable response, @NotNull Object value) {
            if(value instanceof byte[])
                response.setData((byte[]) value);
            else if(value instanceof InputStream)
                response.setStream((InputStream) value);
            else if(value instanceof File)
                response.setFile((File) value);
            else if(value instanceof Path)
                response.setFile((Path) value);
            else if(value instanceof Body)
                response.setBody((Body) value);
            else if(value instanceof BodyWriter)
                response.setBody(Body.ofWriter((BodyWriter) value));
            else {
                ContentType contentType = contentType(value);
                if(contentType == ContentType.PLAINTEXT)
                    response.setText((String) value);
                else if(contentType == ContentType.JSON)
                    response.setJson(value);
                else if(contentType == ContentType.XML)
                    response.setXML((Node) value);
                else if(contentType == ContentType.HTML)
                    response.setHTML((Document) value);
                else response.setXML((Document) value, de.rccookie.xml.XML.XHTML);
            }
        }

        @Override
        public Class<?>[] requiredTypes() {
            return new Class<?>[0];
        }

        @Override
        public ContentType contentType(@Nullable Object value) {
            if(value instanceof Body || value instanceof BodyWriter)
                return null;
            if(value instanceof byte[] || value instanceof InputStream)
                return ContentType.BINARY;
            if(value instanceof File || value instanceof Path)
                return ContentType.guessFromName(value.toString());
            if(value instanceof String)
                return ContentType.PLAINTEXT;
            if(!(value instanceof Node))
                return ContentType.JSON;
            if(!(value instanceof Document))
                return ContentType.XML;

            Node root = ((Document) value).rootNode();
            if(root == null || !root.tag.equalsIgnoreCase("html"))
                return ContentType.XML;
            return "http://www.w3.org/1999/xhtml".equalsIgnoreCase(root.attribute("xmlns")) ? ContentType.XHTML : ContentType.HTML;
        }
    }

    final class Plain implements Serializer<Object> {

        @Override
        public void write(HttpResponse.Editable response, @NotNull Object value) {
            response.setText(Objects.toString(value));
        }

        @Override
        public Class<?>[] requiredTypes() {
            return new Class<?>[0];
        }

        @Override
        public ContentType contentType(@Nullable Object value) {
            return ContentType.PLAINTEXT;
        }
    }

    final class Binary implements Serializer<Object> {

        @Override
        public void write(HttpResponse.Editable response, @NotNull Object value) {
            if(value instanceof byte[])
                response.setData((byte[]) value);
            else response.setStream((InputStream) value);
        }

        @Override
        public Class<?>[] requiredTypes() {
            return new Class[] {
                    byte[].class,
                    InputStream.class
            };
        }

        @Override
        public ContentType contentType(@Nullable Object value) {
            return ContentType.BINARY;
        }
    }
}

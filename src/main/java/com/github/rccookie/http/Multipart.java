package com.github.rccookie.http;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.github.rccookie.json.Json;
import com.github.rccookie.util.Arguments;
import com.github.rccookie.xml.Node;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A in-place builder for multipart form data.
 */
public class Multipart {

    /**
     * The boundary for the multipart data.
     */
    private final String boundary = Long.toHexString(System.currentTimeMillis()) + Long.toHexString(System.nanoTime());;

    /**
     * The data elements.
     */
    private final List<FormData> data = new ArrayList<>();



    /**
     * Adds the given xml data block to this multipart form data.
     *
     * @param name The name of this block
     * @param xml The xml data in the block
     * @return This instance
     */
    public Multipart addXML(@NotNull String name, @NotNull Node xml) {
        return addXML(name, null, xml);
    }

    /**
     * Adds the given xml data block to this multipart form data.
     *
     * @param name The name of this block
     * @param filename The filename parameter for the block, or null
     * @param xml The xml data in the block
     * @return This instance
     */
    public Multipart addXML(@NotNull String name, @Nullable String filename, @NotNull Node xml) {
        return add(name, filename, "application/xml", xml.toXML());
    }

    /**
     * Adds the given html data block to this multipart form data.
     *
     * @param name The name of this block
     * @param html The html data in the block
     * @return This instance
     */
    public Multipart addHTML(@NotNull String name, @NotNull Node html) {
        return addHTML(name, null, html);
    }

    /**
     * Adds the given html data block to this multipart form data.
     *
     * @param name The name of this block
     * @param filename The filename parameter for the block, or null
     * @param html The html data in the block
     * @return This instance
     */
    public Multipart addHTML(@NotNull String name, @Nullable String filename, @NotNull Node html) {
        return add(name, filename, "text/html", html.toHTML(0));
    }

    /**
     * Adds the given json data block to this multipart form data.
     *
     * @param name The name of this block
     * @param json The data in the block. Must be json-convertible
     * @return This instance
     */
    public Multipart addJson(@NotNull String name, Object json) {
        return addJson(name, null, json);
    }

    /**
     * Adds the given json data block to this multipart form data.
     *
     * @param name The name of this block
     * @param filename The filename parameter for the block, or null
     * @param json The data in the block. Must be json-convertible
     * @return This instance
     */
    public Multipart addJson(@NotNull String name, @Nullable String filename, Object json) {
        return add(name, filename, "application/json", Json.toString(json));
    }

    /**
     * Adds the given data block to this multipart form data.
     *
     * @param name The name of this block
     * @param data The data in the block
     * @return This instance
     */
    public Multipart add(@NotNull String name, @NotNull String data) {
        return add(name, null, data);
    }

    /**
     * Adds the given data block to this multipart form data.
     *
     * @param name The name of this block
     * @param filename The filename parameter for the block, or null
     * @param data The data in the block
     * @return This instance
     */
    public Multipart add(@NotNull String name, @Nullable String filename, @NotNull String data) {
        return add(name, filename, null, data);
    }

    /**
     * Adds the given data block to this multipart form data.
     *
     * @param name The name of this block
     * @param filename The filename parameter for the block, or null
     * @param contentType The content type for this block, or null
     * @param data The data in the block
     * @return This instance
     */
    public Multipart add(@NotNull String name, @Nullable String filename, @Nullable String contentType, @NotNull String data) {
        return add(name, filename, contentType, data.getBytes());
    }

    /**
     * Adds the given data block to this multipart form data.
     *
     * @param name The name of this block
     * @param data The data in the block
     * @return This instance
     */
    public Multipart add(@NotNull String name, byte @NotNull [] data) {
        return add(name, null, data);
    }

    /**
     * Adds the given data block to this multipart form data.
     *
     * @param name The name of this block
     * @param data The data in the block
     * @return This instance
     */
    public Multipart add(@NotNull String name, @Nullable String filename, byte @NotNull [] data) {
        return add(name, filename, null, data);
    }

    /**
     * Adds the given data block to this multipart form data.
     *
     * @param name The name of this block
     * @param filename The filename parameter for the block, or null
     * @param contentType The content type for this block, or null
     * @param data The data in the block
     * @return This instance
     */
    public Multipart add(@NotNull String name, @Nullable String filename, @Nullable String contentType, byte @NotNull [] data) {
        this.data.add(new FormData(name, filename, contentType, data));
        return this;
    }


    /**
     * Converts the multipart form data to its byte representation.
     *
     * @return This multipart, as bytes
     */
    public byte[] getBytes() {
        String boundary = "--" + this.boundary;
        byte[] boundaryBytes = boundary.getBytes();
        byte[] newline = "\n".getBytes();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        for(FormData data : this.data) {
            bytes.writeBytes(boundaryBytes);
            bytes.writeBytes(newline);

            StringBuilder header = new StringBuilder("Content-Disposition: form-data; name=");
            header.append(Json.toString(data.name)); // "..."

            if(data.filename != null)
                header.append("; filename=").append(Json.toString(data.filename));

            if(data.type != null)
                header.append("\nContent-Type: ").append(data.type);

            header.append("\n\n");
            bytes.writeBytes(header.toString().getBytes());

            bytes.writeBytes(data.bytes);
            bytes.writeBytes(newline);
        }

        bytes.writeBytes(boundaryBytes);
        bytes.writeBytes("--".getBytes());

        return bytes.toByteArray();
    }

    /**
     * Returns the boundary used for this multipart form data.
     *
     * @return The boundary
     */
    public String getBoundary() {
        return boundary;
    }


    private static class FormData {
        @Nullable
        final String type;
        final String name;
        final byte[] bytes;
        @Nullable
        final String filename;

        private FormData(@NotNull String name, @Nullable String filename, @Nullable String type, byte @NotNull [] bytes) {
            this.type = type;
            this.name = Arguments.checkNull(name, "name");
            this.bytes = Arguments.checkNull(bytes, "data");
            this.filename = filename;
        }
    }
}

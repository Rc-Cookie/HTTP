package com.github.rccookie.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.rccookie.json.Json;
import com.github.rccookie.json.JsonElement;
import com.github.rccookie.xml.Document;
import com.github.rccookie.xml.XML;

import org.jetbrains.annotations.NotNull;

/**
 * A fully loaded response from an HTTP request.
 */
public final class LoadedResponse {

    /**
     * The response code for the request. Anything &gt;= 400 means error.
     */
    public final int code;
    /**
     * The response data sent by the server.
     */
    @NotNull
    public final String data;
    /**
     * The raw response data sent by the server.
     */
    public final byte @NotNull[] bytes;
    /**
     * Header fields in the response.
     */
    @NotNull
    public final Map<String,String> header;
    /**
     * Whether the request was successful, which is true exactly when
     * {@link #code} &lt; 400.
     */
    public final boolean success;


    /**
     * Json generated from the data.
     */
    private JsonElement json = null;

    /**
     * XML parsed from the data, using different parsing flags.
     */
    private final Map<Long, Document> xmlDocs = new HashMap<>();


    /**
     * Creates a new HTTP response from the given response data.
     *
     * @param code The response code
     * @param data The response content
     * @param header The response header fields
     */
    LoadedResponse(int code, byte @NotNull[] bytes, @NotNull String data, @NotNull Map<String,String> header) {
        this.code = code;
        this.bytes = bytes;
        this.data = new String(bytes);
        //noinspection Java9CollectionFactory
        this.header = Collections.unmodifiableMap(new HashMap<>(header));
        this.success = code < 400;
    }

    /**
     * Returns the response data parsed as json. Multiple calls to this method
     * will cache the result.
     *
     * @return The json element representing the json data.
     */
    public JsonElement json() {
        return json != null ? json : (json = Json.parse(data));
    }

    /**
     * Parses the response data as HTML.
     *
     * @return The parsed HTML
     */
    public Document html() {
        return xml(XML.HTML);
    }

    /**
     * Parses the response data as XML.
     *
     * @return The parsed XML
     */
    public Document xml() {
        return xml(0);
    }

    /**
     * Parses the response data as XML using the given parsing flags.
     *
     * @param options Options to pass to the XML parser.
     * @return The parsed XML
     */
    public Document xml(long options) {
        Document xml = xmlDocs.get(options);
        if(xml != null) return xml;
        xmlDocs.put(options, xml = XML.parse(data, options));
        return xml;
    }

    /**
     * Returns the response code and the response data.
     *
     * @return A string representation of this object
     */
    @Override
    public String toString() {
        return "Response code: " + code + "\n" + data;
    }
}

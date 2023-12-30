package de.rccookie.http;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;

import de.rccookie.json.JsonElement;
import de.rccookie.util.Future;
import de.rccookie.xml.Document;
import de.rccookie.xml.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * Represents the response of a server to an http request.
 */
public interface HttpResponse {

    /**
     * The status code of the request.
     *
     * @return The response code
     */
    ResponseCode code();

    /**
     * Determines whether the request succeeded, which is the case if the response code is
     * not an error code, which is if the response code is <400.
     *
     * @return Whether the request succeeded
     */
    default boolean success() {
        return code().success();
    }

    /**
     * Returns the response header.
     *
     * @return The header of the response
     */
    Header header();

    /**
     * Shorthand for <code>header().getContentType()</code>.
     *
     * @return The response type of the request, if any
     */
    default ContentType contentType() {
        return header().getContentType();
    }

    /**
     * Shorthand for <code>header().getSetCookies()</code>.
     *
     * @return The cookies to be set by the client; the content of the <code>"Set-Cookie"</code> header field
     */
    @NotNull
    default Map<String, Cookie> cookies() {
        return header().getSetCookies();
    }

    /**
     * Returns the response body.
     *
     * @return The content of this response
     */
    Body body();

    /**
     * Returns <code>body().stream()</code> if this response has a body, otherwise <code>null</code>.
     *
     * @return The content of this response as input stream, if any
     */
    default InputStream stream() {
        Body body = body();
        return body != null ? body.stream() : null;
    }

    /**
     * Returns <code>body().data()</code> if this response has a body, otherwise <code>null</code>.
     *
     * @return The content of this response as bytes, if any
     */
    default byte[] data() {
        Body body = body();
        return body != null ? body.data() : null;
    }

    /**
     * Returns <code>body().text()</code> if this response has a body, otherwise <code>null</code>.
     *
     * @return The content of this response as string, if any
     */
    default String text() {
        Body body = body();
        return body != null ? body.text() : null;
    }

    /**
     * Returns <code>body().json()</code> if this response has a body, otherwise <code>null</code>.
     *
     * @return The content of this response parsed from json, if any
     */
    default JsonElement json() {
        Body body = body();
        return body != null ? body.json() : null;
    }

    /**
     * Returns <code>body().xml()</code> if this response has a body, otherwise <code>null</code>.
     *
     * @return The content of this response parsed from xml, if any
     */
    default Document xml() {
        Body body = body();
        return body != null ? body.xml() : null;
    }

    /**
     * Returns <code>body().html()</code> if this response has a body, otherwise <code>null</code>.
     *
     * @return The content of this response parsed from html, if any
     */
    default Document html() {
        Body body = body();
        return body != null ? body.html() : null;
    }

    /**
     * Returns whether this request uses https or plain http.
     *
     * @return Whether this request uses https
     */
    boolean isHttps();

    /**
     * Returns the http version used for the request and response, for example "1.1".
     *
     * @return The http version used
     */
    String version();

    /**
     * Returns the sender's ip address.
     *
     * @return The ip address of the client
     */
    InetSocketAddress client();

    /**
     * Returns the server's ip address.
     *
     * @return The ip address of the server
     */
    InetSocketAddress server();

    /**
     * Returns the state of this http response.
     *
     * @return The current state of this response
     */
    State state();

    /**
     * Returns the http request that this response responds to. Note that the
     * body of the request might be closed and unreadable.
     *
     * @return The request this response responds to
     */
    @SuppressWarnings("NullableProblems")
    @NotNull
    HttpRequest request();



    /**
     * Serializes this http response into its string representation, without serializing the body.
     *
     * @return A http-protocol conform string representing this http response
     */
    default String toStringWithoutBody() {
        StringBuilder str = new StringBuilder();
        ResponseCode code = code();
        if(!code.isKnown()) throw new RuntimeException("Unknown status code");
        str.append("HTTP/").append(version()).append(' ').append(code).append("\r\n");
        header().forEach((k,vs) -> vs.forEach(v -> str.append(k).append(": ").append(v).append("\r\n")));
        return str.toString();
    }



    /**
     * Represents an unsent response to an http request which can be edited and sent.
     */
    interface Sendable extends HttpResponse {

        /**
         * Sets the status code of this request.
         *
         * @param code The response code to set
         * @return This response
         */
        Sendable setCode(ResponseCode code);

        /**
         * Sets the status code of this request.
         *
         * @param code The response code to set
         * @return This response
         */
        default Sendable setCode(@Range(from = 100, to = 599) int code) {
            return setCode(ResponseCode.get(code));
        }

        /**
         * Shorthand for <code>header().add(key, value)</code>.
         *
         * @param key The name of the header field to add a value to
         * @param value The value to add to that header field
         * @return This response
         */
        default Sendable addHeaderField(String key, String value) {
            header().add(key, value);
            return this;
        }

        /**
         * Shorthand for <code>header().set(key, values)</code>.
         *
         * @param key The name of the header field to set
         * @param values The values to set
         * @return This response
         */
        default Sendable setHeaderField(String key, @Nullable Header.Values values) {
            header().set(key, values);
            return this;
        }

        /**
         * Shorthand for <code>header().set(key, value)</code>.
         *
         * @param key The name of the header field to set
         * @param value The value to set
         * @return This response
         */
        default Sendable setHeaderField(String key, @Nullable String value) {
            header().set(key, value);
            return this;
        }

        /**
         * Shorthand for <code>header().setContentType(contentType)</code>.
         *
         * @param contentType The mime type to set as content type
         * @return This response
         */
        default Sendable setContentType(@Nullable ContentType contentType) {
            header().setContentType(contentType);
            return this;
        }

        /**
         * Shorthand for <code>header().addSetCookie(cookie)</code>.
         *
         * @param cookie The cookie to add to the <code>"Set-Cookie"</code> header field.
         * @return This response
         */
        default Sendable addCookie(Cookie cookie) {
            header().addSetCookie(cookie);
            return this;
        }

        /**
         * Shorthand for <code>header().addSetCookie(Cookie.of(name, value))</code>.
         *
         * @param name The name of the cookie to add to the <code>"Set-Cookie"</code> field
         * @param value The value of the cookie to add to the <code>"Set-Cookie"</code> field
         * @return This response
         */
        default Sendable addCookie(String name, String value) {
            return addCookie(Cookie.of(name, value));
        }

        /**
         * Sets the content of this http response.
         *
         * @param body The response body to set
         * @return This response
         */
        Sendable setBody(Body body);

        /**
         * Shorthand for <code>setBody(Body.of(stream))</code>.
         *
         * @param stream The stream to use as source of the response data. Must be open
         * @return This response
         * @see Body#of(InputStream)
         */
        default Sendable setStream(InputStream stream) {
            return setBody(Body.of(stream));
        }

        /**
         * Shorthand for <code>setBody(Body.of(bytes))</code>.
         *
         * @param bytes The bytes to send in the response
         * @return This response
         * @see Body#of(byte[])
         */
        default Sendable setData(byte[] bytes) {
            return setBody(Body.of(bytes));
        }

        /**
         * Shorthand for <code>setBody(Body.of(data))</code>.
         *
         * @param data The test to send in the response
         * @return This response
         * @see Body#of(String)
         */
        default Sendable setText(String data) {
            return setBody(Body.of(data));
        }

        /**
         * Shorthand for <code>setBody(Body.ofJson(data))</code>. Also sets the
         * content type to <code>application/json</code>.
         *
         * @param data The json data to send in the response, must be json-serializable
         * @return This response
         * @see Body#ofJson(Object)
         */
        default Sendable setJson(Object data) {
            return setBody(Body.ofJson(data)).setContentType(ContentType.JSON);
        }

        /**
         * Shorthand for <code>setBody(Body.ofXML(xml, options))</code>.
         *
         * @param xml The xml data to send in the response
         * @param options Determines how the xml data should be serialized into a string
         * @return This response
         * @see Body#ofXML(Node,long)
         */
        default Sendable setXML(Node xml, long options) {
            return setBody(Body.ofXML(xml, options));
        }

        /**
         * Shorthand for <code>setBody(Body.ofXML(xml))</code>. Also sets the
         * content type to <code>application/json</code>.
         *
         * @param xml The xml data to send in the response
         * @return This response
         * @see Body#ofXML(Node)
         */
        default Sendable setXML(Node xml) {
            return setBody(Body.ofXML(xml)).setContentType(ContentType.XML);
        }

        /**
         * Shorthand for <code>setBody(Body.ofHtml(html))</code>. Also sets the
         * content type to <code>application/json</code>.
         *
         * @param html The html data to send in the response
         * @return This response
         * @see Body#ofHTML(Document)
         */
        default Sendable setHTML(Document html) {
            return setBody(Body.ofHTML(html)).setContentType(ContentType.HTML);
        }

        /**
         * Asynchronously sends the http response back to the client. This also reads the response's body,
         * if any, and closes both its body and the request's body, if any.
         *
         * @return A future which fulfills when the response has been sent
         */
        Future<Void> sendAsync();

        /**
         * Synchronously sends the http response back to the client. Unlike the asynchronous execution,
         * if an exception occurs it will be propagated through this method call. This also reads the
         * response's body, if any, and closes both its body and the request's body, if any.
         */
        void send();
    }

    /**
     * Different lifetime states of an http response.
     */
    enum State {
        /**
         * The http response has not yet been (attempted to be) sent and can
         * be edited. Server-side only.
         */
        EDITABLE,
        /**
         * The http request has already been (attempted to be) sent and cannot
         * be edited. This is particularly true for responses received by the
         * client.
         */
        SENT,
        /**
         * The http response has not yet been (attempted to be) sent, and has
         * been invalidated. Accessing the response may cause exceptions, and
         * the body (if any) has been closed.
         */
        INVALID
    }
}

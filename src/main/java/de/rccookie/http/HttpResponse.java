package de.rccookie.http;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import de.rccookie.json.JsonElement;
import de.rccookie.util.Arguments;
import de.rccookie.util.Future;
import de.rccookie.xml.Document;
import de.rccookie.xml.FormData;
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
     * Determines whether the request succeeded and the requested action is performed, which
     * is the case iff the response code is >=200 and &le;300.
     *
     * <p>This method differs from {@link #success()} in that it returns <code>false</code> for
     * <code>1XX</code> and <code>3XX</code> response codes.</p>
     *
     * @return Whether the request succeeded
     */
    default boolean ok() {
        return code().ok();
    }

    /**
     * Returns the response header.
     *
     * @return The header of the response
     */
    Header header();

    /**
     * Returns the value of the <code>"Date"</code> header field, parsed as RFC 1123 timestamp.
     *
     * @return The response date, or <code>null</code> if not present
     * @see Header#getDate()
     */
    default Instant date() {
        return header().getDate();
    }

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
     * Returns <code>body().asMultipart()</code> if this response has a body, otherwise <code>null</code>.
     *
     * @return The content of this response parsed as <code>multipart/formdata</code>, if any
     */
    default Body.Multipart multipart() {
        Body body = body();
        return body != null ? body.asMultipart() : null;
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
    default String toHttpWithoutBody() {
        StringBuilder str = new StringBuilder();
        str.append("HTTP/").append(version()).append(' ');

        ResponseCode code = code();
        if(code.isKnown())
            str.append(code);
        else str.append(code.code());
        str.append("\r\n");

        header().forEach((k,vs) -> vs.forEach(v -> str.append(k).append(": ").append(v).append("\r\n")));
        return str.toString();
    }

    /**
     * Serializes this http response into its string representation, including the body (which may consume
     * the body - use {@link Body#buffer()} if you want to further use the response body).
     *
     * @return A http-protocol conform string representing this http response
     */
    default String toHttp() {
        Body body = body();
        if(body == null)
            return toHttpWithoutBody();
        return toHttpWithoutBody() + "\n" + body.text();
    }



    /**
     * Represents an unsent response to an http request which can be edited.
     */
    interface Editable extends HttpResponse {

        /**
         * Sets the status code of this request.
         *
         * @param code The response code to set
         * @return This response
         */
        Editable setCode(ResponseCode code);

        /**
         * Sets the status code of this request.
         *
         * @param code The response code to set
         * @return This response
         */
        default Editable setCode(@Range(from = 100, to = 599) int code) {
            return setCode(ResponseCode.get(code));
        }

        /**
         * Shorthand for <code>header().add(key, value)</code>.
         *
         * @param key   The name of the header field to add a value to
         * @param value The value to add to that header field
         * @return This response
         */
        default Editable addHeaderField(String key, String value) {
            header().add(key, value);
            return this;
        }

        /**
         * Shorthand for <code>header().set(key, values)</code>.
         *
         * @param key    The name of the header field to set
         * @param values The values to set
         * @return This response
         */
        default Editable setHeaderField(String key, @Nullable Header.Values values) {
            header().set(key, values);
            return this;
        }

        /**
         * Shorthand for <code>header().set(key, value)</code>.
         *
         * @param key   The name of the header field to set
         * @param value The value to set
         * @return This response
         */
        default Editable setHeaderField(String key, @Nullable String value) {
            header().set(key, value);
            return this;
        }

        /**
         * Shorthand for <code>header().setDate(date)</code>.
         *
         * @param date The date of the response. Usually populated automatically.
         * @return This response
         * @see Header#setDate(Instant)
         */
        default Editable setDate(@Nullable Instant date) {
            header().setDate(date);
            return this;
        }

        /**
         * Shorthand for <code>header().setContentType(contentType)</code>.
         *
         * @param contentType The mime type to set as content type
         * @return This response
         */
        default Editable setContentType(@Nullable ContentType contentType) {
            header().setContentType(contentType);
            return this;
        }

        /**
         * Shorthand for <code>header().addSetCookie(cookie)</code>.
         *
         * @param cookie The cookie to add to the <code>"Set-Cookie"</code> header field.
         * @return This response
         */
        default Editable addCookie(Cookie cookie) {
            header().addSetCookie(cookie);
            return this;
        }

        /**
         * Shorthand for <code>header().addSetCookie(Cookie.of(name, value))</code>.
         *
         * @param name  The name of the cookie to add to the <code>"Set-Cookie"</code> field
         * @param value The value of the cookie to add to the <code>"Set-Cookie"</code> field
         * @return This response
         */
        default Editable addCookie(String name, String value) {
            return addCookie(Cookie.of(name, value));
        }

        /**
         * Sets the content of this http response.
         *
         * @param body The response body to set
         * @return This response
         */
        Editable setBody(Body body);

        /**
         * Shorthand for <code>setBody(Body.of(stream))</code>.
         *
         * @param stream The stream to use as source of the response data. Must be open
         * @return This response
         * @see Body#of(InputStream)
         */
        default Editable setStream(InputStream stream) {
            return setBody(Body.of(stream));
        }

        /**
         * Shorthand for <code>setBody(Body.of(file))</code>. Also attempts to guess the content type based
         * on the file's name.
         *
         * @param file The file whose content to use as the body of this http response
         * @return This http response
         */
        default Editable setFile(Path file) {
            return setFile(file, true);
        }

        /**
         * Shorthand for <code>setBody(Body.of(file))</code>.
         *
         * @param file The file whose content to use as the body of this http response
         * @param guessContentType Whether to attempt to guess the file's content type from its name
         *                         and, on success, override the Content-Type header field
         * @return This http response
         */
        default Editable setFile(Path file, boolean guessContentType) {
            setBody(Body.of(file));
            if(guessContentType) {
                ContentType contentType = ContentType.guessFromName(file.getFileName().toString());
                if(contentType != null)
                    setContentType(contentType);
            }
            return this;
        }

        /**
         * Shorthand for <code>setBody(Body.of(file))</code>. Also attempts to guess the content type based
         * on the file's name.
         *
         * @param file The file whose content to use as the body of this http response
         * @return This http response
         */
        default Editable setFile(File file) {
            return setFile(file, true);
        }

        /**
         * Shorthand for <code>setBody(Body.of(file))</code>.
         *
         * @param file The file whose content to use as the body of this http response
         * @param guessContentType Whether to attempt to guess the file's content type from its name
         *                         and, on success, override the Content-Type header field
         * @return This http response
         */
        default Editable setFile(File file, boolean guessContentType) {
            return setFile(Arguments.checkNull(file, "file").toPath(), guessContentType);
        }

        /**
         * Shorthand for <code>setBody(Body.of(bytes))</code>.
         *
         * @param bytes The bytes to send in the response
         * @return This response
         * @see Body#of(byte[])
         */
        default Editable setData(byte[] bytes) {
            return setBody(Body.of(bytes));
        }

        /**
         * Shorthand for <code>setBody(Body.of(data))</code>.
         *
         * @param data The test to send in the response
         * @return This response
         * @see Body#of(String)
         */
        default Editable setText(String data) {
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
        default Editable setJson(Object data) {
            return setBody(Body.ofJson(data)).setContentType(ContentType.JSON);
        }

        /**
         * Shorthand for <code>setBody(Body.ofXML(xml, options))</code>.
         *
         * @param xml     The xml data to send in the response
         * @param options Determines how the xml data should be serialized into a string
         * @return This response
         * @see Body#ofXML(Node, long)
         */
        default Editable setXML(Node xml, long options) {
            return setBody(Body.ofXML(xml, options));
        }

        /**
         * Shorthand for <code>setBody(Body.ofXML(xml))</code>. Also sets the
         * content type to <code>application/xml</code>.
         *
         * @param xml The xml data to send in the response
         * @return This response
         * @see Body#ofXML(Node)
         */
        default Editable setXML(Node xml) {
            return setBody(Body.ofXML(xml)).setContentType(ContentType.XML);
        }

        /**
         * Shorthand for <code>setBody(Body.ofHtml(html))</code>. Also sets the
         * content type to <code>text/html</code>.
         *
         * @param html The html data to send in the response
         * @return This response
         * @see Body#ofHTML(Document)
         */
        default Editable setHTML(Document html) {
            return setBody(Body.ofHTML(html)).setContentType(ContentType.HTML);
        }

        /**
         * Shorthand for <code>setBody(Body.multipart().add(part[0])...add(part[n]))</code>,
         * also sets the content type to <code>multipart/form-data</code> with the correct boundary.
         *
         * @param parts The multipart parts to send
         * @return This response
         * @see Body#multipart()
         */
        default Editable setMultipart(Body.Multipart.Part... parts) {
            Body.Multipart.Editable multipart = Body.multipart();
            for(Body.Multipart.Part part : parts)
                multipart = multipart.add(part);
            return setMultipart(multipart);
        }

        /**
         * Shorthand for <code>setBody(multipart)</code>, also sets the content type
         * to <code>multipart/form-data</code> with the correct boundary.
         *
         * @param multipart The multipart data to send
         * @return This response
         * @see Body#multipart()
         */
        default Editable setMultipart(Body.Multipart multipart) {
            return setBody(multipart).setContentType(ContentType.multipart(multipart.boundary()));
        }

        /**
         * Shorthand for <code>setBody(Body.Multipart.of(formData))</code>, also sets the
         * content type to <code>multipart/form-data</code> with a randomly generated boundary.
         *
         * @param formData The data for the multipart body
         * @return This response
         * @see Body#of(FormData)
         */
        default Editable setFormData(FormData formData) {
            return setMultipart(Body.of(formData));
        }


        @SuppressWarnings("NullableProblems")
        @Override
        @NotNull
        HttpRequest.Received request();
    }

    /**
     * Represents an unsent response to an http request which can be edited and sent.
     */
    interface Sendable extends Editable {

        @Override
        Sendable setCode(ResponseCode code);

        @Override
        default Sendable setCode(@Range(from = 100, to = 599) int code) {
            return (Sendable) Editable.super.setCode(code);
        }

        @Override
        default Sendable addHeaderField(String key, String value) {
            return (Sendable) Editable.super.addHeaderField(key, value);
        }

        @Override
        default Sendable setHeaderField(String key, Header.@Nullable Values values) {
            return (Sendable) Editable.super.setHeaderField(key, values);
        }

        @Override
        default Sendable setHeaderField(String key, @Nullable String value) {
            return (Sendable) Editable.super.setHeaderField(key, value);
        }

        @Override
        default Sendable setContentType(@Nullable ContentType contentType) {
            return (Sendable) Editable.super.setContentType(contentType);
        }

        @Override
        default Sendable addCookie(Cookie cookie) {
            return (Sendable) Editable.super.addCookie(cookie);
        }

        @Override
        default Sendable addCookie(String name, String value) {
            return (Sendable) Editable.super.addCookie(name, value);
        }

        @Override
        Sendable setBody(Body body);

        @Override
        default Sendable setStream(InputStream stream) {
            return (Sendable) Editable.super.setStream(stream);
        }

        @Override
        default Sendable setData(byte[] bytes) {
            return (Sendable) Editable.super.setData(bytes);
        }

        @Override
        default Sendable setText(String data) {
            return (Sendable) Editable.super.setText(data);
        }

        @Override
        default Sendable setJson(Object data) {
            return (Sendable) Editable.super.setJson(data);
        }

        @Override
        default Sendable setXML(Node xml, long options) {
            return (Sendable) Editable.super.setXML(xml, options);
        }

        @Override
        default Sendable setXML(Node xml) {
            return (Sendable) Editable.super.setXML(xml);
        }

        @Override
        default Sendable setHTML(Document html) {
            return (Sendable) Editable.super.setHTML(html);
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

package de.rccookie.http;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import de.rccookie.json.JsonElement;
import de.rccookie.util.Future;
import de.rccookie.util.URLBuilder;
import de.rccookie.util.UncheckedException;
import de.rccookie.xml.Document;
import de.rccookie.xml.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an http request. Contains static methods to create new http requests using
 * the default http client.
 */
public interface HttpRequest {

    /**
     * The url this request targets.
     *
     * @return The request url
     */
    URL url();

    /**
     * Returns the host name this http request is targeting.
     *
     * @return The host part from the url
     */
    default String host() {
        return url().getHost();
    }

    /**
     * Returns the path that this http request requests, which always starts
     * with a forward slash '/'.
     *
     * @return The path part from the url
     */
    default Path path() {
        return Path.fromUrl(url());
    }

    /**
     * Returns the (readonly) query parameters of this http request.
     *
     * @return The query parameters from the url
     */
    default Query query() {
        return Query.of(url());
    }

    /**
     * Returns whether this request uses https or plain http.
     *
     * @return Whether this request uses https
     */
    default boolean isHttps() {
        return url().getProtocol().equalsIgnoreCase("https");
    }

    /**
     * Returns the http protocol version used by this request, for example "1.1".
     *
     * @return The http version for the request
     */
    String httpVersion();

    /**
     * The request method of this request.
     *
     * @return The request method
     */
    Method method();

    /**
     * The header of this http request.
     *
     * @return The header
     */
    Header header();

    /**
     * Returns the values of the <code>"Cookie"</code> header field, parsed as Cookies.
     *
     * @return The request cookies
     */
    @NotNull
    default Map<String, Cookie> cookies() {
        return header().getCookies();
    }

    /**
     * Returns the value of the <code>"Content-Type"</code> header field.
     *
     * @return The request content type, if specified
     */
    default ContentType contentType() {
        return header().getContentType();
    }

    /**
     * Returns the MIME types in the <code>"Accept"</code> header field, or <code>"*&#47;*"</code>.
     *
     * @return The accepted MIME types for this request
     */
    default ContentTypes accept() {
        return header().getAccept();
    }

    /**
     * The body of this http request.
     *
     * @return The request body
     */
    Body body();

    /**
     * Returns <code>body().stream()</code> if this request has a body, otherwise <code>null</code>.
     *
     * @return The content of this request as input stream, if any
     */
    default InputStream stream() {
        Body body = body();
        return body != null ? body.stream() : null;
    }

    /**
     * Returns <code>body().data()</code> if this request has a body, otherwise <code>null</code>.
     *
     * @return The content of this request as bytes, if any
     */
    default byte[] data() {
        Body body = body();
        return body != null ? body.data() : null;
    }

    /**
     * Returns <code>body().text()</code> if this request has a body, otherwise <code>null</code>.
     *
     * @return The content of this request as string, if any
     */
    default String text() {
        Body body = body();
        return body != null ? body.text() : null;
    }

    /**
     * Returns <code>body().json()</code> if this request has a body, otherwise <code>null</code>.
     *
     * @return The content of this request parsed from json, if any
     */
    default JsonElement json() {
        Body body = body();
        return body != null ? body.json() : null;
    }

    /**
     * Returns <code>body().xml()</code> if this request has a body, otherwise <code>null</code>.
     *
     * @return The content of this request parsed from xml, if any
     */
    default Document xml() {
        Body body = body();
        return body != null ? body.xml() : null;
    }

    /**
     * Returns <code>body().html()</code> if this request has a body, otherwise <code>null</code>.
     *
     * @return The content of this request parsed from html, if any
     */
    default Document html() {
        Body body = body();
        return body != null ? body.xml() : null;
    }

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
     * Serializes this http request into its string representation, without serializing the body.
     *
     * @return A http-protocol conform string representing this http request
     */
    default String toStringWithoutBody() {
        StringBuilder str = new StringBuilder();
        str.append(method()).append(' ').append(url()).append(" HTTP/1.1\r\n");
        header().forEach((k,vs) -> vs.forEach(v -> str.append(k).append(": ").append(v).append("\r\n")));
        return str.toString();
    }


    /**
     * Represents an unsent http request which can be edited and sent.
     */
    interface Unsent extends HttpRequest {

        /**
         * Sets the url this request targets.
         *
         * @param url The target url to set
         * @return This request
         */
        Unsent setUrl(URL url);

        /**
         * Sets the url this request targets.
         *
         * @param url The target url to set
         * @return This request
         */
        default Unsent setUrl(String url) {
            try {
                return setUrl(new URL(url));
            } catch(MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }

        /**
         * Sets whether this request should use https or plain http.
         *
         * @param https Whether to use https
         * @return This request
         */
        default Unsent setHttps(boolean https) {
            return setUrl(new URLBuilder(url()).protocol(https ? "https" : "http").toString());
        }

        /**
         * Sets the http protocol version to be used in the request, for example "1.1".
         *
         * @param version The http protocol version to use
         * @return This request
         */
        Unsent setHttpVersion(String version);

        /**
         * Sets the request method for this request.
         *
         * @param method The request method to use
         * @return This request
         */
        Unsent setMethod(Method method);

        /**
         * Whether this http request should follow redirects.
         *
         * @return Whether to follow redirects
         */
        boolean doRedirects();

        /**
         * Sets whether this request should follow redirects.
         *
         * @param doRedirects Whether to follow redirects
         * @return This request
         */
        Unsent setDoRedirects(boolean doRedirects);

        /**
         * Returns the header of this request, which can be edited to adjust the header.
         *
         * @return The request's header
         */
        @Override
        Header header();

        /**
         * Shorthand for <code>header().add(key, value)</code>.
         *
         * @param key The header key to add a value to
         * @param value The value to add to that header field
         * @return This request
         * @see Header#add(String, String)
         */
        default Unsent addHeaderField(String key, String value) {
            header().add(key, value);
            return this;
        }

        /**
         * Shorthand for <code>header().set(key, value)</code>.
         *
         * @param key The header key to set the value for
         * @param value The value to set
         * @return This request
         * @see Header#set(String, String)
         */
        default Unsent setHeaderField(String key, String value) {
            header().set(key, value);
            return this;
        }

        /**
         * Shorthand for <code>header().set(key, value)</code>.
         *
         * @param key The header key to set the value for
         * @param value The value to set
         * @return This request
         * @see Header#set(String, Header.Values)
         */
        default Unsent setHeaderField(String key, Header.Values value) {
            header().set(key, value);
            return this;
        }

        /**
         * Shorthand for <code>header().setContentType(contentType)</code>.
         *
         * @param contentType The mime type to set as content type
         * @return This request
         * @see Header#setContentType(ContentType)
         */
        default Unsent setContentType(ContentType contentType) {
            header().setContentType(contentType);
            return this;
        }

        /**
         * Shorthand for <code>header().addCookie(cookie)</code>.
         *
         * @param cookie The cookie to add. Only name and value are relevant
         * @return This request
         * @see Header#addCookie(Cookie)
         */
        default Unsent addCookie(Cookie cookie) {
            header().addCookie(cookie);
            return this;
        }

        /**
         * Shorthand for <code>header().addCookie(Cookie.of(name, value))</code>.
         *
         * @param name The name for the cookie to add
         * @param value The value for the cookie to add
         * @return This request
         */
        default Unsent addCookie(String name, String value) {
            return addCookie(Cookie.of(name, value));
        }

        /**
         * Shorthand for <code>header().addCookies(cookies)</code>.
         *
         * @param cookies The cookies to add to the "Cookie" header field
         * @return This request
         */
        default Unsent addCookies(Collection<? extends Cookie> cookies) {
            header().addCookies(cookies);
            return this;
        }

        /**
         * Shorthand for <code>addCookies(cookies.values())</code>. The cookies must be mapped
         * by their name.
         *
         * @param cookies The cookies to add to the "Cookie" header field, mapped to their name
         * @return This request
         */
        default Unsent addCookies(Map<? extends String, ? extends Cookie> cookies) {
            cookies.forEach((n,c) -> {
                if(!n.equals(c.name()))
                    throw new IllegalArgumentException("Inconsistent cookie map: Cookie with name '"+c.name()+"' mapped to different name '"+n+"'");
            });
            return addCookies(cookies.values());
        }

        /**
         * Shorthand for <code>addCookies(cookies.cookies())</code>.
         *
         * @param cookies The cookie store containing the cookies to be added
         * @return This request
         */
        default Unsent addCookies(CookieStore cookies) {
            return addCookies(cookies.cookies());
        }

        /**
         * Shorthand for <code>header().putAccept(contentTypes)</code>.
         *
         * @param contentTypes The mime types of the contents that this request accepts
         * @return This request
         */
        default Unsent setAccept(ContentTypes contentTypes) {
            header().setAccept(contentTypes);
            return this;
        }

        /**
         * Shorthand for <code>setAccept(ContentTypes.of(contentTypes))</code>.
         *
         * @param contentTypes The mime types of the contents that this request accepts
         * @return This request
         */
        default Unsent setAccept(ContentType... contentTypes) {
            return setAccept(ContentTypes.of(contentTypes));
        }

        /**
         * Sets the request's body. May be <code>null</code>. If the body knows its size, the
         * <code>Content-Length</code> header field will also be set, otherwise it will be cleared.
         *
         * @param body The request body to send
         * @return This request
         */
        Unsent setBody(@Nullable Body body);

        /**
         * Shorthand for <code>setBody(HttpBody.of(stream))</code>.
         *
         * @param stream The input stream to use as body source. Must be open
         * @return This request
         * @see Body#of(InputStream)
         */
        default Unsent setData(InputStream stream) {
            return setBody(Body.of(stream));
        }

        /**
         * Shorthand for <code>setBody(HttpBody.of(bytes))</code>.
         *
         * @param bytes The bytes to send in the http request
         * @return This request
         * @see Body#of(byte[])
         */
        default Unsent setData(byte[] bytes) {
            return setBody(Body.of(bytes));
        }

        /**
         * Shorthand for <code>setBody(HttpBody.of(data))</code>.
         *
         * @param data The data to send in the http request
         * @return This request
         * @see Body#of(String)
         */
        default Unsent setData(String data) {
            return setBody(Body.of(data));
        }

        /**
         * Shorthand for <code>setBody(HttpBody.ofJson(data))</code>, also sets the content type
         * to <code>application/json</code>.
         *
         * @param data The json data to send, must be json-serializable
         * @return This request
         * @see Body#ofJson(Object)
         */
        default Unsent setJson(Object data) {
            return setBody(Body.ofJson(data)).setContentType(ContentType.JSON);
        }

        /**
         * Shorthand for <code>setBody(HttpBody.ofXML(xml))</code>.
         *
         * @param xml The xml data to send
         * @param options Determines how to serialize the xml data into a string
         * @return This request
         * @see Body#ofXML(Node,long)
         */
        default Unsent setXML(Node xml, long options) {
            return setBody(Body.ofXML(xml, options));
        }

        /**
         * Shorthand for <code>setBody(HttpBody.ofXML(xml))</code>, also sets the content type
         * to <code>application/xml</code>.
         *
         * @param xml The xml data to send
         * @return This request
         * @see Body#ofXML(Node)
         */
        default Unsent setXML(Node xml) {
            return setBody(Body.ofXML(xml)).setContentType(ContentType.XML);
        }

        /**
         * Shorthand for <code>setBody(HttpBody.ofHTML(html))</code>, also sets the content type
         * to <code>application/html</code>.
         *
         * @param html The html data to send
         * @return This request
         * @see Body#ofHTML(Document)
         */
        default Unsent setHTML(Document html) {
            return setBody(Body.ofHTML(html)).setContentType(ContentType.HTML);
        }

        /**
         * Shorthand for <code>setBody(Body.multipart().add(part[0])...add(part[n]))</code>,
         * also sets the content type to <code>multipart/form-data</code> with the correct boundary.
         *
         * @param parts The multipart parts to send
         * @return This request
         * @see Body#multipart()
         */
        default Unsent setMultipart(Body.Multipart.Part... parts) {
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
         * @return This request
         * @see Body#multipart()
         */
        default Unsent setMultipart(Body.Multipart multipart) {
            return setBody(multipart).setContentType(ContentType.multipart(multipart.boundary()));
        }

        /**
         * Modifies the request url by adding the given url parameter. Also sets the content type
         * of the request to <code>application/x-www-form-urlencoded</code>.
         *
         * @param name The name of the parameter
         * @param value The value of the parameter
         * @return This request
         * @see #addParams(Map)
         */
        default Unsent addParam(String name, String value) {
            return addParam(name, value, true);
        }

        /**
         * Modifies the request url by adding the given url parameter.
         *
         * @param name The name of the parameter
         * @param value The value of the parameter
         * @param setContentType Whether to set the content type to <code>application/x-www-form-urlencoded</code>
         * @return This request
         * @see #addParams(Map, boolean)
         */
        default Unsent addParam(String name, String value, boolean setContentType) {
            return addParams(Map.of(name, value), setContentType);
        }

        /**
         * Modifies the request url by adding the given url parameters. Also sets the content type
         * of the request to <code>application/x-www-form-urlencoded</code>.
         *
         * @param params The parameters to add
         * @return This request
         */
        default Unsent addParams(Map<? extends String, ? extends String> params) {
            return addParams(params, true);
        }

        /**
         * Modifies the request url by adding the given url parameters.
         *
         * @param params The parameters to add
         * @param setContentType Whether to set the content type to <code>application/x-www-form-urlencoded</code>
         * @return This request
         */
        default Unsent addParams(Map<? extends String, ? extends String> params, boolean setContentType) {
            return (setContentType ? setContentType(ContentType.URL_ENCODED) : this)
                    .setUrl(new URLBuilder(url()).addParams(params).toString());
        }

        /**
         * Modifies the request url by setting the given url parameters. Also sets the content type
         * of the request to <code>application/x-www-form-urlencoded</code>.
         *
         * @param params The parameters to set (instead of any current query parameters)
         * @return This request
         */
        default Unsent setParams(Map<? extends String, ? extends String> params) {
            return setParams(params, true);
        }

        /**
         * Modifies the request url by setting the given url parameters.
         *
         * @param params The parameters to set (instead of any current query parameters)
         * @param setContentType Whether to set the content type to <code>application/x-www-form-urlencoded</code>
         * @return This request
         */
        default Unsent setParams(Map<? extends String, ? extends String> params, boolean setContentType) {
            return (setContentType ? setContentType(ContentType.URL_ENCODED) : this)
                    .setUrl(new URLBuilder(url()).setParams(params).toString());
        }

        /**
         * Asynchronously sends this http request. This will also read the request's body, if any,
         * and close it.
         *
         * @return A future which will be fulfilled when the response is received
         */
        Future<HttpResponse> sendAsync();

        /**
         * Synchronously sends this http request. Unlike the asynchronous execution, if an exception
         * occurs it will be propagated through this method call. This will also read the request's body,
         * if and, and close it.
         *
         * @return The received http request
         */
        default HttpResponse send() {
            try {
                return sendAsync().except(() -> {} /* Prevent exception from being logged */).waitFor();
            } catch(IllegalStateException e) {
                Throwable cause = e.getCause();
                if(cause == null) throw e;
                if(cause instanceof Error) throw e;
                throw UncheckedException.of(cause);
            }
        }
    }

    /**
     * Represents a http request received by the server, which the server can respond to.
     */
    interface Received extends HttpRequest {

        /**
         * Returns the response to this http request, or <code>null</code> if not yet created.
         * The response may have already been sent.
         *
         * @return The http response instance, or <code>null</code>
         */
        @Nullable
        HttpResponse.Sendable getResponse();

        /**
         * Returns an unsent, mutable http response responding to this http request. If this method
         * is called multiple times, the previously returned requests are invalidated, or the method
         * throws an exception if the previous request has already been sent.
         *
         * @param code The response code for the response. Can be changed later
         * @return A response to this request, not yet sent
         */
        HttpResponse.Sendable respond(ResponseCode code);

        /**
         * Returns an unsent, mutable http response responding to this http request. If this method
         * is called multiple times, the previously returned requests are invalidated, or the method
         * throws an exception if the previous request has already been sent.
         *
         * @param code The response code for the response. Can be changed later
         * @return A response to this request, not yet sent
         */
        default HttpResponse.Sendable respond(int code) {
            return respond(ResponseCode.get(code));
        }

        /**
         * Returns an unsent, mutable http response responding to this http request, with response
         * code <code>200 OK</code>. If this method is called multiple times, the previously returned
         * requests are invalidated, or the method throws an exception if the previous request has
         * already been sent.
         *
         * @return A response to this request, not yet sent
         */
        default HttpResponse.Sendable respond() {
            return respond(ResponseCode.OK);
        }
    }


    /**
     * HTTP request methods.
     *
     * <p><a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods">Reference</a></p>
     */
    enum Method {
        /**
         * The GET method requests a representation of the specified resource. Requests using GET should only retrieve data.
         */
        GET,
        /**
         * The HEAD method asks for a response identical to a GET request, but without the response body.
         */
        POST,
        /**
         * The POST method submits an entity to the specified resource, often causing a change in state or side effects on the server.
         */
        PUT,
        /**
         * The HEAD method asks for a response identical to a GET request, but without the response body.
         */
        HEAD,
        /**
         * The DELETE method deletes the specified resource.
         */
        DELETE,
        /**
         * The CONNECT method establishes a tunnel to the server identified by the target resource.
         */
        CONNECT,
        /**
         * The OPTIONS method describes the communication options for the target resource.
         */
        OPTIONS,
        /**
         * The TRACE method performs a message loop-back test along the path to the target resource.
         */
        TRACE,
        /**
         * The PATCH method applies partial modifications to a resource.
         */
        PATCH
    }


    /**
     * Creates a new, unsent GET request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent get(String url) {
        return HttpClient.getDefault().get(url);
    }

    /**
     * Creates a new, unsent GET request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent get(URL url) {
        return HttpClient.getDefault().get(url);
    }

    /**
     * Creates a new, unsent POST request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent post(String url) {
        return HttpClient.getDefault().post(url);
    }

    /**
     * Creates a new, unsent POST request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent post(URL url) {
        return HttpClient.getDefault().post(url);
    }

    /**
     * Creates a new, unsent PUT request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent put(String url) {
        return HttpClient.getDefault().put(url);
    }

    /**
     * Creates a new, unsent PUT request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent put(URL url) {
        return HttpClient.getDefault().put(url);
    }

    /**
     * Creates a new, unsent HEAD request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent head(String url) {
        return HttpClient.getDefault().head(url);
    }

    /**
     * Creates a new, unsent HEAD request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent head(URL url) {
        return HttpClient.getDefault().head(url);
    }

    /**
     * Creates a new, unsent DELETE request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent delete(String url) {
        return HttpClient.getDefault().delete(url);
    }

    /**
     * Creates a new, unsent DELETE request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent delete(URL url) {
        return HttpClient.getDefault().delete(url);
    }

    /**
     * Creates a new, unsent OPTIONS request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent options(String url) {
        return HttpClient.getDefault().options(url);
    }

    /**
     * Creates a new, unsent OPTIONS request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent options(URL url) {
        return HttpClient.getDefault().options(url);
    }

    /**
     * Creates a new, unsent TRACE request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent trace(String url) {
        return HttpClient.getDefault().trace(url);
    }

    /**
     * Creates a new, unsent TRACE request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent trace(URL url) {
        return HttpClient.getDefault().trace(url);
    }

    /**
     * Creates a new, unsent CONNECT request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent connect(String url) {
        return HttpClient.getDefault().connect(url);
    }

    /**
     * Creates a new, unsent CONNECT request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent connect(URL url) {
        return HttpClient.getDefault().connect(url);
    }

    /**
     * Creates a new, unsent PATCH request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent patch(String url) {
        return HttpClient.getDefault().patch(url);
    }

    /**
     * Creates a new, unsent PATCH request to the given url.
     *
     * @param url The target url
     * @return A modifiable, unsent http request
     */
    static HttpRequest.Unsent patch(URL url) {
        return HttpClient.getDefault().patch(url);
    }
}

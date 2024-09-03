package de.rccookie.http;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import de.rccookie.http.client.ConnectTimeoutException;
import de.rccookie.http.client.HttpClient;
import de.rccookie.http.server.HttpServer;
import de.rccookie.http.server.RawHttpServer;
import de.rccookie.http.useragent.UserAgent;
import de.rccookie.json.Json;
import de.rccookie.json.JsonDeserializer;
import de.rccookie.json.JsonElement;
import de.rccookie.json.JsonObject;
import de.rccookie.json.JsonSerializable;
import de.rccookie.util.Arguments;
import de.rccookie.util.Cloneable;
import de.rccookie.util.Future;
import de.rccookie.util.URLBuilder;
import de.rccookie.util.UncheckedException;
import de.rccookie.util.login.Login;
import de.rccookie.xml.Document;
import de.rccookie.xml.FormData;
import de.rccookie.xml.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an http request. Contains static methods to create new http requests using
 * the default http client.
 */
public interface HttpRequest extends JsonSerializable {

    int _init = init();
    private static int init() {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        Json.registerDeserializer(HttpRequest.class, json -> {
            HttpClient client = json.get("httpClient").or(HttpClient.class, HttpClient.getDefault());
            HttpRequest.Unsent r = client.get(json.getString("url"))
                    .setMethod(json.get("method").or(Method.class, Method.GET))
                    .setDoRedirects(json.get("doRedirects").or(false))
                    .setBody(json.get("body").as(Body.class));
            r.header().putAll(json.get("header").as(Header.class));
            return r;
        });
        return 0;
    }

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
     * Returns the route that this http request requests, which always starts
     * with a forward slash '/'.
     *
     * @return The route part from the url
     */
    default Route route() {
        return Route.fromUrl(url());
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
     * Returns the query parameters of this http request parsed as the given type.
     * The object will be parsed from json, equivalently to:
     * <pre>
     *     JsonElement.wrap(query(), JsonDeserializer.STRING_CONVERSION).as(type)
     * </pre>
     * This means that all required fields of the type must serialize to a primitive
     * json type (string, number or boolean).
     *
     * @param type The type to parse the query to
     * @return The parsed query
     */
    @NotNull
    default <T> T query(Class<T> type) {
        return JsonElement.wrap(query(), JsonDeserializer.STRING_CONVERSION).as(type);
    }

    /**
     * Returns the query parameters of this http request parsed as the given type.
     * The object will be parsed from json, equivalently to:
     * <pre>
     *     JsonElement.wrap(query(), JsonDeserializer.STRING_CONVERSION).as(rawType, typeParams)
     * </pre>
     * This means that all required fields of the type must serialize to a primitive
     * json type (string, number or boolean).
     *
     * @param rawType The raw type to parse the query to
     * @param typeParams Generic type parameters for the given type
     * @return The parsed query
     */
    @NotNull
    default <T> T query(Class<? super T> rawType, Type... typeParams) {
        return JsonElement.wrap(query(), JsonDeserializer.STRING_CONVERSION).as(rawType, typeParams);
    }

    /**
     * Returns the query parameters of this http request parsed as the given type.
     * The object will be parsed from json, equivalently to:
     * <pre>
     *     JsonElement.wrap(query(), JsonDeserializer.STRING_CONVERSION).as(type)
     * </pre>
     * This means that all required fields of the type must serialize to a primitive
     * json type (string, number or boolean).
     *
     * @param type The (possibly generic) type to parse the query to
     * @return The parsed query
     */
    @NotNull
    default <T> T query(Type type) {
        return JsonElement.wrap(query(), JsonDeserializer.STRING_CONVERSION).as(type);
    }

    /**
     * Returns the (readonly) url-parameter encoded body of this http request (equivalently
     * to <code>body().params()</code>), or an empty query map if this request does not have a body.
     *
     * @return The url parameters from the request body
     */
    default Query params() {
        Body body = body();
        return body != null ? body.params() : Query.EMPTY;
    }

    /**
     * Returns the url-parameter encoded body of this http request parsed as the given type.
     * The object will be parsed from json, equivalently to:
     * <pre>
     *     JsonElement.wrap(params(), JsonDeserializer.STRING_CONVERSION).as(type)
     * </pre>
     * This means that all required fields of the type must serialize to a primitive
     * json type (string, number or boolean).
     *
     * @param type The type to parse the url parameters to
     * @return The parsed url parameters
     */
    @NotNull
    default <T> T params(Class<T> type) {
        return JsonElement.wrap(params(), JsonDeserializer.STRING_CONVERSION).as(type);
    }

    /**
     * Returns the url-parameter encoded body of this http request parsed as the given type.
     * The object will be parsed from json, equivalently to:
     * <pre>
     *     JsonElement.wrap(query(), JsonDeserializer.STRING_CONVERSION).as(rawType, typeParams)
     * </pre>
     * This means that all required fields of the type must serialize to a primitive
     * json type (string, number or boolean).
     *
     * @param rawType The raw type to parse the url parameters to
     * @param typeParams Generic type parameters for the given type
     * @return The parsed url parameters
     */
    @NotNull
    default <T> T params(Class<? super T> rawType, Type... typeParams) {
        return JsonElement.wrap(params(), JsonDeserializer.STRING_CONVERSION).as(rawType, typeParams);
    }

    /**
     * Returns the url-parameter encoded body of this http request parsed as the given type.
     * The object will be parsed from json, equivalently to:
     * <pre>
     *     JsonElement.wrap(query(), JsonDeserializer.STRING_CONVERSION).as(type)
     * </pre>
     * This means that all required fields of the type must serialize to a primitive
     * json type (string, number or boolean).
     *
     * @param type The (possibly generic) type to parse the url parameters to
     * @return The parsed query
     */
    @NotNull
    default <T> T params(Type type) {
        return JsonElement.wrap(params(), JsonDeserializer.STRING_CONVERSION).as(type);
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
     * Returns the values for the given header field joined by newlines, or <code>null</code>
     * if the header field is not present.
     *
     * @param key The name of the header field to get
     * @return The values joined by newlines, or <code>null</code>
     * @see Header#getString(String)
     */
    default String headerField(String key) {
        return header().getString(key);
    }

    /**
     * Returns the values for the given header field, or <code>null</code> if the header field
     * is not present.
     *
     * @param key The name of the header field to get
     * @return The corresponding values, or <code>null</code>
     * @see Header#get(Object)
     */
    default Header.Values headerFields(String key) {
        return header().get(key);
    }

    /**
     * Returns the value of the <code>"Date"</code> header field, parsed as RFC 1123 timestamp.
     *
     * @return The request date, or <code>null</code> if not present
     * @see Header#getDate()
     */
    default Instant date() {
        return header().getDate();
    }

    /**
     * Returns the values of the <code>"Cookie"</code> header field, parsed as Cookies.
     *
     * @return The request cookies
     * @see #cookie(String)
     * @see Header#getCookies()
     */
    @NotNull
    default Map<String, Cookie> cookies() {
        return header().getCookies();
    }

    /**
     * Returns the cookie with the given name from the <code>"Cookie"</code> header field,
     * or <code>null</code> if the cookie is not present.
     *
     * @param name The name of the cookie
     * @return The cookie with that name, or <code>null</code>
     * @see #cookies()
     */
    default Cookie cookie(String name) {
        return header().getCookies().get(name);
    }

    /**
     * Returns the value of the <code>"Content-Type"</code> header field.
     *
     * @return The request content type, if specified
     * @see Header#getContentType()
     */
    default ContentType contentType() {
        return header().getContentType();
    }

    /**
     * Returns the MIME types in the <code>"Accept"</code> header field, or <code>"*&#47;*"</code>.
     *
     * @return The accepted MIME types for this request
     * @see Header#getAccept()
     */
    default ContentTypes accept() {
        return header().getAccept();
    }

    /**
     * Returns the user agent based on the <code>"User-Agent"</code> header field, or
     * {@link UserAgent#UNKNOWN}.
     *
     * @return The user agent of this request
     * @see Header#getUserAgent()
     */
    default UserAgent userAgent() {
        return header().getUserAgent();
    }

    /**
     * Returns the login information based on the <code>"Authorization"</code> header field using
     * the Basic authentication scheme, or <code>null</code> if no such header exists, or it doesn't
     * confirm the Basic protocol.
     *
     * @return The basic authentication credentials of this request
     * @see Header#getBasicAuth()
     */
    default Login basicAuth() {
        return header().getBasicAuth();
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
     * Returns <code>body().asMultipart()</code> if this request has a body, otherwise <code>null</code>.
     *
     * @return The content of this request parsed as <code>multipart/formdata</code>, if any
     */
    default Body.Multipart multipart() {
        Body body = body();
        return body != null ? body.asMultipart() : null;
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
    default String toHttpWithoutBody() {
        StringBuilder str = new StringBuilder();
        str.append(method()).append(' ').append(url()).append(" HTTP/1.1\r\n");
        header().forEach((k,vs) -> vs.forEach(v -> str.append(k).append(": ").append(v).append("\r\n")));
        return str.toString();
    }

    /**
     * Serializes this http request into its string representation, including the body (which may consume
     * the body - use {@link Body#buffer()} if you want to further use the request body).
     *
     * @return A http-protocol conform string representing this http request
     */
    default String toHttp() {
        Body body = body();
        if(body == null)
            return toHttpWithoutBody();
        return toHttpWithoutBody() + "\n" + body.text();
    }

    @Override
    default Object toJson() {
        return new JsonObject(
                "method", method(),
                "url", url(),
                "header", header(),
                "body", body()
        );
    }

    /**
     * Represents an unsent http request which can be edited and sent.
     */
    interface Unsent extends HttpRequest, Cloneable<HttpRequest.Unsent> {

        int _init = init();
        private static int init() {
            Json.registerDeserializer(Unsent.class, json -> (Unsent) json.as(HttpRequest.class));
            return 0;
        }

        /**
         * Returns the root http client that was used to create this request.
         *
         * @return The http client used to create this request
         */
        HttpClient httpClient();

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
         * Returns the connection timeout after which an {@link ConnectTimeoutException}
         * will be thrown, if the connection to the server could not be established.
         *
         * @return The current connect timeout in milliseconds
         */
        long connectTimeout();

        /**
         * Sets the connection timeout after which an {@link ConnectTimeoutException}
         * will be thrown, if the connection to the server could not be established.
         *
         * @param timeout The timeout to set, in milliseconds
         * @return This request
         */
        Unsent setConnectTimeout(long timeout);

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
         * Shorthand for <code>header().setDate(date)</code>.
         *
         * @param date The date of the request. Usually populated automatically.
         * @return This request
         * @see Header#setDate(Instant)
         */
        default Unsent setDate(@Nullable Instant date) {
            header().setDate(date);
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
         * Shorthand for <code>header().setAccept(contentTypes)</code>.
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
         * Shorthand for <code>header().setUserAgent(userAgent)</code>.
         *
         * @param userAgent The user agent to set, or <code>null</code> to remove the header field
         * @return This request
         */
        default Unsent setUserAgent(@Nullable UserAgent userAgent) {
            header().setUserAgent(userAgent);
            return this;
        }

        /**
         * Shorthand for <code>header().setBasicAuth(username, password)</code>.
         *
         * @param username The username to set as basic auth header, may not contain colons
         * @param password The password to set as basic auth header (can contain any chars)
         * @return This request
         */
        default Unsent setBasicAuth(String username, String password) {
            header().setBasicAuth(username, password);
            return this;
        }

        /**
         * Shorthand for <code>header().setBasicAuth(credentials)</code>.
         *
         * @param credentials The credentials to set as basic auth header, username may not contain colons
         * @return This request
         */
        default Unsent setBasicAuth(Login credentials) {
            header().setBasicAuth(credentials);
            return this;
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
         * Shorthand for <code>setBody(Body.of(file))</code>. Also attempts to guess the content type based
         * on the file's name.
         *
         * @param file The file whose content to use as the body of this http request
         * @return This http request
         */
        default Unsent setFile(Path file) {
            return setFile(file, true);
        }

        /**
         * Shorthand for <code>setBody(Body.of(file))</code>.
         *
         * @param file The file whose content to use as the body of this http request
         * @param guessContentType Whether to attempt to guess the file's content type from its name
         *                         and, on success, override the Content-Type header field
         * @return This http request
         */
        default Unsent setFile(Path file, boolean guessContentType) {
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
         * @param file The file whose content to use as the body of this http request
         * @return This http request
         */
        default Unsent setFile(File file) {
            return setFile(file, true);
        }

        /**
         * Shorthand for <code>setBody(Body.of(file))</code>.
         *
         * @param file The file whose content to use as the body of this http request
         * @param guessContentType Whether to attempt to guess the file's content type from its name
         *                         and, on success, override the Content-Type header field
         * @return This http request
         */
        default Unsent setFile(File file, boolean guessContentType) {
            return setFile(Arguments.checkNull(file, "file").toPath(), guessContentType);
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
         * Shorthand for <code>setBody(Body.Multipart.of(formData))</code>, also sets the
         * content type to <code>multipart/form-data</code> with a randomly generated boundary.
         *
         * @param formData The data for the multipart body
         * @return This request
         * @see Body#of(FormData)
         */
        default Unsent setFormData(FormData formData) {
            return setMultipart(Body.of(formData));
        }

        /**
         * Modifies the request url by adding the given query parameter.
         *
         * @param name The name of the parameter
         * @param value The value of the parameter
         * @return This request
         * @see #addQuery(Map)
         */
        default Unsent addQuery(String name, String value) {
            return addQuery(Map.of(name, value));
        }

        /**
         * Modifies the request url by adding the given query parameter.
         * <p>Shorthand for <code>addQuery(name, value+"")</code>.</p>
         *
         * @param name The name of the parameter
         * @param value The value of the parameter
         * @return This request
         * @see #addQuery(Map)
         */
        default Unsent addQuery(String name, long value) {
            return addQuery(name, value+"");
        }

        /**
         * Modifies the request url by adding the given query parameter.
         * <p>This method converts a the number to a string without using scientific
         * notation as the standard toString() conversion may do.</p>
         *
         * @param name The name of the parameter
         * @param value The value of the parameter
         * @return This request
         * @see #addQuery(Map)
         */
        default Unsent addQuery(String name, double value) {
            return addQuery(name, HttpUtils.NO_SCIENTIFIC_FLOATS.format(value));
        }

        /**
         * Modifies the request url by adding the given query parameters.
         *
         * @param params The parameters to add
         * @return This request
         */
        default Unsent addQuery(Map<? extends String, ? extends String> params) {
            return setUrl(new URLBuilder(url()).addParams(params).toString());
        }

        /**
         * Modifies the request url by adding the given query parameters. The parameters must be serializable
         * to JSON and the serialized form must be convertible to a string map, which means it only contains
         * primitive type values (numbers and booleans are ok).
         *
         * @param paramsAsJson The parameters to add. Will be serialized to a string map equivalently to:
         *                     <pre>JsonElement.wrap(paramsAsJson, JsonDeserializer.STRING_CONVERSION).asMap(String.class)</pre>
         * @return This request
         */
        default Unsent addQuery(Object paramsAsJson) {
            return addQuery(JsonElement.wrap(paramsAsJson, JsonDeserializer.STRING_CONVERSION).asMap(String.class));
        }

        /**
         * Modifies the request url by setting the given query parameters.
         *
         * @param params The parameters to set (instead of any current query parameters)
         * @return This request
         */
        default Unsent setQuery(Map<? extends String, ? extends String> params) {
            return setUrl(new URLBuilder(url()).setParams(params).toString());
        }

        /**#
         * Modifies the request url by setting the given query parameters. The parameters must be serializable
         * to JSON and the serialized form must be convertible to a string map, which means it only contains
         * primitive type values (numbers and booleans are ok).
         *
         * @param paramsAsJson The parameters to set (instead of any current query parameters). Will be serialized
         *                     to a string map equivalently to:
         *                     <pre>JsonElement.wrap(paramsAsJson, JsonDeserializer.STRING_CONVERSION).asMap(String.class)</pre>
         * @return This request
         */
        default Unsent setQuery(Object paramsAsJson) {
            return setQuery(JsonElement.wrap(paramsAsJson, JsonDeserializer.STRING_CONVERSION).asMap(String.class));
        }

        /**
         * Sets the request body to the given url parameters. Also sets the content type
         * of the request to <code>application/x-www-form-urlencoded</code>.
         *
         * @param params The parameters to set as body
         * @return This request
         */
        default Unsent setParams(Map<? extends String, ? extends String> params) {
            return setParams(params, true);
        }

        /**
         * Sets the request body to the given url parameters.
         *
         * @param params The parameters to set (instead of any current query parameters)
         * @param setContentType Whether to set the content type to <code>application/x-www-form-urlencoded</code>
         * @return This request
         */
        default Unsent setParams(Map<? extends String, ? extends String> params, boolean setContentType) {
            return (setContentType ? setContentType(ContentType.URL_ENCODED) : this)
                    .setData(URLBuilder.queryString(params));
        }

        /**
         * Sets the request body to the given url parameters. The parameters must be serializable
         * to JSON and the serialized form must be convertible to a string map, which means it only contains
         * primitive type values (numbers and booleans are ok). This also sets the content type of the request
         * to <code>application/x-www-form-urlencoded</code>.
         *
         * @param paramsAsJson The parameters to set (instead of any current query parameters). Will be serialized
         *                     to a string map equivalently to:
         *                     <pre>JsonElement.wrap(paramsAsJson, JsonDeserializer.STRING_CONVERSION).asMap(String.class)</pre>
         * @return This request
         */
        default Unsent setParams(Object paramsAsJson) {
            return setParams(paramsAsJson, true);
        }

        /**
         * Sets the request body to the given url parameters. The parameters must be serializable
         * to JSON and the serialized form must be convertible to a string map, which means it only contains
         * primitive type values (numbers and booleans are ok).
         *
         * @param paramsAsJson The parameters to set (instead of any current query parameters). Will be serialized
         *                     to a string map equivalently to:
         *                     <pre>JsonElement.wrap(paramsAsJson, JsonDeserializer.STRING_CONVERSION).asMap(String.class)</pre>
         * @param setContentType Whether to set the content type to <code>application/x-www-form-urlencoded</code>
         * @return This request
         */
        default Unsent setParams(Object paramsAsJson, boolean setContentType) {
            return setParams(JsonElement.wrap(paramsAsJson, JsonDeserializer.STRING_CONVERSION).asMap(String.class), setContentType);
        }

        /**
         * Adds a preprocessor that will be executed on a received response. Instead of the received response,
         * the returned response will be returned (which may be the same). Any exceptions thrown will not be
         * caught. If multiple preprocessors are registered, they will be executed in the order they were
         * registered.
         *
         * @param preprocessor The preprocessor to execute on the received request
         * @return This request
         */
        Unsent addResponsePreprocessor(@NotNull Function<? super HttpResponse, ? extends HttpResponse> preprocessor);

        /**
         * Adds a preprocessor that will be executed on a received response. Any exceptions thrown will not
         * be caught. If multiple preprocessors are registered, they will be executed in the order they were
         * registered.
         *
         * @param preprocessor The preprocessor to execute on the received request
         * @return This request
         */
        default Unsent addResponsePreprocessor(@NotNull Consumer<? super HttpResponse> preprocessor) {
            Arguments.checkNull(preprocessor, "preprocessor");
            return addResponsePreprocessor(r -> { preprocessor.accept(r); return r; });
        }

        /**
         * Requires the response to be a successful code (2XX) and will, upon receiving the response, throw
         * an {@link UnexpectedResponseCodeException} if a different code is received. Calling this method
         * will replace any response code expectation previously registered using {@link #expectResponseCode(Predicate)}.
         *
         * @return This request
         */
        default Unsent expectSuccess() {
            return expectResponseCode(ResponseCode::ok);
        }

        /**
         * Requires the response to be exactly the given code and will, upon receiving the response, throw
         * an {@link UnexpectedResponseCodeException} if a different code is received. Calling this method
         * will replace any response code expectation previously registered using {@link #expectResponseCode(Predicate)}.
         *
         * @return This request
         */
        default Unsent expectResponseCode(@Nullable ResponseCode code) {
            if(code == null)
                return expectResponseCode((Predicate<? super ResponseCode>) null);
            return expectResponseCode(c -> c == code);
        }

        /**
         * Requires the response to match the given filter and will, upon receiving the response, throw
         * an {@link UnexpectedResponseCodeException} if a different code is received. Calling this method
         * will replace any response code expectation previously registered using {@link #expectSuccess()}
         * or {@link #expectResponseCode(Predicate)}. Passing <code>null</code> will remove any filter.
         *
         * @param filter The filter used to test received response codes, or <code>null</code>
         * @return This request
         */
        Unsent expectResponseCode(@Nullable Predicate<? super ResponseCode> filter);

        /**
         * Removes any response code filter previously set using one of the {@link #expectResponseCode(Predicate)}
         * or {@link #expectSuccess()} methods, if any. This is equivalent to passing <code>null</code> to one of
         * the {@link #expectResponseCode(Predicate)} methods (however, <code>null</code> cannot be distinguished
         * between a <code>ResponseCode</code> and a <code>Predicate</code> which makes calling those methods with
         * <code>null</code> inconvenient, thus this method.
         *
         * @return This request
         */
        default Unsent expectAnyResponseCode() {
            return expectResponseCode((Predicate<? super ResponseCode>) null);
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
         * if any, and close it.
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

        @Override
        default Object toJson() {
            JsonObject json = (JsonObject) HttpRequest.super.toJson();
            json.put("httpClient", httpClient());
            json.put("doRedirects", doRedirects());
            return json;
        }
    }

    /**
     * Represents a http request received by the server, which the server can configure a response for.
     */
    interface Received extends HttpRequest {

        /**
         * Returns the java server object that represents the http server that received the request, e.g.
         * an instance of {@link HttpServer} or {@link RawHttpServer}.
         *
         * @return The server context. May be null if there is no context for whatever reason, but can be expected
         *         to be non-null when using a {@link HttpServer} or {@link RawHttpServer}.
         */
        Object serverObject();

        /**
         * Returns the java server object that represents the http server that received the request cast
         * to the given type, e.g. an instance of {@link HttpServer} or {@link RawHttpServer}.
         *
         * @return The server context, cast to the given type. Never null (that would cause an exception).
         * @throws NullPointerException If no context is present at all
         * @throws ClassCastException If there is a server context present but not of the given type
         */
        @NotNull
        default <S> S serverObject(Class<S> type) {
            return type.cast(Objects.requireNonNull(serverObject(), "No server context present"));
        }

        /**
         * Returns the response to this http request, or <code>null</code> if not yet created.
         * The response may have already been sent.
         *
         * @return The http response instance, or <code>null</code>
         */
        @Nullable
        HttpResponse.Editable getResponse();

        /**
         * Returns an unsent, mutable http response responding to this http request. If this method
         * is called multiple times, the previously returned requests are invalidated, or the method
         * throws an exception if the previous request has already been sent.
         *
         * @param code The response code for the response. Can be changed later
         * @return A response to this request, not yet sent
         */
        HttpResponse.Editable respond(ResponseCode code);

        /**
         * Returns an unsent, mutable http response responding to this http request. If this method
         * is called multiple times, the previously returned requests are invalidated, or the method
         * throws an exception if the previous request has already been sent.
         *
         * @param code The response code for the response. Can be changed later
         * @return A response to this request, not yet sent
         */
        default HttpResponse.Editable respond(int code) {
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
        default HttpResponse.Editable respond() {
            return respond(ResponseCode.OK);
        }

        /**
         * Invalidates any previously configured response, if any. If the response has already been
         * sent, an exception will be thrown.
         *
         * @return This request
         */
        Received invalidateResponse();

        /**
         * Adds a configurator that will be executed before the response to this http request is
         * sent, unless the response has the response code 500. The configurators may be run at
         * any time, possibly multiple times (but only once per response instance) and on any thread.
         *
         * @param configurator The function to run on a non-internal-server-error response on this request
         * @return This request
         */
        HttpRequest.Received addResponseConfigurator(Consumer<? super HttpResponse.Editable> configurator);

        /**
         * Returns all response configurators added as a single consumer. If no configurator has
         * been added, the returned consumer does nothing.
         *
         * @return All configurators added
         */
        @NotNull
        Consumer<? super HttpResponse.Editable> getResponseConfigurators();

        /**
         * Removes all response configurators previously added using {@link #addResponseConfigurator(Consumer)}.
         *
         * @return This request
         */
        HttpRequest.Received clearResponseConfigurators();

        /**
         * Registers additional information about this request. If a parameter of this type already exists,
         * it will be overridden.
         *
         * @param type The type of extra information
         * @param value The extra information
         * @return This request
         */
        <T> HttpRequest.Received bindOptionalParam(Class<T> type, T value);

        /**
         * Returns the additional request information about this request, of the given type (the parameter
         * must have been registered with exactly that type, not a subtype). If no information of that type
         * was ever registered, an exception will be thrown.
         *
         * @param type The type of extra information to get
         * @return The extra information of that type. The information must have been registered, but may still
         *         be <code>null</code> if that is the value that was bound using {@link #bindOptionalParam(Class, Object)}.
         * @throws NoSuchElementException If no value was registered for the given type using {@link #bindOptionalParam(Class, Object)}.
         *                                If the value <code>null</code> was registered, no exception will be thrown.
         */
        <T> T getOptionalParam(Class<T> type) throws NoSuchElementException;

        /**
         * Returns whether additional request information of the given type has been registered (note that the
         * value may still be <code>null</code>).
         *
         * @param type The type to test whether extra information is present for
         * @return Whether extra information of the given type is present
         */
        boolean hasOptionalParam(Class<?> type);

        /**
         * Returns the additional request information of the given type, or evaluates the given supplier and
         * returns that value if no information was registered.
         *
         * @param type The type of extra information to get
         * @param ifAbsent The default value supplier if no value was registered
         * @return The extra information registered for that type, or the generated default value. Note
         *         that the returned value may still be <code>null</code> if null was explicitly registered
         *         using {@link #bindOptionalParam(Class, Object)}, or if the default value returned was null.
         */
        default <T> T getOptionalParam(Class<T> type, Supplier<? extends T> ifAbsent) {
            synchronized(this) {
                if(hasOptionalParam(type))
                    return getOptionalParam(type);
            }
            return ifAbsent.get();
        }
    }

    /**
     * Represents a http request received by the server, which the server can configure <i>and send</i>
     * a response for.
     */
    interface Respondable extends Received {
        @Override
        @Nullable HttpResponse.Sendable getResponse();

        @Override
        HttpResponse.Sendable respond(ResponseCode code);

        @Override
        default HttpResponse.Sendable respond(int code) {
            return (HttpResponse.Sendable) Received.super.respond(code);
        }

        @Override
        default HttpResponse.Sendable respond() {
            return (HttpResponse.Sendable) Received.super.respond();
        }

        @Override
        Respondable invalidateResponse();

        @Override
        Respondable addResponseConfigurator(Consumer<? super HttpResponse.Editable> configurator);

        @Override
        Respondable clearResponseConfigurators();

        @Override
        <T> Respondable bindOptionalParam(Class<T> type, T value);
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

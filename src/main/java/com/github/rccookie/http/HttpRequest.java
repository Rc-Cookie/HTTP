package com.github.rccookie.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.rccookie.json.Json;
import com.github.rccookie.util.Arguments;
import com.github.rccookie.xml.Node;
import com.github.rccookie.xml.XML;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An unsent HTTP request. HTTP requests can be safely reused.
 */
public class HttpRequest {

    /**
     * The url for the request.
     */
    final String url;
    /**
     * The request method.
     */
    Method method = Method.GET;
    /**
     * Custom header fields for the request.
     */
    final Map<String,String> header = new HashMap<>();
    /**
     * View of {@link #header}.
     */
    private final Map<String,String> headerView = Collections.unmodifiableMap(header);
    /**
     * Whether redirects are allowed.
     */
    boolean redirects = true;


    /**
     * Creates a new HTTP request for the specified url.
     *
     * @param url The url to send the request to
     */
    public HttpRequest(String url) {
        if(!Arguments.checkNull(url, "url").startsWith("http"))
            throw new IllegalArgumentException("HTTP request urls must start with http or https");
        this.url = url;
    }

    /**
     * Returns the HTTP request url.
     *
     * @return The url used
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns a view of the custom header fields in this HTTP request.
     *
     * @return The header fields
     */
    public Map<String, String> getHeader() {
        return headerView;
    }

    /**
     * Returns the request method, which is {@link Method#GET} by default.
     *
     * @return The request method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Returns whether automatic redirects are allowed.
     *
     * @return Whether redirects are allowed
     */
    public boolean isAllowRedirects() {
        return redirects;
    }

    /**
     * Sets the request method for the HTTP request. The default method is
     * {@link Method#GET}.
     *
     * @param method The method to use
     * @return This HTTP request
     */
    public HttpRequest setMethod(@NotNull Method method) {
        this.method = Arguments.checkNull(method, "method");
        return this;
    }

    /**
     * Sets the specified header field to the given value.
     *
     * @param key The header field to set
     * @param value The value of that header field
     * @return This HTTP request
     */
    public HttpRequest setHeaderField(@NotNull String key, @NotNull String value) {
        header.put(Arguments.checkNull(key, "key"), Arguments.checkNull(value, "value"));
        return this;
    }

    /**
     * Sets the {@code "Content-Type"} header field.
     *
     * @param contentType The content type of this HTTP request
     * @return This HTTP request
     */
    public HttpRequest setContentType(String contentType) {
        return setHeaderField("Content-Type", contentType);
    }

    /**
     * Sets the {@code "Cookie"} header field. Cookies must be seperated with {@code "; "}.
     *
     * @param cookies The cookies to use
     * @return This HTTP request
     */
    public HttpRequest setCookies(String cookies) {
        return setHeaderField("Cookie", cookies);
    }

    /**
     * Sets whether automatic redirects should be allowed. This is on by default.
     *
     * @param allow Whether redirects should be allowed
     * @return This HTTP request
     */
    public HttpRequest allowRedirects(boolean allow) {
        this.redirects = allow;
        return this;
    }


    /**
     * Sends this HTTP request.
     *
     * @param bytes The bytes to send.
     * @return The HTTP response
     */
    public HttpResponse send(byte @Nullable [] bytes) {
        return new HttpResponse(url, this, bytes);
    }

    /**
     * Sends this HTTP request.
     *
     * @param data The data to send.
     * @return The HTTP response
     */
    public HttpResponse send(@Nullable String data) {
        return send(data != null ? data.getBytes() : null);
    }

    /**
     * Sends this HTTP request without content.
     *
     * @return The HTTP response
     */
    public HttpResponse send() {
        return send((byte[]) null);
    }

    /**
     * Sends this HTTP request with the given data converted to json format.
     *
     * @param json The object to be sent as json. Must be convertable to json
     * @return The HTTP response
     */
    public HttpResponse sendJson(Object json) {
        return send(Json.toString(json));
    }

    /**
     * Sends this HTTP request with the given HTML as data. The content type
     * will also be set to text/html.
     *
     * @param html The HTML to send
     * @return The HTTP response
     */
    public HttpResponse sendHTML(Node html) {
        setContentType("text/html");
        return sendXML(html, XML.HTML);
    }

    /**
     * Sends this HTTP request with the given XML as data.
     *
     * @param xml The XML to send
     * @return The HTTP response
     */
    public HttpResponse sendXML(Node xml) {
        return sendXML(xml, XML.XML);
    }

    /**
     * Sends this HTTP request with the given XML as data.
     *
     * @param xml THe XML to send
     * @param options XML formatting options
     * @return The HTTP response
     */
    public HttpResponse sendXML(Node xml, long options) {
        return send(xml.toString(options));
    }

    /**
     * Sends this HTTP request with the given multipart form data.
     * The content type of the request itself will also be set to
     * multipart/form-data.
     *
     * @param data The data to send
     * @return The HTTP response
     */
    public HttpResponse sendMultipart(Multipart data) {
        setContentType("multipart/form-data; boundary=" + data.getBoundary());
        return send(data.getBytes());
    }

    /**
     * Sends this HTTP request with the given parameters appended on the url.
     * The url must not include any parameters before. This will also set the
     * content type to application/x-www-form-urlencoded.
     *
     * @param params Parameters to be included in the url of the request to send, converted to
     *               strings using their respective toString() method
     * @return A future to the HTTP response
     */
    public HttpResponse sendParams(Map<?,?> params) {
        setContentType("application/x-www-form-urlencoded");
        String paramsString = "?" + params.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("&"));
        return new HttpResponse(url + paramsString, this, null);
    }

    /**
     * Returns the request method and the url as string.
     *
     * @return A string representation of this object
     */
    @Override
    public String toString() {
        return method + " " + url;
    }

    /**
     * HTTP request methods.
     *
     * <p><a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods">Reference</a></p>
     */
    public enum Method {
        /**
         * The GET method requests a representation of the specified resource. Requests using GET should only retrieve data.
         */
        GET,
        /**
         * The HEAD method asks for a response identical to a GET request, but without the response body.
         */
        POST,
        /**
         * The PUT method replaces all current representations of the target resource with the request payload.
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
}

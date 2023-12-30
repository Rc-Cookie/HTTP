package de.rccookie.http.server;

import java.io.InputStream;
import java.net.InetSocketAddress;

import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.Path;
import de.rccookie.http.Query;
import de.rccookie.json.JsonElement;

/**
 * Classes implementing this interface can be passed as http handlers to
 * an http server. They can define (multiple) http handlers by defining
 * <code>void</code> methods which are annotated with one or multiple
 * {@link On} annotations, and optionally with {@link AsyncResponse}. Each method
 * can take the following parameter types (all optional and in any order):
 * <ul>
 *     <li>{@link HttpRequest} or {@link HttpRequest.Received} will be passed
 *     the incoming request directly.</li>
 *     <li>{@link Body} will receive the request's body.</li>
 *     <li>{@link Body.Multipart} will receive the request's body parsed as
 *     multipart, or <code>null</code> if the body already was <code>null</code>.</li>
 *     <li>{@link InputStream} will receive the bodies input stream, or <code>null</code>
 *     if the body was already <code>null</code>.</li>
 *     <li><code>byte[]</code> will receive the bodies byte data, or <code>null</code>
 *     if the body was already <code>null</code>.</li>
 *     <li>{@link String} will receive the bodies text, or <code>null</code>
 *     if the body was already <code>null</code>.</li>
 *     <li>{@link JsonElement} will receive the body parsed as json, or <code>null</code>
 *     if the body was already <code>null</code>.</li>
 *     <li>{@link Header} will receive the request's header.</li>
 *     <li>{@link Query} will receive the request's query parameters.</li>
 *     <li>{@link HttpRequest.Method} will receive the request method.</li>
 *     <li>{@link Path} will receive the request's path.</li>
 *     <li>{@link InetSocketAddress} will receive the client's ip address and port.</li>
 *     <li>Any other type must be annotated with {@link Parse} to specify how it should
 *     be parsed from the request.</li>
 * </ul>
 * The handler method may throw any type of exception, which will all be caught. If
 * an exception of type {@link HttpRequestFailure} is thrown, a suitable response will
 * be generated with the specified response code. For any other exception, a
 * <code>500 INTERNAL SERVER ERROR</code> will be returned to the client.
 */
public interface HttpRequestListener {
}

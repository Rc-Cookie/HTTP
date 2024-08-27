package de.rccookie.http.server;

import java.io.InputStream;
import java.net.InetSocketAddress;

import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.Method;
import de.rccookie.http.Query;
import de.rccookie.http.Route;
import de.rccookie.http.server.annotation.Parse;
import de.rccookie.http.server.annotation.Response;
import de.rccookie.http.server.annotation.methods.GET;
import de.rccookie.http.server.annotation.methods.POST;
import de.rccookie.json.JsonElement;

/**
 * Classes implementing this interface can be passed as http handlers to
 * an http server. They can define (multiple) http handlers by defining
 * methods which are annotated with a {@link Route} and/or a method annotation
 * (@{@link GET}, {@link POST} etc.). If no route is specified, the method name
 * will be used. If no method is specified, it will be {@link GET}. Multiple
 * methods can be specified. If neither route nor method is present, the method
 * will be ignored. Each method can take the following parameter types (all
 * optional and in any order):
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
 *     <li>{@link Method} will receive the request method.</li>
 *     <li>{@link Route} will receive the request's path.</li>
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

    /**
     * Http listeners returned by this method will automatically also be registered when
     * registering this http request listener, with the route prefix of this listener
     * as prefix for all routes of the sub listeners.
     *
     * @return The listeners for sub routes that should be registered with this listener
     */
    default HttpRequestListener[] subRoutes() {
        return new HttpRequestListener[0];
    }

    /**
     * Http processors returned by this method will be added to the http processors
     * for all listener methods of this listener. Processors returned by this method
     * will be executed after any processors added using annotations.
     *
     * @return The http processors to use for all listener methods in this class
     */
    default HttpProcessor[] extraProcessors() {
        return new HttpProcessor[0];
    }

    /**
     * Returns the current http request, which is the request that is currently being
     * handled by a specific route of this listener. Only valid when called from the thread
     * on which the listener method is being called, while the method is being called.
     *
     * @return The current http request
     */
    default HttpRequest.Received request() {
        return CurrentHttpServerContext.request();
    }

    /**
     * Returns a http response to the current http request, which is the request that is
     * currently being handled by a specific route of this listener. Unlike calling
     * <code>request.respond()</code>, this method does not create a new response every
     * time and does not invalidate the old one. If no request has been created yet, the
     * response gets initialized with the response code <code>200 OK</code> which can later
     * be changed. If, at a later point, one of the {@link HttpRequest.Received#respond()}
     * methods gets called again, the old request and any modifications made to it will
     * be lost and this method will return that new response. Any modification made to the
     * "final" response will be kept and not be overridden by an option of the {@link Response}
     * annotation.
     *
     * <p>This method behaves exactly like</p>
     * <pre>
     * return request().getResponse() != null ? request.getResponse() : request().respond();
     * </pre>
     *
     * @return The response associated with the current http request
     */
    default HttpResponse.Editable response() {
        HttpRequest.Received request = request();
        HttpResponse.Editable response = request.getResponse();
        return response != null ? response : request.respond();
    }
}

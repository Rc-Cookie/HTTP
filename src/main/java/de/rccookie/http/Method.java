package de.rccookie.http;

import de.rccookie.http.client.DefaultHostHttpClient;
import de.rccookie.http.client.HttpClient;
import de.rccookie.http.client.SimpleHttpClient;

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
     * The POST method submits an entity to the specified resource, often causing a change in state or side effects on the server.
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
    PATCH,

    // --- WEBDAV ---

    /**
     * Copy a resource from one uniform resource identifier (URI) to another.
     */
    COPY,
    /**
     * Put a lock on a resource. WebDAV supports both shared and exclusive locks.
     */
    LOCK,
    /**
     * Create collections (also known as a directory).
     */
    MKCOL,
    /**
     * Move a resource from one URI to another.
     */
    MOVE,
    /**
     * Retrieve properties, stored as XML, from a web resource. It is also overloaded
     * to allow one to retrieve the collection structure (also known as directory
     * hierarchy) of a remote system.
     */
    PROPFIND,
    /**
     * Change and delete multiple properties on a resource in a single atomic act.
     */
    PROPPATCH,
    /**
     * Remove a lock from a resource.
     */
    UNLOCK;


    public static void main(String[] args) {
        HttpClient webdav = new SimpleHttpClient(new DefaultHostHttpClient(null, "https://cloud.vintageshop.rccookie.de/remote.php/dav/files"), r -> r.setBasicAuth("admin", "XYxmSO1QTf2tWT"));
        System.out.println(webdav.get("/admin").setMethod(PROPFIND).send().toHttp());
    }
}

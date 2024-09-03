package de.rccookie.http;

/**
 * HTTP request methods.
 *
 * <p><a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods">Reference</a></p>
 */
public enum Method {
    /**
     * The GET method requests a representation of the specified resource. Requests using GET should only retrieve data.
     */
    GET(false),
    /**
     * The POST method submits an entity to the specified resource, often causing a change in state or side effects on the server.
     */
    POST(false),
    /**
     * The PUT method replaces all current representations of the target resource with the request payload.
     */
    PUT(false),
    /**
     * The HEAD method asks for a response identical to a GET request, but without the response body.
     */
    HEAD(false),
    /**
     * The DELETE method deletes the specified resource.
     */
    DELETE(false),
    /**
     * The CONNECT method establishes a tunnel to the server identified by the target resource.
     */
    CONNECT(false),
    /**
     * The OPTIONS method describes the communication options for the target resource.
     */
    OPTIONS(false),
    /**
     * The TRACE method performs a message loop-back test along the path to the target resource.
     */
    TRACE(false),
    /**
     * The PATCH method applies partial modifications to a resource.
     */
    PATCH(false),

    // --- WEBDAV ---

    /**
     * Copy a resource from one uniform resource identifier (URI) to another.
     */
    COPY(true),
    /**
     * Put a lock on a resource. WebDAV supports both shared and exclusive locks.
     */
    LOCK(true),
    /**
     * Create collections (also known as a directory).
     */
    MKCOL(true),
    /**
     * Move a resource from one URI to another.
     */
    MOVE(true),
    /**
     * Retrieve properties, stored as XML, from a web resource. It is also overloaded
     * to allow one to retrieve the collection structure (also known as directory
     * hierarchy) of a remote system.
     */
    PROPFIND(true),
    /**
     * Change and delete multiple properties on a resource in a single atomic act.
     */
    PROPPATCH(true),
    /**
     * Remove a lock from a resource.
     */
    UNLOCK(true);

    public final boolean isWebDAV;

    Method(boolean isWebDAV) {
        this.isWebDAV = isWebDAV;
    }
}

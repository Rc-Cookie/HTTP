package de.rccookie.http.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.HttpRequest;

/**
 * A method of an {@link HttpRequestListener} annotated with this annotation
 * one or multiple times specifies the annotated method as http request handler,
 * for a given path pattern and specified request methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(On.Multiple.class)
@SuppressWarnings("NewClassNamingConvention")
public @interface On {

    /**
     * The request path this handler handles. This path must start with <code>"/"</code>,
     * and can contain wildcards:
     * <ul>
     *     <li>A single wildcard matches exactly one directory with any name, for example
     *     <code>"/a/*"</code> matches <code>/a/b</code>, but not <code>/a/b/c</code>.</li>
     *     <li>A double wildcard matches zero or more directories with any name, for example
     *     <code>"/a/**&#47;c"</code> matches <code>/a/c</code>, <code>/a/b/c</code> or
     *     <code>/a/b1/b2/c</code>, but not <code>/a/b/d</code>.</li>
     * </ul>
     * If multiple handlers are match the same path, a handler with a concrete path will
     * always be chosen over a handler with a path pattern. If multiple patterns match,
     * an arbitrary handler may be chosen.
     */
    String path();

    /**
     * The request method this handler handles, at least one must be specified.
     */
    HttpRequest.Method[] method();

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Multiple {
        On[] value();
    }
}

package de.rccookie.http.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The request route this handler handles. This route must start with <code>"/"</code>,
 * and can contain wildcards:
 * <ul>
 *     <li>A single wildcard matches exactly one directory with any name, for example
 *     <code>"/a/*"</code> matches <code>/a/b</code>, but not <code>/a/b/c</code>.</li>
 *     <li>A double wildcard matches zero or more directories with any name, for example
 *     <code>"/a/**&#47;c"</code> matches <code>/a/c</code>, <code>/a/b/c</code> or
 *     <code>/a/b1/b2/c</code>, but not <code>/a/b/d</code>.</li>
 * </ul>
 * Additionally, the path can contain <i>path variables</i> which can be obtained later
 * using the {@link PathVar} annotation. Path variables start with '<code>&le;</code>',
 * followed by their name and finally a closing '<code>></code>'. The name contain none of
 * a forward slash '<code>/</code>', a less than symbol '<code>&le;</code> nor a color
 * '<code>:</code>'. Additionally, path variables can define a regular expression which
 * controls which paths they match. To define such a pattern, the name should be followed
 * by a colon '<code>:</code>' followed by the regular expression. Inside the pattern, the
 * special characters mentioned above have to be escaped with a backslash. For example, the
 * path variable <code>&le;number:\d+></code> would match any route that contains a number
 * in that place. If no regular expression is specified, a path variable is semantically
 * identical to a single wildcard as defined above.
 *
 * <p>If multiple handlers are match the same route, a handler with a concrete route will
 * always be chosen over a handler with a route pattern. If multiple patterns match,
 * an arbitrary handler may be chosen.</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Route {

    /**
     * The request route this handler handles. This route must start with <code>"/"</code>,
     * and can contain wildcards:
     * <ul>
     *     <li>A single wildcard matches exactly one directory with any name, for example
     *     <code>"/a/*"</code> matches <code>/a/b</code>, but not <code>/a/b/c</code>.</li>
     *     <li>A double wildcard matches zero or more directories with any name, for example
     *     <code>"/a/**&#47;c"</code> matches <code>/a/c</code>, <code>/a/b/c</code> or
     *     <code>/a/b1/b2/c</code>, but not <code>/a/b/d</code>.</li>
     * </ul>
     * Additionally, the path can contain <i>path variables</i> which can be obtained later
     * using the {@link PathVar} annotation. Path variables start with '<code>&le;</code>',
     * followed by their name and finally a closing '<code>></code>'. The name contain none of
     * a forward slash '<code>/</code>', a less than symbol '<code>&le;</code> nor a color
     * '<code>:</code>'. Additionally, path variables can define a regular expression which
     * controls which paths they match. To define such a pattern, the name should be followed
     * by a colon '<code>:</code>' followed by the regular expression. Inside the pattern, the
     * special characters mentioned above have to be escaped with a backslash. For example, the
     * path variable <code>&le;number:\d+></code> would match any route that contains a number
     * in that place. If no regular expression is specified, a path variable is semantically
     * identical to a single wildcard as defined above.
     *
     * <p>If multiple handlers are match the same route, a handler with a concrete route will
     * always be chosen over a handler with a route pattern. If multiple patterns match,
     * an arbitrary handler may be chosen.</p>
     */
    String value();
}

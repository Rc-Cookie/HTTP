package de.rccookie.http.server.annotation.methods;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.Method;

/**
 * Specifies that this route accepts {@link Method#HEAD HEAD} requests.
 * Any route that accepts GET requests automatically also accepts HEAD
 * requests. If a route explicitly accepts the HEAD request method (by
 * being annotated with this annotation) it will be used instead of the
 * GET method to generate the HEAD response.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HEAD { }

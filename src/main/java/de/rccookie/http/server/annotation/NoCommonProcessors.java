package de.rccookie.http.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.Method;
import de.rccookie.http.server.HttpRequestHandler;
import de.rccookie.http.server.HttpServer;

/**
 * The equivalent to calling {@link HttpServer#addHandler(String, HttpRequestHandler, boolean, Method...)}
 * with <code>useCommonProcessors = false</code>.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoCommonProcessors {
}

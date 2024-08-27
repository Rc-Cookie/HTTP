package de.rccookie.http.server.annotation.methods;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.Method;

/**
 * Specifies that this route accepts {@link Method#GET GET} requests.
 * If a method isn't annotated with any specific method, it will be handling
 * GET requests by default.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GET { }

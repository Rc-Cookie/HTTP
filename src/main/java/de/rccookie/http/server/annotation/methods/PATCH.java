package de.rccookie.http.server.annotation.methods;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.Method;

/**
 * Specifies that this route accepts {@link Method#PATCH PATCH} requests.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PATCH { }

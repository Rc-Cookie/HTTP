package de.rccookie.http.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.Query;
import de.rccookie.http.Route;
import de.rccookie.http.server.Parser;

/**
 * The parser types which may be used for trying to parse the contents of the request.
 * If a class is annotated with this annotation, the specific parsers will be used for
 * all method parameters which aren't annotated themselves (except standard parameter
 * types such as {@link HttpRequest}, {@link Query} or {@link Route}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.TYPE})
public @interface Parse {

    /**
     * The parser types which may be used for trying to parse the contents of the request.
     * If a class is annotated with this annotation, the specific parsers will be used for
     * all method parameters which aren't annotated themselves (except standard parameter
     * types such as {@link HttpRequest}, {@link Query} or {@link Route}).
     */
    Class<? extends Parser>[] value();
}

package de.rccookie.http.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that this parameter should get populated by a "path variable", a placeholder
 * in the route string. For example, the route <code>/api/&le;id></code> contains the path
 * variable <code>id</code> which gets assigned the actual value of that path part for a
 * given request, for example <code>42</code> for a request on the route <code>/api/42</code>.
 *
 * <p>The string value of the path variable will get parsed to the parameter type using
 * the json deserializer for it. Note that the value gets interpreted as one string literal,
 * not as a json formatted file content. As such, any type can be parsed which supports json
 * deserialization directly from a string, exactly like <code>JsonElement.wrap(pathVar).as(paramType)</code>.</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathVar {

    /**
     * The name of the path variable, as declared in the route of the listener.
     */
    String value();

    /**
     * If this is true (which is the case by default) the server will return 404 Not Found
     * if it encounters a path variable value which fails to be parsed into the parameter type.
     * Otherwise, a 400 Bad Request will be returned with information about the failed parsing.
     */
    boolean reportErrorAsNotFound() default true;
}

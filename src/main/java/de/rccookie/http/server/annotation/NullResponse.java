package de.rccookie.http.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.ResponseCode;
import org.intellij.lang.annotations.Language;

/**
 * Defines that an error response should be sent if the annotated method returns <code>null</code>.
 * If the type is annotated, this will cause every method in the class to be handled that way.
 *
 * <p>This annotation may only be used on non-void methods. If the class is annotated with the
 * annotation and contains void handler methods, the annotation will have no effect on these
 * methods.</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NullResponse {

    /**
     * The (usually error) code to send if the method returns <code>null</code>.
     */
    ResponseCode code();

    /**
     * The (usually error) message to send along with the response code. If not specified,
     * a standard error message may be used for specific error response codes.
     */
    String message() default "";

    /**
     * The content type for the response. This is only relevant if the response code is not
     * an error code, otherwise the message will be embedded in a generated error response.
     */
    @Language("mime-type-reference")
    String contentType() default "";
}

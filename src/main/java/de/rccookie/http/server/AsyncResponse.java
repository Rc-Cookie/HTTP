package de.rccookie.http.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods of an {@link HttpRequestListener} annotated with {@link On} and this annotation
 * specify that they themselves ensure to send the response to a request, unlike synchronous
 * listeners for which the response will be sent automatically after they return, if not
 * already done. This can be useful if the response is handled by a different thread and this
 * thread returns before the response is ready to be sent.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncResponse {
}

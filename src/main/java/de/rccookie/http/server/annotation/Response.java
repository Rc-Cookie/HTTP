package de.rccookie.http.server.annotation;

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.ResponseCode;
import de.rccookie.http.server.HttpRequestListener;
import de.rccookie.http.server.Serializer;
import de.rccookie.xml.Document;
import de.rccookie.xml.Node;
import org.intellij.lang.annotations.Language;

/**
 * This annotation adjusts how the return value of a {@link HttpRequestListener}'s
 * handler method gets converted to a http response. Not annotating a method with this
 * annotation has the same effect as annotating it with <code>@Response</code> (without
 * setting any fields); every option will use its default value.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Response {

    /**
     * The response code to send with a successful response. If the response code is
     * <code>200 OK</code> (the default value) and the annotated method is of type
     * <code>void</code>, the response code will automatically be changed to
     * <code>204 NO CONTENT</code>.
     */
    ResponseCode code() default ResponseCode.OK;

    /**
     * The serializer type used to serialize the return value of the method into a http response
     * body. The default serializer serializes {@link Document Documents} and {@link Node Nodes}
     * into html, xHtml and xml, <code>byte[]</code> or {@link InputStream}s raw as binary, and
     * anything else into json.
     *
     * <p>When using a custom serializer, make sure the serializer class has a parameterless
     * constructor.</p>
     */
    Class<? extends Serializer<?>> serializer() default Serializer.Default.class;

    /**
     * Sets the content type header value of the response, e.g. <code>application/json</code>.
     * By default, the content type is determined by the serializer used. This option allows
     * to override that content type.
     */
    @Language("mime-type-reference")
    String contentType() default "";
}

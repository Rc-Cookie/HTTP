package de.rccookie.http.server.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.server.annotation.HttpProcessorType;
import de.rccookie.json.JsonStructure;

/**
 * Adds the option for the client to specify a filter for a json response.
 * Instead of transferring all json data, the client can specify a filter
 * to select only specific fields of objects. The filter works according to
 * {@link JsonStructure#filter(Object, boolean)} and can be specified either
 * as query parameter, or as header field.
 */
@HttpProcessorType(JsonFilterProcessor.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface JsonFilter {

    /**
     * The name of the query or header field (depending on {@link #fromHeader()})
     * which contains the filter. The default query parameter name is <code>"filter"</code>,
     * the default header field is <code>"X-Filter"</code>.
     */
    String fieldName() default "";

    /**
     * If set to true, the filter will be read from a header field rather than
     * from a query parameter.
     */
    boolean fromHeader() default false;

    /**
     * Whether to send an error response if the client filter specifies a
     * field which the response data does not contain. By default, that field
     * will just be ignored.
     */
    boolean errorOnMissing() default false;

    /**
     * If set to true, no warnings will be printed if this processor is used on an incompatible route.
     * Useful if this should be used on all routes, but some don't support it.
     */
    boolean quiet() default false;
}

package de.rccookie.http.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.ResponseCode;
import de.rccookie.http.server.Parser;

/**
 * Specifies that this parameter should get populated with the value of a query
 * parameter of a given request.
 *
 * <p>By default, the string value of the query parameter will get parsed to the parameter type using
 * the json deserializer for it. Note that the value gets interpreted as one string literal,
 * not as a json formatted file content. As such, any type can be parsed which supports json
 * deserialization directly from a string, exactly like <code>JsonElement.wrap(pathVar).as(paramType)</code>.</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryParam {

    String NULL = "<not specified$%&?>";

    /**
     * The name of the query parameter to assign to the annotated parameter.
     */
    String value();

    /**
     * Specifies how the query parameter content should be parsed into the
     * target type. The default interprets the content as the content of a
     * json string literal (not as json formatted text).
     *
     * @see #json()
     */
    Class<? extends Parser> parser() default Parser.JsonString.class;

    /**
     * Specifies that the query parameter content should be interpreted as json,
     * rather than as a string literal inside a json file. In other words, parsing
     * will use <code>Json.parse(queryParam).as(paramType)</code> instead of
     * <code>JsonElement.wrap(queryParam).as(paramType)</code>. Note that if this
     * is set to true and one wants to send a literal string, they will have to
     * enquote it first. Thus, this is disabled by default.
     *
     * <p>This is short for <code>parser = Parser.Json.class</code>. If this is
     * specified, no other parser may be specified.</p>
     */
    boolean json() default false;

    /**
     * Whether this parameter is required to be present in the request query.
     * If it is required and not present in a request, an error response will
     * be sent and the actual method will not be executed. The error message can
     * be adjusted with {@link #errorCode()} and {@link #errorMsg()}.
     * <p>If the parameter is not required and not present, it will be assigned
     * the value <code>null</code>. If {@link #defaultVal()} is specified, this
     * option will be ignored and treated as if it was set to <code>false</code>.</p>
     */
    boolean required() default true;

    /**
     * Specifies a default value to use if the parameter is not present in the
     * request query. In that case this string will be parsed instead of the
     * parameter using the same parser as would be used for the query parameter,
     * e.g. using <code>Json.wrap(defaultVal()).as(paramType)</code> if no other
     * parser has been specified.
     * <p>If this option is specified, {@link #required()} will be ignored. To
     * achieve a default value of <code>null</code>, use <code>required = false</code>
     * instead.</p>
     */
    String defaultVal() default NULL;

    /**
     * The error response code to send if the parameter was required but not found
     * in the request query.
     */
    ResponseCode errorCode() default ResponseCode.BAD_REQUEST;

    /**
     * An additional error message to include in an error response if the parameter
     * is required but wasn't found in a given request query. The response always,
     * independent of the value of this option, includes information about the fact
     * that the query parameter with the specified name was missing.
     */
    String errorMsg() default "";
}

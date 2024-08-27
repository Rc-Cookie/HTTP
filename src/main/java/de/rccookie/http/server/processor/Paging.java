package de.rccookie.http.server.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.rccookie.http.server.annotation.HttpProcessorType;

/**
 * Adds the possibility to only request a portion of the data, using two query (or header)
 * parameters <code>"offset"</code> and <code>"limit"</code> (names can be adjusted).
 * Specifying this will skip the first <code>offset</code> response entries and, starting
 * from there, return the next <code>limit</code> entries or less, if insufficient data is
 * available.
 *
 * <p>The regular procedure is to first compute the whole result, and then discard results
 * outside of the specified range to reduce payload length. If computation cost should be
 * reduces, the {@link #parseOnly()} flag can be set. In that case, the specified range will
 * be parsed into an {@link Pagination.Range} object, which can be retrieved as optional
 * parameter from the request (by simply adding a parameter of type {@link Pagination.Range}
 * to the handler method). The result will not be modified and no header field will be set,
 * the handler is now responsible for only returning the range.</p>
 *
 * <p><b>Paging is only supported for JSON responses returning objects or arrays, unless the
 * {@link #parseOnly()} flag is set.</b></p>
 */
@HttpProcessorType(Pagination.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Paging {

    /**
     * If the limit parameter is not given in the query, this will be used as default limit.
     * By default, the whole response will be returned then (possibly starting from an offset).
     */
    long defaultLimit() default 0;

    /**
     * The most entries that are allowed in one response. If the request specifies a higher
     * limit, an error response will be returned. If
     */
    long maxLimit() default Integer.MAX_VALUE;

    /**
     * The name of the query parameter (or header field, if used) to specify the limit. If the
     * default value is used and {@link #fromHeader()} is set, this will default to <code>"X-Limit"</code>.
     */
    String limitName() default "limit";

    /**
     * The name of the query parameter (or header field, if used) to specify the offset. If the
     * default value is used and {@link #fromHeader()} is set, this will default to <code>"X-Offset"</code>.
     */
    String offsetName() default "offset";

    /**
     * The name of the header field into which to write the total number of available entries,
     * before having applied the range. Specify an empty string to omit the header field.
     */
    String totalHeaderName() default "X-Total-Count";

    /**
     * If set to true, the limit and offset values will be parsed from header fields, rather than
     * from query parameters. If not otherwise specified, the field names will be changed to
     * <code>"X-Offset"</code> and <code>"X-Limit"</code>.
     */
    boolean fromHeader() default false;

    /**
     * If set to true, the range will only be parsed into an optional parameter of type {@link Pagination.Range},
     * but the result will not be truncated. This can be used if the computation overhead for computing all
     * results and discarding some should be avoided. The implementation of the pagination should then be done
     * by the handler itself.
     */
    boolean parseOnly() default false;

    /**
     * If set to true, no warnings will be printed if this processor is used on an incompatible route.
     * Useful if this should be used on all routes, but some don't support it.
     */
    boolean quiet() default false;
}

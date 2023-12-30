package de.rccookie.http;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import de.rccookie.json.Json;
import de.rccookie.json.JsonSerializable;
import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an http cookie.
 */
public interface Cookie extends JsonSerializable {

    /**
     * Date formatter to format valid time stamps.
     */
    DateTimeFormatter DATE_FORMATTER = getDateFormatterAndInitJson();
    private static DateTimeFormatter getDateFormatterAndInitJson() {
        Json.registerDeserializer(Cookie.class, json -> Cookie.parseCookie(json.asString()));
        return DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT"));
    }

    /**
     * Converts this cookie to a valid cookie string with all parameters, as found in the
     * <code>Set-Cookie</code> http response header field. If you only need the name-value
     * pairs, use {@link #toString(Collection)} instead.
     *
     * @param context The path context this cookie will be used in. Determines whether the path parameter can be left out (if it is identical)
     * @return This cookie as a string
     */
    String toString(Path context);

    /**
     * Converts this cookie to a valid cookie string with all parameters, as found in the
     * <code>Set-Cookie</code> http response header field. If you only need the name-value
     * pairs, use {@link #toString(Collection)} instead. The context path will be the root
     * path "/".
     *
     * @return This cookie as a string
     */
    String toString();

    @Override
    default Object toJson() {
        return toString();
    }

    /**
     * Returns whether this cookie is equal to the given object, that is, the given object
     * is also an instance of {@link Cookie} and the name and value fields are equal. The
     * parameters are ignored.
     *
     * @param obj The object to compare for equality
     * @return Whether the given object is a cookie equal to this cookie
     */
    boolean equals(Object obj);

    /**
     * The name of the cookie.
     */
    @NotNull
    String name();

    /**
     * The value of the cookie.
     */
    @NotNull
    String value();

    /**
     * The domain this cookie belongs to, if any.
     */
    @Nullable
    String domain();

    /**
     * The timestamp at which the cookie expires, if any. If the {@link #maxAge()} parameter is
     * present, the original value for <code>expires</code> (if any) will be ignored and calling
     * this method will simply return the expires time computed from the maxAge parameter since
     * cookie creation.
     */
    @Nullable
    Instant expires();

    /**
     * Whether this cookie has the <code>httponly</code> flag set, that is,
     * it is inaccessible from JavaScript client code.
     */
    boolean httpOnly();

    /**
     * The maximum age in seconds for this cookie to be valid, if any. If both this and
     * {@link #expires()} is set, the <code>expires</code> value is ignored. If only the
     * <code>expires</code> value was present, this method will compute the resulting max
     * age, which might be negative.
     */
    @Nullable
    default Long maxAge() {
        Instant expires = expires();
        if(expires == null) return null;
        return ChronoUnit.SECONDS.between(Instant.now(), expires);
    }

    /**
     * Whether this cookie has an independent partitioned state (CHIPS), that is, embedded
     * sites on different top level sites cannot be accessed from the same embedded site
     * embedded in a different top level site.
     */
    boolean partitioned();

    /**
     * The path start this cookie belongs to. If not specified, this will be <code>"/"</code>.
     */
    @NotNull
    Path path();

    /**
     * Whether this cookie has the <code>secure</code> flag set, that is, it will only be
     * sent over <code>https</code> requests and never over unsecure <code>http</code> requests.
     */
    boolean secure();

    /**
     * The same site mode for this cookie, if any, which defines when the cookie is sent on
     * cross-site requests.
     */
    @NotNull
    SameSite sameSite();

    /**
     * Different options for the <code>SameSite</code> cookie parameter.
     */
    enum SameSite {
        /**
         * The cookie is only sent with requests originating from the cookie's origin site.
         */
        STRICT,
        /**
         * The cookie is sent with requests originating or targeting the cookie's origin site.
         * If no value is set explicitly, this is the default value.
         */
        LAX,
        /**
         * The cookie is sent with requests originating or targeting the cookie's origin site,
         * but only if the request is secure (over https).
         */
        NONE
    }

    /**
     * Returns a cookie with the given name and value, with all other parameters set to their default.
     *
     * @param name The name of the cookie
     * @param value The value of the cookie
     * @return The cookie with that name and value
     */
    static Cookie of(String name, String value) {
        return new ShallowCookie(name, value);
    }

    /**
     * Returns a new cookie builder with the given name and value (which can subsequently be changed).
     *
     * @param name The name of the cookie
     * @param value The value of the cookie
     * @return A cookie builder with the given name and value set
     */
    static Builder create(String name, String value) {
        return new CookieBuilder(name, value);
    }

    /**
     * Parsers the given cookie string to a cookie, with all parameters.
     *
     * @param cookie The cookie string, which contains the definition for exactly one cookie, as found
     *               in the <code>Set-Cookie</code> http response header field.
     * @return The parsed cookie
     * @throws RuntimeException If the cookie string cannot be parsed
     */
    static Cookie parseCookie(String cookie) {
        String[] parts = cookie.split("; ");
        String[] pair = getKeyValue(parts[0]);
        if(parts.length == 1) return of(pair[0], pair[1]);
        Builder b = create(pair[0], pair[1]);
        for(int i=1; i<parts.length; i++) {
            String p = parts[i];
            if(p.indexOf('=') == -1) {
                switch(p.toLowerCase()) {
                    case "httponly": b = b.httpOnly(true); break;
                    case "partitioned": b = b.partitioned(true); break;
                    case "secure": b = b.secure(true); break;
                    default: throw new IllegalArgumentException("Unknown cookie flag: " + p);
                }
            }
            else {
                pair = getKeyValue(p);
                switch(pair[0].toLowerCase()) {
                    case "domain": b = b.domain(pair[1]); break;
                    case "expires": b = b.expires(Instant.from(DATE_FORMATTER.parse(pair[1]))); break;
                    case "max-age": b = b.maxAge(Long.parseLong(pair[1])); break;
                    case "path": b = b.path(pair[1]); break;
                    case "samesite": b = b.sameSite(SameSite.valueOf(pair[1].toUpperCase())); break;
                    default: throw new IllegalArgumentException("Unknown cookie attribute: " + p);
                }
            }
        }
        return b.build();
    }

    /**
     * Parses all cookies from the given string. The cookies <b>may not</b> contain any parameters.
     *
     * @param cookies The cookies string, as found in the <code>Cookie</code> http request header field
     * @return The parsed cookies, mapped to their names
     */
    static Map<String, Cookie> parseCookies(String cookies) {
        Map<String, Cookie> c = new LinkedHashMap<>();
        for(String cookie : cookies.split("; ")) {
            String[] pair = getKeyValue(cookie);
            c.put(pair[0], of(pair[0], pair[1]));
        }
        return c;
    }

    private static String[] getKeyValue(String keyValuePair) {
        int index = keyValuePair.indexOf('=');
        return new String[] { keyValuePair.substring(0, index), keyValuePair.substring(index+1) };
    }

    /**
     * Converts all given cookies into a string representation, <b>without</b> their parameter
     * values, as found in the <code>Cookie</code> http request header field. To get the full
     * string, use the {@link Cookie#toString()} method.
     *
     * @param cookies The cookies to convert to a string
     * @return The cookies as a single string, without their parameters
     */
    static String toString(Collection<? extends Cookie> cookies) {
        if(Arguments.checkNull(cookies, "cookies").isEmpty()) return "";
        StringBuilder str = new StringBuilder();
        for(Cookie c : cookies)
            str.append(c.name()).append("=").append(c.value()).append("; ");
        return str.substring(0, str.length()-2);
    }

    /**
     * A builder class to build {@link Cookie}s.
     */
    interface Builder extends Cookie {

        /**
         * Sets the name of the cookie.
         *
         * @param name The name to set
         * @return The builder
         */
        @NotNull
        Cookie.Builder name(@NotNull String name);

        /**
         * Sets the value of the cookie.
         *
         * @param value The value to set
         * @return The builder
         */
        @NotNull
        Cookie.Builder value(@NotNull String value);

        /**
         * Sets the domain of the cookie.
         *
         * @param domain The domain to set
         * @return The builder
         */
        @NotNull
        Cookie.Builder domain(String domain);

        /**
         * Sets the expiration date of the cookie.
         *
         * @param expires The expiration date to set, if any
         * @return The builder
         */
        @NotNull
        Cookie.Builder expires(@Nullable Instant expires);

        /**
         * Sets whether the <code>httponly</code> flag should be set.
         *
         * @param httpOnly Whether this cookie should be inaccessible from JavaScript
         * @return The builder
         */
        @NotNull
        Cookie.Builder httpOnly(boolean httpOnly);

        /**
         * Sets the expiration duration of the cookie.
         *
         * @param maxAge The maximum lifetime of this cookie in seconds, if any
         * @return The builder
         */
        @NotNull
        default Cookie.Builder maxAge(@Nullable Long maxAge) {
            if(maxAge == null) return expires(null);
            return expires(Instant.now().plusSeconds(maxAge).plusMillis(500));
        }

        /**
         * Sets the expiration duration of the cookie.
         *
         * @param maxAge The maximum lifetime of this cookie in seconds
         * @return The builder
         */
        default Cookie.Builder maxAge(long maxAge) {
            return maxAge((Long) maxAge);
        }

        /**
         * Sets whether the <code>partitioned</code> flag should be set.
         *
         * @param partitioned Whether this cookie should be partitioned
         * @return The builder
         */
        @NotNull
        Cookie.Builder partitioned(boolean partitioned);

        /**
         * Sets the path start that this cookie belongs to.
         *
         * @param path The path for this cookie, or <code>null</code> to match all (<code>"/"</code>)
         * @return The builder
         */
        @NotNull
        Cookie.Builder path(@Nullable Path path);

        /**
         * Sets the path start that this cookie belongs to.
         *
         * @param path The path for this cookie, or <code>null</code> to match all (<code>"/"</code>)
         * @return The builder
         */
        @NotNull
        default Cookie.Builder path(@Nullable String path) {
            return path(path != null ? Path.of(path) : null);
        }

        /**
         * Sets whether the <code>secure</code> flag should be set.
         *
         * @param secure Whether this cookie should only be sent via <code>https</code>
         * @return The builder
         */
        @NotNull
        Cookie.Builder secure(boolean secure);

        /**
         * Sets the value of the <code>SameSite</code> parameter.
         *
         * @param sameSite The same site option to use, or <code>null</code> for the default, which is {@link SameSite#LAX}.
         * @return The builder
         */
        @NotNull
        Cookie.Builder sameSite(@Nullable SameSite sameSite);

        /**
         * Builds a cookie from this cookie builder. This builder can be reused afterwards.
         *
         * @return The cookie with the values set
         */
        @NotNull
        Cookie build();
    }
}

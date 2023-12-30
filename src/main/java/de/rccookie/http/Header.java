package de.rccookie.http;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an HTTP header. A header consists of a set of key-value pairs,
 * where duplicate keys are allowed.
 */
public interface Header extends Map<String, Header.Values> {

    /**
     * Sets the value for the specified key of the header.
     *
     * @param key The name of the header field to set
     * @param value The value for the given header field
     * @return The previous value for the set header field
     */
    @Nullable
    default Header.Values put(String key, @NotNull String value) {
        return put(key, new EditableHeader.EditableValues(value)); // Should throw an exception anyway if immutable
    }

    /**
     * Sets the values for the specified key of the header, or removes the header field.
     *
     * @param key The name of the header field to set or remove
     * @param values The values to set, or <code>null</code> to remove any value
     * @return The previous values for the set header field
     */
    default Header.Values set(String key, @Nullable Values values) {
        return values != null ? put(key, values) : remove(key);
    }

    /**
     * Sets the value for the specified key of the header, or removes the header field.
     *
     * @param key The name of the header field to set or remove
     * @param value The value to set, or <code>null</code> to remove any value
     * @return The previous values for the set header field
     */
    default Header.Values set(String key, @Nullable String value) {
        return value != null ? put(key, value) : null;
    }

    default Values add(String key, String value) {
        Values v = computeIfAbsent(key, k -> new EditableHeader.EditableValues());
        v.add(value);
        return v;
    }

    default String addCookie(@NotNull Cookie cookie) {
        Arguments.checkNull(cookie, "cookie");
        Values values = get("Cookie");
        Map<String, Cookie> cookies;
        if(values == null || values.isEmpty()) cookies = new LinkedHashMap<>();
        else cookies = Cookie.parseCookies(values.get(0));
        cookies.put(cookie.name(), cookie);
        String cookieString = Cookie.toString(cookies.values());
        put("Cookie", cookieString);
        return cookieString;
    }

    default String addCookies(Collection<? extends Cookie> cookies) {
        Arguments.checkNull(cookies, "cookies");
        Values values = get("Cookie");
        Map<String, Cookie> cookieMap;
        if(values == null || values.isEmpty()) cookieMap = new LinkedHashMap<>();
        else cookieMap = Cookie.parseCookies(values.get(0));
        for(Cookie cookie : cookies)
            cookieMap.put(cookie.name(), cookie);
        String cookieString = Cookie.toString(cookieMap.values());
        put("Cookie", cookieString);
        return cookieString;
    }

    Values addSetCookie(@NotNull Cookie cookie);

    default Values setContentType(@Nullable ContentType contentType) {
        if(contentType == null)
            return remove("Content-Type");
        if(!contentType.isPrecise())
            throw new IllegalArgumentException("Mime type patterns not allowed in Content-Type");
        return put("Content-Type", contentType.toString());
    }

    default Values setAccept(@Nullable ContentTypes contentTypes) {
        if(contentTypes == null)
            return remove("Accept");
        for(ContentType c : contentTypes)
            Arguments.checkInclusive(c.weight(), 0.0, 1.0);
        return put("Accept", contentTypes.toString());
    }

    /**
     * Returns the values for the given key joined with newlines, or <code>null</code> if
     * the header does not contain the given key.
     *
     * @param key The key to get the values as string for
     * @return The values mapped to the given key joined to a single string, or <code>null</code>
     */
    default String getString(String key) {
        return getStringOrDefault(key, null);
    }

    /**
     * Returns the values for the given key joined with newlines, or the specified default if
     * the header does not contain the given key.
     *
     * @param key The key to get the values as string for
     * @param def The string to return if no value is mapped to the specified key
     * @return The values mapped to the given key joined to a single string, or the default value
     */
    default String getStringOrDefault(String key, String def) {
        Values values = get(key);
        return values != null ? values.stream().map(Object::toString).collect(Collectors.joining("\n")) : def;
    }

    @NotNull
    default Map<String, Cookie> getCookies() {
        Values values = get("Cookie");
        if(values == null || values.isEmpty()) return Map.of();
        return Cookie.parseCookies(values.get(0));
    }

    @NotNull
    default Map<String, Cookie> getSetCookies() {
        Values values = get("Set-Cookie");
        if(values == null) return Map.of();
        return values.stream().map(Cookie::parseCookie).collect(Collectors.toMap(Cookie::name, c->c));
    }

    default ContentType getContentType() {
        Values contentType = get("Content-Type");
        if(contentType == null) return null;
        return ContentType.of(contentType.get(0));
    }

    default ContentTypes getAccept() {
        Values accept = get("Accept");
        if(accept == null) return ContentTypes.ANY;

        if(accept.get(0).isBlank()) return ContentTypes.of();

        Set<ContentType> types = new HashSet<>();
        for(String str : accept.get(0).split(",")) {
            ContentType type = ContentType.of(str.trim());
            Arguments.checkInclusive(type.weight(), 0.0, 1.0);
            types.add(type);
        }
        return ContentTypes.of(types);
    }

    @Nullable
    default Header.Values putIfAbsent(String key, String value) {
        return putIfAbsent(key, values(value));
    }

    default boolean replace(String key, String oldValue, String newValue) {
        return replace(key, values(oldValue), values(newValue));
    }

    @Nullable
    default Header.Values replace(String key, String value) {
        return replace(key, values(value));
    }

    /**
     * Values mapped to a header key.
     */
    interface Values extends List<String> {
        /**
         * An empty, unmodifiable header field value.
         */
        Values EMPTY = new ReadonlyHeader.ReadonlyValues(List.of());
    }

    /**
     * Returns an unmodifiable header field value for the given values. No reference
     * to the list remains.
     *
     * @param values The values for the value object
     * @return The value object
     */
    static Values values(List<String> values) {
        return new ReadonlyHeader.ReadonlyValues(values);
    }

    /**
     * Returns an unmodifiable header field value for the given strings. Must not
     * be null or empty
     *
     * @param values The values for the value object
     * @return The values object
     */
    static Values values(String... values) {
        return new ReadonlyHeader.ReadonlyValues(values);
    }

    /**
     * Normalizes the given string as header key, throwing an exception if it has
     * invalid characters. Returns <code>null</code> if the object is not a string.
     */
    @Contract("null->null")
    static String normalizeKey(@Nullable Object key) {
        if(!(key instanceof String)) return null;

        char[] chars = ((String) key).toCharArray();
        if (chars.length == 0) return (String) key;

        boolean nextIsUpper = true;
        for(int i=0; i<chars.length; i++) {
            char c = chars[i];
            if(c == '\r' || c == '\n')
                throw new IllegalArgumentException("Newline in key '"+key+"'");
            if(nextIsUpper) {
                nextIsUpper = false;
                if(c >= 'a' && c <= 'z')
                    //noinspection lossy-conversions
                    chars[i] += 'A' - 'a';
            }
            else {
                if(c >= 'A' && c <= 'Z')
                    chars[i] += 'a' - 'A';
                nextIsUpper = c == ' ' || c == '-';
            }
        }
        return new String(chars);
    }


    /**
     * Returns a new empty, mutable header.
     *
     * @param context The http request to use as context if context is required for an operation
     * @return A new header
     */
    static Header newEmpty(@Nullable HttpRequest context) {
        return new EditableHeader(context);
    }

    /**
     * Returns an immutable header with the given values.
     *
     * @param values The values for the header, no reference will be kept
     * @return A readonly header with the given values
     */
    static Header of(Map<? extends String, ? extends List<? extends String>> values) {
        return new ReadonlyHeader(values);
    }
}

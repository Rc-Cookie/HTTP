package de.rccookie.http;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.rccookie.http.auth.AuthChallenge;
import de.rccookie.http.header.RateLimit;
import de.rccookie.http.useragent.UserAgent;
import de.rccookie.json.Json;
import de.rccookie.util.Arguments;
import de.rccookie.util.Console;
import de.rccookie.util.Utils;
import de.rccookie.util.login.Login;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an HTTP header. A header consists of a set of key-value pairs,
 * where duplicate keys are allowed.
 */
public interface Header extends Map<String, Header.Values> {

    int _init = init();
    private static int init() {
        Json.registerDeserializer(Header.class, json -> Header.of(json.asMap(String.class, Values.class)));
        return 0;
    }

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

    default String setDate(@Nullable Instant date) {
        if(date == null) {
            remove("Date");
            return null;
        }
        String str = date.atZone(ZoneId.of("GMT")).format(DateTimeFormatter.RFC_1123_DATE_TIME);
        put("Date", str);
        return str;
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

    default Values setUserAgent(@Nullable UserAgent userAgent) {
        if(userAgent == null || userAgent == UserAgent.UNKNOWN)
            return remove("User-Agent");
        return put("User-Agent", userAgent.raw());
    }

    default Values setBasicAuth(String username, String password) {
        Arguments.checkNull(username, "username");
        Arguments.checkNull(password, "password");
        if(username.contains(":"))
            throw new IllegalArgumentException("Username must not contain ':'");
        return put("Authorization", "Basic "+Utils.toBase64(username+":"+password));
    }

    default Values setBasicAuth(Login credentials) {
        return setBasicAuth(Arguments.checkNull(credentials, "credentials").username, credentials.password);
    }

    default Values setAuthenticate(@Nullable List<? extends AuthChallenge> challenges) {
        if(challenges == null || challenges.isEmpty())
            return remove("WWW-Authenticate");
        return put("WWW-Authenticate", mutableValues(challenges.stream().map(AuthChallenge::toString).collect(Collectors.toList())));
    }

    default Values addAuthenticate(AuthChallenge challenge) {
        return add("WWW-Authenticate", Arguments.checkNull(challenge, "challenge").toString());
    }

    default Values setProxyAuthenticate(@Nullable List<? extends AuthChallenge> challenges) {
        if(challenges == null || challenges.isEmpty())
            return remove("Proxy-Authenticate");
        return put("Proxy-Authenticate", mutableValues(challenges.stream().map(AuthChallenge::toString).collect(Collectors.toList())));
    }

    default Values addProxyAuthenticate(AuthChallenge challenge) {
        return add("Proxy-Authenticate", Arguments.checkNull(challenge, "challenge").toString());
    }

    default Values setAge(@Nullable Integer age) {
        return set("Age", age+"");
    }

    default void setRateLimit(@Nullable RateLimit rateLimit, @NotNull RateLimit.Naming naming) {
        Arguments.checkNull(naming, "naming");
        setRateLimit(rateLimit, naming.headerPrefix());
    }

    default void setRateLimit(@Nullable RateLimit rateLimit, @NotNull String namePrefix) {
        Arguments.checkNull(namePrefix, "namePrefix");
        if(rateLimit == null) {
            remove(namePrefix+"-Limit");
            remove(namePrefix+"-Remaining");
            remove(namePrefix+"-Reset");
        }
        else {
            set(namePrefix+"-Limit", rateLimit.limit()+"");
            set(namePrefix+"-Remaining", rateLimit.remaining()+"");
            set(namePrefix+"-Reset", Math.max(0, rateLimit.timeUntilReset().getSeconds())+"");
        }
    }

    default Values setKeepAlive(@Nullable Boolean keepAlive) {
        return setConnection(keepAlive);
    }

    default Values setConnection(@Nullable Boolean keepAlive) {
        if(keepAlive == null)
            return remove("Connection");
        return set("Connection", keepAlive ? "Connection" : "close");
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

    default Instant getDate() {
        Values values = get("Date");
        if(values == null || values.isEmpty())
            return null;
        return ZonedDateTime.parse(values.get(0), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
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
        Map<String, Cookie> cookies = new HashMap<>();
        for(String v : values) try {
            Cookie c = Cookie.parseCookie(v);
            cookies.put(c.name(), c);
        } catch(Exception e) {
            Console.warn("Failed to parse Set-Cookie: {}:", v);
            Console.warn(e);
        }
        return cookies;
    }

    default ContentType getContentType() {
        Values contentType = get("Content-Type");
        if(contentType == null) return null;
        return ContentType.of(contentType.get(0));
    }

    @NotNull
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

    @NotNull
    default UserAgent getUserAgent() {
        Values userAgent = get("User-Agent");
        return userAgent != null && !userAgent.isEmpty() ? new UserAgent(userAgent.get(0)) : UserAgent.UNKNOWN;
    }

    default Login getBasicAuth() {
        Values auth = get("Authorization");
        if(auth == null) return null;
        String authStr = auth.get(0);
        if(!authStr.startsWith("Basic ")) return null;
        String str = Utils.fromBase64(authStr.substring(6));
        int colon = str.indexOf(':');
        if(colon < 0)
            return new Login("", str);
        return new Login(str.substring(0, colon), str.substring(colon + 1));
    }

    @NotNull
    default List<AuthChallenge> getAuthenticate() {
        Values auths = get("WWW-Authenticate");
        if(auths == null) return List.of();
        List<AuthChallenge> challenges = new ArrayList<>();
        for(String auth : auths)
            challenges.addAll(AuthChallenge.parse(auth));
        return challenges;
    }

    default <C extends AuthChallenge> C getAuthenticate(Class<C> type) {
        for(AuthChallenge c : getAuthenticate())
            if(type.isInstance(c))
                return type.cast(c);
        return null;
    }

    @NotNull
    default List<AuthChallenge> getProxyAuthenticate() {
        Values auths = get("Proxy-Authenticate");
        if(auths == null) return List.of();
        List<AuthChallenge> challenges = new ArrayList<>();
        for(String auth : auths)
            challenges.addAll(AuthChallenge.parse(auth));
        return challenges;
    }

    default <C extends AuthChallenge> C getProxyAuthenticate(Class<C> type) {
        for(AuthChallenge c : getProxyAuthenticate())
            if(type.isInstance(c))
                return type.cast(c);
        return null;
    }

    default int getAge() {
        return (int) Double.parseDouble(getStringOrDefault("Age", "0"));
    }

    default RateLimit getRateLimit() {
        for(RateLimit.Naming naming : RateLimit.Naming.values()) {
            RateLimit rateLimit = getRateLimit(naming);
            if(rateLimit != null)
                return rateLimit;
        }
        return null;
    }

    default RateLimit getRateLimit(@NotNull RateLimit.Naming naming) {
        return getRateLimit(Arguments.checkNull(naming, "naming").headerPrefix());
    }

    default RateLimit getRateLimit(@NotNull String namePrefix) {
        Arguments.checkNull(namePrefix, "namePrefix");
        if(!containsKey(namePrefix+"-Limit") || !containsKey(namePrefix+"-Remaining") || !containsKey(namePrefix+"-Reset"))
            return null;
        int limit = Integer.parseInt(getString(namePrefix+"-Limit"));
        int remaining = Integer.parseInt(getString(namePrefix+"-Remaining"));
        long reset = Integer.parseInt(getString(namePrefix+"-Reset"));
        return new RateLimit(limit, remaining, Instant.ofEpochSecond(reset <= 60L * 24 * 356 * 20 ? System.currentTimeMillis() / 1000 + reset : reset));
    }

    default Boolean getKeepAlive() {
        return getConnection();
    }

    default Boolean getConnection() {
        String connection = getString("Connection");
        return connection != null ? !connection.equalsIgnoreCase("close") : null;
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
        Values EMPTY = Values.init();
        private static Values init() {
            Json.registerDeserializer(Values.class, json -> Header.mutableValues(json.as(String[].class)));
            return new ReadonlyHeader.ReadonlyValues(List.of());
        }
    }

    /**
     * Returns an unmodifiable header field value for the given values. No reference
     * to the list remains.
     *
     * @param values The values for the value object
     * @return The value object
     */
    static Values values(List<? extends String> values) {
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
     * Returns a mutable header field value for the given values. No reference to the list remains.
     *
     * @param values The values for the value object
     * @return The value object
     */
    static Values mutableValues(List<? extends String> values) {
        return new EditableHeader.EditableValues(values);
    }

    /**
     * Returns a mutable header field value for the given strings. Must not be null or empty
     *
     * @param values The values for the value object
     * @return The values object
     */
    static Values mutableValues(String... values) {
        return new EditableHeader.EditableValues(values);
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

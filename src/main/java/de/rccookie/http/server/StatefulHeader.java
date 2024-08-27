package de.rccookie.http.server;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.rccookie.http.Cookie;
import de.rccookie.http.Header;
import de.rccookie.util.Arguments;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class StatefulHeader implements Header {

    private final Header data;
    private final SendableHttpResponse response;

    StatefulHeader(SendableHttpResponse response) {
        this(Header.newEmpty(response.request), response);
    }

    StatefulHeader(Header data, SendableHttpResponse response) {
        this.data = Arguments.checkNull(data, "data");
        this.response = response;
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return data.equals(obj);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }

    @Override
    public Values get(Object key) {
        return data.get(key);
    }

    @Nullable
    @Override
    public Values put(String key, Values value) {
        synchronized(response) {
            response.checkState();
            return data.put(key, value);
        }
    }

    @Override
    public Values remove(Object key) {
        synchronized(response) {
            response.checkState();
            return data.remove(key);
        }
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends Values> m) {
        synchronized(response) {
            response.checkState();
            data.putAll(m);
        }
    }

    @Override
    public void clear() {
        synchronized(response) {
            response.checkState();
            data.clear();
        }
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return Utils.view(data.keySet());
    }

    @NotNull
    @Override
    public Collection<Values> values() {
        return Utils.view(data.values());
    }

    @NotNull
    @Override
    public Set<Entry<String, Values>> entrySet() {
        return Utils.view(data.entrySet());
    }

    @Override
    public Values addSetCookie(@NotNull Cookie cookie) {
        synchronized(response) {
            return add("Set-Cookie", Arguments.checkNull(cookie, "cookie").toString(response.request().route()));
        }
    }
}

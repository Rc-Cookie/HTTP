package de.rccookie.http;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.rccookie.util.Arguments;
import de.rccookie.util.ImmutableMap;
import de.rccookie.util.URLBuilder;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;

class QueryImpl implements ImmutableMap<String, String>, Query {

    private final Map<String, String> data;

    QueryImpl(Map<String, String> data) {
        this.data = Arguments.checkNull(data, "data");
    }

    @Override
    public String toString() {
        return new URLBuilder("http", "a").query(this).queryString();
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
    public String get(Object key) {
        return data.get(key);
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return Utils.view(data.keySet());
    }

    @NotNull
    @Override
    public Collection<String> values() {
        return Utils.view(data.values());
    }

    @NotNull
    @Override
    public Set<Entry<String, String>> entrySet() {
        return Utils.view(data.entrySet());
    }
}

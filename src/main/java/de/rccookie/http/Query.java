package de.rccookie.http;

import java.net.URL;
import java.util.Map;

import de.rccookie.util.URLBuilder;

public interface Query extends Map<String, String> {

    Query EMPTY = new QueryImpl(Map.of());

    static Query of(URL url) {
        return of(new URLBuilder(url));
    }

    static Query of(URLBuilder url) {
        return new QueryImpl(url.query());
    }
}

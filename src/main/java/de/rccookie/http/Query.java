package de.rccookie.http;

import java.net.URL;
import java.util.Map;

import de.rccookie.util.URLBuilder;

public interface Query extends Map<String, String> {

    static Query of(URL url) {
        return new QueryImpl(new URLBuilder(url).query());
    }
}

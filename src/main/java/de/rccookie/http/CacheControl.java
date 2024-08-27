package de.rccookie.http;

import java.util.HashMap;
import java.util.Map;

import de.rccookie.util.Arguments;
import de.rccookie.util.Utils;

public final class CacheControl {

    private final Map<String, String> directives = new HashMap<>();


    public int maxAge() {
        return getInt("max-age", 0);
    }

    public int sharedMaxAge() {
        return getInt("s-maxage", 0);
    }

    public boolean noCache() {
        return is("no-cache");
    }

    public boolean mustRevalidate() {
        return is("must-revalidate");
    }

    public boolean proxyRevalidate() {
        return is("proxy-revalidate");
    }

    public boolean noStore() {
        return is("no-store");
    }

    public boolean isPrivate() {
        return is("private");
    }

    public boolean isPublic() {
        return is("public");
    }

    public boolean mustUnderstand() {
        return is("must-understand");
    }

    public boolean noTransform() {
        return is("no-transform");
    }

    public boolean immutable() {
        return is("immutable");
    }

    public int staleWhileRevalidate() {
        return getInt("stale-while-revalidate", 0);
    }

    public int staleIfError() {
        return getInt("stale-if-error", 0);
    }

    public int maxStale() {
        return getInt("max-stale", 0);
    }

    public int minFresh() {
        return getInt("min-fresh", 0);
    }

    public boolean onlyIfCached() {
        return is("only-if-cached");
    }



    public String get(String directive) {
        return directives.get(Arguments.checkNull(directive, "directive").toLowerCase());
    }

    public boolean is(String directive) {
        return directives.containsKey(Arguments.checkNull(directive, "directive").toLowerCase());
    }

    public Integer getInt(String directive) {
        String val = get(directive);
        return val != null ? (int) Double.parseDouble(val) : null;
    }

    public int getInt(String directive, int defaultValue) {
        String val = get(directive);
        return val != null ? (int) Double.parseDouble(val) : defaultValue;
    }

    public Map<String, String> directives() {
        return Utils.view(directives);
    }
}

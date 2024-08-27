package de.rccookie.http.useragent;

public enum RenderEngine {
    BLINK("Blink", "Chrome"),
    GECKO("Gecko", "Gecko"),
    WEBKIT("AppleWebKit", "AppleWebKit"),
    PRESTO("Presto", "Presto"),
    EDGE_HTML("EdgeHTML", "EdgeHTML"),
    CURL("curl", "curl");

    final String name;
    final String versionPrefix;

    RenderEngine(String name, String versionPrefix) {
        this.name = name;
        this.versionPrefix = versionPrefix;
    }

    @Override
    public String toString() {
        return name;
    }
}

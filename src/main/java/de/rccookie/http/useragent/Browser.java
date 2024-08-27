package de.rccookie.http.useragent;

public enum Browser {
    FIREFOX("Firefox", "Mozilla"),
    SAFARI("Safari", "Safari"),
    CHROME("Chrome", "Chromium"),
    EDGE("Edge", "Edg"),
    OPERA("Opera", "OPR"),
    CURL("curl", "curl");

    final String name;
    final String versionPrefix;

    Browser(String name, String versionPrefix) {
        this.name = name;
        this.versionPrefix = versionPrefix;
    }

    @Override
    public String toString() {
        return name;
    }
}

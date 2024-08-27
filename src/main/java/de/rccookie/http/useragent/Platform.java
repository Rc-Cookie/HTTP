package de.rccookie.http.useragent;

public enum Platform {
    WINDOWS("Windows", false),
    MACOS("MacOS", false),
    LINUX_DESKTOP("Linux (Desktop)", false),
    UNKNOWN_DESKTOP("Unknown (Desktop)", false),
    ANDROID("Android", true),
    IOS("iOS", true),
    UNKNOWN_MOBILE("Unknown (Mobile)", true);

    final String name;
    final boolean mobile;

    Platform(String name, boolean mobile) {
        this.name = name;
        this.mobile = mobile;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isMobile() {
        return mobile;
    }
}

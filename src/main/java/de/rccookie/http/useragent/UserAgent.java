package de.rccookie.http.useragent;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserAgent {

    public static final UserAgent FIREFOX = new UserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0");
    public static final UserAgent FIREFOX_IOS = new UserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/122.2  Mobile/15E148 Safari/605.1.15");
    public static final UserAgent FIREFOX_ANDROID = new UserAgent("Mozilla/5.0 (Android 13; Mobile; rv:122.0) Gecko/122.0 Firefox/122.0");
    public static final UserAgent CHROME = new UserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");
    public static final UserAgent EDGE = new UserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0");
    public static final UserAgent SAFARI = new UserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.75.14 (KHTML, like Gecko) Version/7.0.3 Safari/7046A194A");
    public static final UserAgent SAFARI_IOS = new UserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1");
    public static final UserAgent CURL = new UserAgent("curl/8.4.0");
    public static final UserAgent UNKNOWN = new UserAgent("");

    private final String value;
    private VersionedRenderEngine engine;
    private VersionedBrowser browser;
    private Platform platform;

    public UserAgent(String value) {
        this.value = Arguments.checkNull(value, "value");
    }

    @Override
    public String toString() {
        VersionedBrowser browser = detectVersionedBrowser();
        VersionedRenderEngine engine = detectVersionedEngine();
        Platform platform = detectPlatform();

        String platformStr = platform == Platform.UNKNOWN_MOBILE ? "Mobile" : platform == Platform.UNKNOWN_DESKTOP ? "Desktop" : platform.toString();

        if(browser != null && engine != null)
            return browser.browser+" "+platformStr+"/"+browser.version+" ("+engine.engine+"/"+engine.version+")";
        if(browser != null)
            return browser.browser+" "+platformStr+"/"+browser.version+" (unknown engine) ["+value+"]";
        if(engine != null)
            return "Unknown "+platformStr+" browser ("+engine.engine+"/"+engine.version+") ["+value+"]";

        return "Unknown "+platformStr+" ["+value+"]";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UserAgent && value.equals(((UserAgent) obj).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public String raw() {
        return value;
    }

    @Nullable
    @Contract(pure = true)
    public RenderEngine detectEngine() {
        VersionedRenderEngine engine = detectVersionedEngine();
        return engine != null ? engine.engine : null;
    }

    @Nullable
    @Contract(pure = true)
    public VersionedRenderEngine detectVersionedEngine() {
        if(engine == null)
            engine = detectVersionedEngine0();
        return engine;
    }

    @Nullable
    @Contract(pure = true)
    public Browser detectBrowser() {
        VersionedBrowser browser = detectVersionedBrowser();
        return browser != null ? browser.browser : null;
    }

    @Nullable
    @Contract(pure = true)
    public VersionedBrowser detectVersionedBrowser() {
        if(browser == null)
            browser = detectVersionedBrowser0();
        return browser;
    }

    @NotNull
    @Contract(pure = true)
    public Platform detectPlatform() {
        if(platform == null)
            platform = detectPlatform0();
        return platform;
    }

    private VersionedRenderEngine detectVersionedEngine0() {
        if(value.contains("curl/"))
            return version(RenderEngine.CURL);
        if(value.contains("Opera/"))
            return version(RenderEngine.PRESTO);
        if(value.contains("OPR/"))
            return new VersionedRenderEngine(RenderEngine.BLINK, version("OPR"));
        if(value.contains("Edge/"))
            return version(RenderEngine.EDGE_HTML);
        if(value.contains("Edg/"))
            return new VersionedRenderEngine(RenderEngine.BLINK, version("Edg"));
        if((value.contains("iPhone") || value.contains("Mac") || value.contains("FxiOS/")) && /* Sanity check to prevent subsequent errors */ value.contains("AppleWebKit/"))
            return version(RenderEngine.WEBKIT);
        if(value.contains("Chrome/"))
            return version(RenderEngine.BLINK);
        if(value.startsWith("Mozilla/") && value.contains("Gecko/"))
            return version(RenderEngine.GECKO);
        return null;
    }

    private VersionedBrowser detectVersionedBrowser0() {
        if(value.contains("curl/"))
            return version(Browser.CURL);
        if(value.contains("Opera/"))
            return new VersionedBrowser(Browser.OPERA, version("Opera"));
        if(value.contains("OPR/"))
            return new VersionedBrowser(Browser.OPERA, version("OPR"));
        if(value.contains("FxiOS"))
            return new VersionedBrowser(Browser.FIREFOX, version("FxiOS"));
        if((value.contains("iPhone") || value.contains("Mac")) && /* Sanity check to prevent subsequent errors */ value.contains("Safari/"))
            return version(Browser.SAFARI);
        if(value.contains("Edge"))
            return new VersionedBrowser(Browser.EDGE, version("Edge"));
        if(value.contains("Edg"))
            return new VersionedBrowser(Browser.EDGE, version("Edg"));
        if(value.contains("Chrome"))
            return version(Browser.CHROME);
        if(value.contains("Mozilla"))
            return version(Browser.FIREFOX);
        return null;
    }

    private Platform detectPlatform0() {
        if(value.contains("Android"))
            return Platform.ANDROID;
        if(value.contains("FxiOS/") || value.contains("iPhone") || value.contains("iOS"))
            return Platform.IOS;
        if(value.contains("Mobi"))
            return Platform.UNKNOWN_MOBILE;
        if(value.contains("Windows") || value.contains("Win64") || value.contains("Win32"))
            return Platform.WINDOWS;
        if(value.contains("Mac"))
            return Platform.MACOS;
        if(value.contains("Linux"))
            return Platform.LINUX_DESKTOP;
        return Platform.UNKNOWN_DESKTOP;
    }

    private VersionedRenderEngine version(RenderEngine engine) {
        return new VersionedRenderEngine(engine, version(engine.versionPrefix));
    }

    private VersionedBrowser version(Browser browser) {
        return new VersionedBrowser(browser, version(browser.versionPrefix));
    }

    private String version(String prefix) {
        int vStart = value.indexOf(prefix+"/") + prefix.length() + 1;
        int vEnd = value.indexOf(' ', vStart);
        if(vEnd < 0) vEnd = value.indexOf(',', vStart);
        if(vEnd < 0) vEnd = value.indexOf(';', vStart);
        if(vEnd < 0) vEnd = value.indexOf('(', vStart);
        if(vEnd < 0) vEnd = value.indexOf(')', vStart);
        if(vEnd < 0) vEnd = value.length();
        return value.substring(vStart, vEnd);
    }
}

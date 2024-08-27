package de.rccookie.http.useragent;

import java.util.Objects;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;

public final class VersionedBrowser {

    @NotNull
    public final Browser browser;

    @NotNull
    public final String version;

    public VersionedBrowser(Browser browser, String version) {
        this.browser = Arguments.checkNull(browser, "browser");
        this.version = Arguments.checkNull(version, "version");
    }

    @Override
    public String toString() {
        return browser.versionPrefix+"/"+version;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof VersionedBrowser)) return false;
        VersionedBrowser that = (VersionedBrowser) o;
        return browser == that.browser && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(browser, version);
    }
}

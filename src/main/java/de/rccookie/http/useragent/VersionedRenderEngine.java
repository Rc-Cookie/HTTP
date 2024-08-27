package de.rccookie.http.useragent;

import java.util.Objects;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;

public final class VersionedRenderEngine {

    @NotNull
    public final RenderEngine engine;

    @NotNull
    public final String version;

    public VersionedRenderEngine(RenderEngine engine, String version) {
        this.engine = Arguments.checkNull(engine, "engine");
        this.version = Arguments.checkNull(version, "version");
    }

    @Override
    public String toString() {
        return engine.versionPrefix+"/"+version;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof VersionedRenderEngine)) return false;
        VersionedRenderEngine that = (VersionedRenderEngine) o;
        return engine == that.engine && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(engine, version);
    }
}

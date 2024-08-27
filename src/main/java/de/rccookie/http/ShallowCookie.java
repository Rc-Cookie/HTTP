package de.rccookie.http;

import java.time.Instant;
import java.util.Objects;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ShallowCookie implements Cookie {

    @NotNull
    private final String name;
    @NotNull
    private final String value;

    ShallowCookie(@NotNull String name, @NotNull String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString(Route context) {
        String str = name + '=' + value;
        if(!Arguments.checkNull(context, "context").isRoot())
            str += "; Path=/";
        return str;
    }

    @Override
    public String toString() {
        return name + '=' + value;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Cookie)) return false;
        Cookie that = (Cookie) o;
        return name.equals(that.name()) && value.equals(that.value());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public @NotNull String value() {
        return value;
    }

    @Override
    public @Nullable String domain() {
        return null;
    }

    @Override
    public @Nullable Instant expires() {
        return null;
    }

    @Override
    public boolean httpOnly() {
        return false;
    }

    @Override
    public boolean secure() {
        return false;
    }

    @Override
    public @Nullable Long maxAge() {
        return null;
    }

    @Override
    public boolean partitioned() {
        return false;
    }

    @Override
    public @NotNull Route path() {
        return Route.ROOT;
    }

    @NotNull
    @Override
    public Cookie.SameSite sameSite() {
        return SameSite.LAX;
    }
}

package de.rccookie.http;

import java.time.Instant;
import java.util.Objects;

import de.rccookie.util.Arguments;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class CookieBuilder implements Cookie.Builder {

    String name;
    String value;
    @Nullable
    String domain = null;
    @Nullable
    Instant expires = null;
    boolean httpOnly = false;
    boolean partitioned = false;
    @NotNull
    Path path = Path.ROOT;
    boolean secure = false;
    @NotNull
    Cookie.SameSite sameSite = SameSite.LAX;

    CookieBuilder(@NotNull String name,
                  @NotNull String value) {
        name(name);
        value(value);
    }

    @Override
    public String toString(Path context) {
        StringBuilder str = new StringBuilder();
        str.append(name).append('=').append(value);
        if(domain != null)
            str.append("; Domain=").append(domain);
        if(httpOnly)
            str.append("; HttpOnly");
        if(expires != null)
            str.append("; Max-Age=").append(maxAge());
        if(partitioned)
            str.append("; Partitioned");
        if(!context.normalize().equals(path.normalize()))
            str.append("; Path=").append(path);
        if(secure)
            str.append("; Secure");
        str.append("; SameSite=").append(sameSite);
        return str.toString();
    }

    @Override
    public String toString() {
        return toString(Path.ROOT);
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
        return domain;
    }

    @Override
    public @Nullable Instant expires() {
        return expires;
    }

    @Override
    public boolean httpOnly() {
        return httpOnly;
    }

    @Override
    public boolean partitioned() {
        return partitioned;
    }

    @Override
    public @NotNull Path path() {
        return path;
    }

    @Override
    public boolean secure() {
        return secure;
    }

    @Override
    public @NotNull Cookie.SameSite sameSite() {
        return sameSite;
    }

    @Override
    public @NotNull Cookie.Builder name(@NotNull String name) {
        this.name = Arguments.checkNull(name, "name");
        return this;
    }

    @Override
    public @NotNull Cookie.Builder value(@NotNull String value) {
        this.value = Arguments.checkNull(value, "value");
        return this;
    }

    @Override
    public @NotNull Cookie.Builder domain(String domain) {
        this.domain = domain;
        return this;
    }

    @Override
    public @NotNull Cookie.Builder expires(Instant expires) {
        this.expires = expires;
        return this;
    }

    @Override
    public @NotNull Cookie.Builder httpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    @Override
    public @NotNull Cookie.Builder partitioned(boolean partitioned) {
        this.partitioned = partitioned;
        return this;
    }

    @Override
    public @NotNull Cookie.Builder path(@Nullable Path path) {
        this.path = path != null ? path : Path.ROOT;
        return this;
    }

    @Override
    public @NotNull Cookie.Builder secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    @Override
    public @NotNull Cookie.Builder sameSite(@Nullable SameSite sameSite) {
        this.sameSite = sameSite != null ? sameSite : SameSite.LAX;
        return this;
    }

    @Override
    public @NotNull Cookie build() {
        return new CookieBuilder(name, value)
                .domain(domain)
                .expires(expires)
                .httpOnly(httpOnly)
                .partitioned(partitioned)
                .path(path)
                .secure(secure)
                .sameSite(sameSite);
    }
}

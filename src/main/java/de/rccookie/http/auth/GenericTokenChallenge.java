package de.rccookie.http.auth;

import java.util.Map;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class GenericTokenChallenge extends AbstractChallenge {

    @NotNull
    private final String schema;
    @Nullable
    private final String token;

    public GenericTokenChallenge(@NotNull String schema, @Nullable String token) {
        this.schema = Arguments.checkNull(schema, "schema");
        this.token = token;
    }

    @Override
    @NotNull
    public String scheme() {
        return schema;
    }

    @Override
    @Nullable
    public String token() {
        return token;
    }

    @Override
    @Contract("->null")
    public String realm() {
        return null;
    }

    @Override
    public String param(String name) {
        return null;
    }

    @Override
    public @NotNull Map<String, String> params() {
        return Map.of();
    }
}

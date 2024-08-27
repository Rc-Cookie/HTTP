package de.rccookie.http.auth;

import java.util.Map;

import de.rccookie.util.Arguments;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;

class GenericParameterChallenge extends AbstractChallenge {

    private final String scheme;
    private final Map<String, String> params;

    GenericParameterChallenge(String scheme, Map<String, String> params) {
        this.scheme = Arguments.checkNull(scheme, "scheme");
        this.params = Utils.view(Arguments.checkNull(params, "params"));
    }

    @Override
    public @NotNull String scheme() {
        return scheme;
    }

    @Override
    public String realm() {
        return param("realm");
    }

    @Override
    public String param(String name) {
        return params.get(name);
    }

    @Override
    public @NotNull Map<String, String> params() {
        return params;
    }
}

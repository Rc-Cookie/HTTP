package de.rccookie.http.auth;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BearerChallenge extends AbstractChallenge {

    @Nullable
    private final String realm;
    @NotNull
    private final List<String> scopes;
    @Nullable
    private final ErrorCode error;
    @Nullable
    private final String errorDescription;
    @Nullable
    private final URI errorURI;

    public BearerChallenge(@Nullable String realm, @Nullable List<String> scopes, @Nullable ErrorCode error, @Nullable String errorDescription, @Nullable URI errorURI) {
        this.realm = realm;
        this.scopes = scopes != null ? List.copyOf(scopes) : List.of();
        for(String scope : this.scopes)
            if(scope.contains(" "))
                throw new IllegalArgumentException("Scopes must not contain whitespaces: "+scope);
        this.error = error;
        this.errorDescription = errorDescription;
        this.errorURI = errorURI;
    }

    @Override
    public @NotNull String scheme() {
        return "Bearer";
    }

    @Override
    @Nullable
    public String realm() {
        return realm;
    }

    @NotNull
    public List<String> scopes() {
        return scopes;
    }

    @Nullable
    public ErrorCode error() {
        return error;
    }

    @Nullable
    public String errorMessage() {
        return errorDescription;
    }

    @Nullable
    public URI errorURI() {
        return errorURI;
    }

    @Override
    public String param(String name) {
        switch(name.toLowerCase()) {
            case "realm": return realm;
            case "scope": return scopes.isEmpty() ? null : String.join(" ", scopes);
            case "error": return error != null ? error.toString() : null;
            case "error_description": return errorDescription;
            case "error_uri": return errorURI != null ? errorURI.toString() : null;
            default: return null;
        }
    }

    @Override
    public @NotNull Map<String, String> params() {
        Map<String, String> params = new HashMap<>();
        if(realm != null)
            params.put("realm", realm);
        if(!scopes.isEmpty())
            params.put("scope", String.join(" ", scopes));
        if(error != null)
            params.put("error", error.toString());
        if(errorDescription != null)
            params.put("error_description", errorDescription);
        if(errorURI != null)
            params.put("error_uri", errorURI.toString());
        return params;
    }



    public enum ErrorCode {
        INVALID_REQUEST,
        INVALID_TOKEN,
        INSUFFICIENT_SCOPE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }



    public static BearerChallenge parse(@Nullable String realm, @Nullable String scope, @Nullable String error, @Nullable String errorDescription, @Nullable String errorURI) {
        return new BearerChallenge(
                realm,
                scope != null ? Arrays.asList(scope.split(" ")) : null,
                error != null ? ErrorCode.valueOf(error.toUpperCase()) : null,
                errorDescription,
                errorURI != null ? URI.create(errorURI) : null
        );
    }
}

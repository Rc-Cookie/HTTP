package de.rccookie.http.auth;

import java.util.HashMap;
import java.util.Map;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MutualChallenge extends AbstractChallenge {

    @NotNull
    private final String version;
    @NotNull
    private final String realm;
    @NotNull
    private final Algorithm algorithm;
    @NotNull
    private final Validation validation;
    @Nullable
    private final String authScope;
    @NotNull
    private final String rawReason;
    @NotNull
    private final Reason reason;

    public MutualChallenge(@NotNull String version, @NotNull String realm, @NotNull Algorithm algorithm, @NotNull Validation validation, @Nullable String authScope, @NotNull String rawReason) {
        this.version = Arguments.checkNull(version, "version");
        this.realm = Arguments.checkNull(realm, "realm");
        this.algorithm = Arguments.checkNull(algorithm, "algorithm");
        this.validation = Arguments.checkNull(validation, "validation");
        this.authScope = authScope;
        this.rawReason = Arguments.checkNull(rawReason, "rawReason");
        Reason reason;
        try {
            reason = Reason.valueOf(rawReason.toUpperCase().replace('-', '_'));
        } catch(IllegalArgumentException e) {
            reason = Reason.AUTH_FAILED;
        }
        this.reason = reason;
    }


    @Override
    public @NotNull String scheme() {
        return "Mutual";
    }

    @Override
    @NotNull
    public String realm() {
        return realm;
    }

    @NotNull
    public String version() {
        return version;
    }

    @NotNull
    public Algorithm algorithm() {
        return algorithm;
    }

    @NotNull
    public Validation validation() {
        return validation;
    }

    @Nullable
    public String authScope() {
        return authScope;
    }

    @NotNull
    public String rawReason() {
        return rawReason;
    }

    @NotNull
    public Reason reason() {
        return reason;
    }

    @Override
    public String param(String name) {
        switch(name.toLowerCase()) {
            case "version": return version;
            case "realm": return realm;
            case "algorithm": return algorithm.toString();
            case "validation": return validation.toString();
            case "auth-scope": return authScope;
            case "reason": return rawReason;
            default: return null;
        }
    }

    @Override
    public @NotNull Map<String, String> params() {
        Map<String, String> params = new HashMap<>();
        params.put("version", version);
        params.put("realm", realm);
        params.put("algorithm", algorithm.toString());
        params.put("validation", validation.toString());
        if(authScope != null)
            params.put("auth-scope", authScope);
        params.put("reason", rawReason);
        return params;
    }


    public enum Algorithm {
        ISO_KAM3_DL_2048_SHA256,
        ISO_KAM3_DL_4096_SHA512,
        ISO_KAM3_EC_2048_SHA256,
        ISO_KAM3_EC_4096_SHA512;

        @Override
        public String toString() {
            return name().toLowerCase().replace('_', '-');
        }
    }

    public enum Validation {
        HOST,
        TLS_SERVER_END_POINT,
        TLS_UNIQUE;

        @Override
        public String toString() {
            return name().toLowerCase().replace('_', '-');
        }
    }

    public enum Reason {
        INITIAL,
        STALE_SESSION,
        AUTH_FAILED,
        REAUTH_NEEDED,
        INVALID_PARAMETERS,
        INTERNAL_ERROR,
        USER_UNKNOWN,
        INVALID_CREDENTIAL,
        AUTHZ_FAILED;

        @Override
        public String toString() {
            return name().toLowerCase().replace('_', '-');
        }
    }



    public static MutualChallenge parse(@NotNull String version,
                                        @NotNull String realm,
                                        @NotNull String algorithm,
                                        @NotNull String validation,
                                        @Nullable String authScope,
                                        @NotNull String reason) {
        return new MutualChallenge(
                version,
                realm,
                Algorithm.valueOf(algorithm.toUpperCase().replace('-', '_')),
                Validation.valueOf(validation.toUpperCase().replace('-', '_')),
                authScope,
                reason
        );
    }
}

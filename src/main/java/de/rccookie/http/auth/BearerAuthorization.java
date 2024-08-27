package de.rccookie.http.auth;

import org.jetbrains.annotations.NotNull;

public class BearerAuthorization extends GenericTokenAuthorization {

    public BearerAuthorization(@NotNull String token) {
        super("Bearer", token);
    }

    @Override
    @NotNull
    public String token() {
        return super.token();
    }

    @Override
    @NotNull
    public String credential() {
        return super.credential();
    }



    public static BearerAuthorization parse(String token) {
        return new BearerAuthorization(token);
    }
}

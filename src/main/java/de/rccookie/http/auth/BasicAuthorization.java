package de.rccookie.http.auth;

import java.util.Map;

import de.rccookie.util.Arguments;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;

public class BasicAuthorization extends AbstractAuthorization {

    @NotNull
    private final String username;
    @NotNull
    private final String password;

    public BasicAuthorization(@NotNull String username, @NotNull String password) {
        this.username = Arguments.checkNull(username, "username");
        if(username.contains(":"))
            throw new IllegalArgumentException("Username must not contain colon: "+username);
        this.password = Arguments.checkNull(password, "password");
    }

    @Override
    public @NotNull String scheme() {
        return "Basic";
    }

    @NotNull
    public String username() {
        return username;
    }

    @NotNull
    public String password() {
        return password;
    }

    @Override
    public String credential() {
        return Utils.toBase64(username+":"+password);
    }

    @Override
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



    public static BasicAuthorization parse(String credential) {
        String decoded = Utils.fromBase64(credential);
        int colon = decoded.indexOf(':');
        if(colon == -1)
            return new BasicAuthorization(decoded, "");
        return new BasicAuthorization(decoded.substring(0, colon), decoded.substring(colon + 1));
    }
}

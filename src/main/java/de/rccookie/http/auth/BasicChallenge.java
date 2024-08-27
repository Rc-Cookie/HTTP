package de.rccookie.http.auth;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

public class BasicChallenge extends AbstractChallenge {

    private final String realm;
    private final boolean utf8;

    public BasicChallenge(String realm, Boolean utf8) {
        this.realm = realm != null ? realm : "";
        this.utf8 = utf8 != null ? utf8 : false;
    }

    @Override
    public String realm() {
        return realm;
    }

    public boolean utf8() {
        return utf8;
    }

    @Override
    public @NotNull String scheme() {
        return "Basic";
    }

    @Override
    public String param(String name) {
        switch(name.toLowerCase()) {
            case "realm": return realm;
            case "charset": return utf8 ? "UTF-8" : null;
            default: return null;
        }
    }

    @Override
    public @NotNull Map<String, String> params() {
        return utf8 ? Map.of("realm", realm, "charset", "UTF-8") : Map.of("realm", realm);
    }



    public static BasicChallenge parse(String realm, String charset) {
        return new BasicChallenge(realm, charset != null && (charset.equalsIgnoreCase("utf-8") || charset.equalsIgnoreCase("utf8")));
    }
}

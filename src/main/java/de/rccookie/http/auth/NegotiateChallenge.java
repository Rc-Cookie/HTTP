package de.rccookie.http.auth;

import org.jetbrains.annotations.Nullable;

public class NegotiateChallenge extends GenericTokenChallenge {
    public NegotiateChallenge(@Nullable String token) {
        super("Negotiate", token);
    }
}

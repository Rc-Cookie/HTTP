package de.rccookie.http.auth;

import java.util.Objects;

abstract class AbstractChallenge implements AuthChallenge {

    @Override
    public String token() {
        return null;
    }

    @Override
    public String toString() {
        return AuthUtils.toString(scheme(), token(), params());
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme().toLowerCase(), params());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(!(obj instanceof AuthChallenge)) return false;
        AuthChallenge a = (AuthChallenge) obj;
        return scheme().equalsIgnoreCase(a.scheme()) && token().equals(a.token()) && params().equals(a.params());
    }
}

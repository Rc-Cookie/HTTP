package de.rccookie.http.auth;

import java.util.Objects;

abstract class AbstractAuthorization implements Authorization {

    @Override
    public String credential() {
        return null;
    }

    @Override
    public String toString() {
        return AuthUtils.toString(scheme(), credential(), params());
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme().toLowerCase(), params());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(!(obj instanceof Authorization)) return false;
        Authorization a = (Authorization) obj;
        return scheme().equalsIgnoreCase(a.scheme()) && credential().equals(a.credential()) && params().equals(a.params());
    }
}

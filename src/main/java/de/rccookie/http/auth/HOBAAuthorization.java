package de.rccookie.http.auth;

import java.util.Base64;
import java.util.Map;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;

public class HOBAAuthorization extends AbstractAuthorization {

    @NotNull
    private final String keyID;
    @NotNull
    private final String challenge;
    @NotNull
    private final String nonce;
    private final byte[] signature;

    public HOBAAuthorization(@NotNull String keyID, @NotNull String challenge, @NotNull String nonce, byte[] signature) {
        this.keyID = Arguments.checkNull(keyID, "keyID");
        this.challenge = Arguments.checkNull(challenge, "challenge");
        this.nonce = Arguments.checkNull(nonce, "nonce");
        this.signature = Arguments.checkNull(signature, "signature");
    }

    @Override
    public @NotNull String scheme() {
        return "HOBA";
    }

    @Override
    public String realm() {
        return null;
    }

    @NotNull
    public String keyID() {
        return keyID;
    }

    @NotNull
    public String challenge() {
        return challenge;
    }

    @NotNull
    public String nonce() {
        return nonce;
    }

    public byte[] signature() {
        return signature.clone();
    }

    @NotNull
    public String result() {
        return Base64.getUrlEncoder().encodeToString(keyID.getBytes())
               + "." + Base64.getUrlEncoder().encodeToString(challenge.getBytes())
               + "." + Base64.getUrlEncoder().encodeToString(nonce.getBytes())
               + "." + Base64.getUrlEncoder().encodeToString(signature);
    }

    @Override
    public String param(String name) {
        if(name.equalsIgnoreCase("result"))
            return result();
        return null;
    }

    @Override
    public @NotNull Map<String, String> params() {
        return Map.of("result", result());
    }
}

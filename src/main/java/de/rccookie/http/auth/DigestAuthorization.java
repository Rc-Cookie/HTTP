package de.rccookie.http.auth;

import java.net.URI;
import java.util.Map;

import de.rccookie.http.HttpRequest;
import de.rccookie.math.Mathf;
import org.jetbrains.annotations.NotNull;

import static de.rccookie.http.auth.AuthUtils.enquote;

public class DigestAuthorization extends AbstractAuthorization {

    @NotNull
    private final HttpRequest request;
    @NotNull
    private final String username;
    @NotNull
    private final String password;
    @NotNull
    private final String realm;
    private final URI uri;
    @NotNull
    private final QualityOfProtection qualityOfProtection;
    @NotNull
    private final String serverNonce;
    @NotNull
    private final String clientNonce;
    private final int nonceCount;
    private final boolean userHash;
    @NotNull
    private final DigestChallenge.Algorithm algorithm;
    private final boolean session;
    @NotNull
    private final String opaque;

    private String response = null;

    public DigestAuthorization(@NotNull HttpRequest request, @NotNull String username, @NotNull String password, @NotNull String realm, URI uri, @NotNull QualityOfProtection qualityOfProtection, @NotNull String serverNonce, @NotNull String clientNonce, int nonceCount, boolean userHash, @NotNull DigestChallenge.Algorithm algorithm, boolean session, @NotNull String opaque) {
        this.request = request;
        this.username = username;
        this.password = password;
        this.realm = realm;
        this.uri = uri;
        this.qualityOfProtection = qualityOfProtection;
        this.serverNonce = serverNonce;
        this.clientNonce = clientNonce;
        this.nonceCount = nonceCount;
        this.userHash = userHash;
        this.algorithm = algorithm;
        this.session = session;
        this.opaque = opaque;
    }

    @Override
    public @NotNull String scheme() {
        return "Digest";
    }

    @Override
    public String realm() {
        return realm;
    }

    public String response() {
        if(response == null) {
            byte[] realm = this.realm.getBytes();
            byte[] clientNonce = this.clientNonce.getBytes();
            byte[] serverNonce = this.serverNonce.getBytes();
            byte[] username = this.username.getBytes();
            byte[] password = this.password.getBytes();

            if(userHash)
                username = algorithm.hash(colonConcat(username, realm));

            byte[] a1 = colonConcat(username, realm, password);
            if(session)
                a1 = colonConcat(algorithm.hash(a1), serverNonce, serverNonce);

            byte[] a2 = colonConcat(request.method().toString().getBytes(), request.url().toString().getBytes());
            if(qualityOfProtection == QualityOfProtection.AUTH_WITH_INTEGRITY_PROTECTION) {
                request.body().buffer();
                a2 = colonConcat(a2, algorithm.hash(request.body().data()));
            }

            response = AuthUtils.bytesToHex(algorithm.hash(colonConcat(
                    algorithm.hash(a1),
                    serverNonce,
                    Integer.toHexString(nonceCount).getBytes(),
                    clientNonce,
                    qualityOfProtection.toString().getBytes(),
                    algorithm.hash(a2)
            )));
        }
        return response;
    }

    private static byte[] colonConcat(byte[]... arrays) {
        if(arrays.length == 0)
            return new byte[0];
        if(arrays.length == 1)
            return arrays[0];
        byte[] res = new byte[Mathf.sum(arrays, a -> a.length) + arrays.length - 1];
        for(int i=0,j=0; i<arrays.length; j+=arrays[i].length+1,i++) {
            System.arraycopy(arrays[i], 0, res, j, arrays[i].length);
            if(i != arrays.length - 1)
                res[j + arrays[i].length] = ':';
        }
        return res;
    }

    @Override
    public String param(String name) {
        return null;
    }

    @Override
    public @NotNull Map<String, String> params() {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();

        str.append("Digest username=");
        enquote(username, str);
        str.append(", realm=");
        enquote(realm, str);
        if(uri != null) {
            str.append(", uri=");
            enquote(uri.toASCIIString(), str);
        }
        str.append(", qop=").append(qualityOfProtection);
        str.append(", nonce=");
        enquote(serverNonce, str);
        str.append(", cnonce=");
        enquote(clientNonce, str);
        str.append(", nc=").append(Integer.toHexString(nonceCount));
        if(userHash)
            str.append(", userhash=\"true\"");
        str.append(", response=");
        enquote(response(), str);
        str.append(", algorithm=").append(algorithm);
        if(session)
            str.append("-sess");
        str.append(", opaque=");
        enquote(opaque, str);

        return str.toString();
    }

    public enum QualityOfProtection {
        AUTH("auth"),
        AUTH_WITH_INTEGRITY_PROTECTION("auth-int");

        private final String name;

        QualityOfProtection(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public QualityOfProtection fromString(String str) {
            if(str.equalsIgnoreCase("auth"))
                return AUTH;
            if(str.equalsIgnoreCase("auth-int"))
                return AUTH_WITH_INTEGRITY_PROTECTION;
            throw new IllegalArgumentException("Unknown quality of protection: "+str);
        }
    }
}

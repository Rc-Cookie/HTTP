package de.rccookie.http.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static de.rccookie.http.auth.AuthUtils.enquote;

public class DigestChallenge extends AbstractChallenge {

    @Nullable
    private final String realm;
    @NotNull
    private final List<String> domains;
    @NotNull
    private final String nonce;
    @NotNull
    private final String opaque;
    private final boolean stale;
    @NotNull
    private final Algorithm algorithm;
    private final boolean session;
    private final boolean supportsAuth;
    private final boolean supportsAuthWithIntegrityProtection;
    private final boolean utf8;
    private final boolean userHash;

    public DigestChallenge(@Nullable String realm,
                           @Nullable List<String> domains,
                           @NotNull String nonce,
                           @NotNull String opaque,
                           boolean stale,
                           @Nullable Algorithm algorithm,
                           Boolean session,
                           boolean supportsAuth,
                           boolean supportsAuthWithIntegrityProtection,
                           Boolean utf8,
                           Boolean userHash) {
        this.realm = realm;
        this.domains = domains != null ? List.copyOf(domains) : List.of();
        this.nonce = Arguments.checkNull(nonce, "nonce");
        this.opaque = Arguments.checkNull(opaque, "opaque");
        this.stale = stale;
        this.algorithm = algorithm != null ? algorithm : Algorithm.MD5;
        this.session = session != null ? session : false;
        this.supportsAuth = supportsAuth;
        this.supportsAuthWithIntegrityProtection = supportsAuthWithIntegrityProtection;
        this.utf8 = utf8 != null ? utf8 : false;
        this.userHash = userHash != null ? userHash : false;
    }

    @Override
    public @NotNull String scheme() {
        return "Digest";
    }

    @Override
    @Nullable
    public String realm() {
        return realm;
    }

    @NotNull
    public List<String> domains() {
        return domains;
    }

    @NotNull
    public String nonce() {
        return nonce;
    }

    @NotNull
    public String opaque() {
        return opaque;
    }

    public boolean stale() {
        return stale;
    }

    @NotNull
    public Algorithm algorithm() {
        return algorithm;
    }

    public boolean session() {
        return session;
    }

    public boolean supportsAuth() {
        return supportsAuth;
    }

    public boolean supportsAuthWithIntegrityProtection() {
        return supportsAuthWithIntegrityProtection;
    }

    public boolean utf8() {
        return utf8;
    }

    public boolean userHash() {
        return userHash;
    }

    @Override
    public String param(String name) {
        switch(name.toLowerCase()) {
            case "realm": return realm;
            case "domain": return domains.isEmpty() ? null : String.join(" ", domains);
            case "nonce": return nonce;
            case "opaque": return opaque;
            case "stale": return stale ? "true" : null;
            case "algorithm": return algorithm + (session ? "-sess" : "");
            case "qop": return qop();
            case "charset": return utf8 ? "UTF-8" : null;
            case "userhash": return userHash ? "true" : null;
            default: return null;
        }
    }

    private String qop() {
        return supportsAuth ? supportsAuthWithIntegrityProtection ? "auth, auth-int" : "auth" : supportsAuthWithIntegrityProtection ? "auth-int" : "";
    }

    @Override
    public @NotNull Map<String, String> params() {
        Map<String, String> params = new HashMap<>();
        if(realm != null)
            params.put("realm", realm);
        if(!domains.isEmpty())
            params.put("domain", String.join(" ", domains));
        params.put("nonce", nonce);
        params.put("opaque", opaque);
        if(stale)
            params.put("stale", "true");
        params.put("algorithm", algorithm + (session ? "-sess" : ""));
        params.put("qop", qop());
        if(utf8)
            params.put("charset", "UTF-8");
        if(userHash)
            params.put("userhash", "true");
        return params;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();

        str.append("Digest nonce=");
        enquote(nonce, str);
        if(realm != null) {
            str.append(", realm=");
            enquote(realm, str);
        }
        if(!domains.isEmpty()) {
            str.append(", domain=");
            enquote(String.join(" ", domains), str);
        }
        str.append(", opaque=");
        enquote(opaque, str);
        if(stale)
            str.append(", stale=true");
        str.append(", algorithm=").append(algorithm);
        if(session)
            str.append("-sess");
        str.append(", qop=");
        enquote(qop(), str);
        if(utf8)
            str.append(", charset=\"utf-8\"");
        if(userHash)
            str.append(", userhash=\"true\"");

        return str.toString();
    }

    public enum Algorithm {
        MD5,
        SHA_256,
        SHA_512;

        private MessageDigest messageDigest;

        @Override
        public String toString() {
            return name().replace('_', '-');
        }

        public MessageDigest messageDigest() {
            if(messageDigest == null) try {
                messageDigest = MessageDigest.getInstance(toString());
            } catch(NoSuchAlgorithmException e) {
                throw new UnsupportedOperationException(e);
            }
            return messageDigest;
        }

        public byte[] hash(String str) {
            return hash(str.getBytes());
        }

        public byte[] hash(byte[] bytes) {
            return messageDigest.digest(bytes);
        }
    }



    public static DigestChallenge parse(@Nullable String realm,
                                        @Nullable String domain,
                                        @NotNull String nonce,
                                        @NotNull String opaque,
                                        @Nullable String stale,
                                        @Nullable String algorithm,
                                        @NotNull String qup,
                                        @Nullable String charset,
                                        @Nullable String userhash) {
        String lowerQUP = qup.toLowerCase();
        return new DigestChallenge(
                realm,
                domain != null ? Arrays.asList(domain.split("\\s+")) : null,
                nonce,
                opaque,
                stale != null && stale.equalsIgnoreCase("true"),
                algorithm != null ? Algorithm.valueOf(algorithm.toUpperCase().replace("-SESS", "").replace('-', '_')) : null,
                algorithm != null && algorithm.toLowerCase().endsWith("-sess"),
                lowerQUP.contains("auth,") || lowerQUP.endsWith("auth"),
                lowerQUP.contains("auth-int"),
                charset != null && (charset.equalsIgnoreCase("utf-8") || charset.equalsIgnoreCase("utf8")),
                userhash != null && userhash.equalsIgnoreCase("true")
        );
    }
}

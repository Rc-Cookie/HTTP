package de.rccookie.http.auth;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import de.rccookie.util.Arguments;
import de.rccookie.util.URLBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class HOBAChallenge extends AbstractChallenge {

    @Nullable
    private final String realm;
    @Range(from = 0)
    private final int maxAge;
    @NotNull
    private final Challenge challenge;

    public HOBAChallenge(@Nullable String realm, @Range(from = 0) int maxAge, @NotNull Challenge challenge) {
        this.realm = realm;
        this.maxAge = maxAge;
        this.challenge = challenge;
    }

    @Override
    public @NotNull String scheme() {
        return "HOBA";
    }

    @Override
    public String realm() {
        return realm;
    }

    @Range(from = 0)
    public int maxAge() {
        return maxAge;
    }

    @NotNull
    public Challenge challenge() {
        return challenge;
    }

    @Override
    public String param(String name) {
        switch(name.toLowerCase()) {
            case "realm": return realm;
            case "max-age": return maxAge+"";
            case "challenge": return challenge.toString();
            default: return null;
        }
    }

    @Override
    public @NotNull Map<String, String> params() {
        Map<String, String> params = new HashMap<>();
        if(realm != null)
            params.put("realm", realm);
        params.put("max-age", maxAge+"");
        params.put("challenge", challenge.toString());
        return params;
    }


    public static class Challenge {

        private final byte[] nonce;
        @Range(from = 0)
        private final int algorithmId;
        @NotNull
        private final URL origin;
        @NotNull
        private final String realm;
        private final byte[] keyId;
        private final byte[] challenge;

        public Challenge(byte[] nonce, int algorithmId, @NotNull URL origin, @Nullable String realm, byte[] keyId, byte[] challenge) {
            this.nonce = Arguments.checkNull(nonce, "nonce");
            this.algorithmId = Arguments.checkRange(algorithmId, 0, null);
            this.realm = realm != null ? realm : "";
            if(Arguments.checkNull(origin, "origin").getPort() == -1)
                throw new IllegalArgumentException("Origin must specify port: "+origin);
            if(!origin.getPath().isEmpty() || origin.getQuery() != null || origin.getRef() != null || origin.getUserInfo() != null) try {
                origin = new URLBuilder(origin)
                        .path(null)
                        .clearParams()
                        .ref(null)
                        .toURL();
            } catch(MalformedURLException e) {
                throw new RuntimeException(e);
            }
            this.origin = origin;
            this.keyId = Arguments.checkNull(keyId, "keyId");
            this.challenge = Arguments.checkNull(challenge, "challenge");
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();

            appendBase64(nonce, str);

            String algIdStr = algorithmId+"";
            str.append(algIdStr.length()).append(':').append(algIdStr);

            String originStr = origin.toString();
            str.append(originStr.length()).append(':').append(originStr);

            appendBase64(realm.getBytes(), str);
            appendBase64(keyId, str);
            appendBase64(challenge, str);

            return str.toString();
        }

        private static void appendBase64(byte[] data, StringBuilder out) {
            byte[] base64 = Base64.getUrlEncoder().encode(data);
            out.append(base64.length).append(':');
            for(byte b : data)
                out.append((char) b);
        }

        public byte[] nonce() {
            return nonce;
        }

        public Algorithm algorithm() {
            return Algorithm.fromId(algorithmId);
        }

        @NotNull
        public URL origin() {
            return origin;
        }

        @NotNull
        public String realm() {
            return realm;
        }

        public byte[] keyId() {
            return keyId;
        }

        public byte[] challenge() {
            return challenge;
        }


        public static Challenge parse(String str) {
            String[] parts = new String[6];
            int index = 0;
            for(int i=0; i<6; i++) {
                int count = str.charAt(index) - '0';
                while(true) {
                    char c = str.charAt(++index);
                    if(c == ':') break;
                    count = 10 * count + str.charAt(index) - '0';
                }
                index++;
                parts[i] = str.substring(index, index + count);
            }
            try {
                return new Challenge(
                        Base64.getUrlDecoder().decode(parts[0]),
                        Integer.parseInt(parts[1]),
                        new URI(parts[2]).toURL(),
                        new String(Base64.getUrlDecoder().decode(parts[3])),
                        Base64.getUrlDecoder().decode(parts[4]),
                        Base64.getUrlDecoder().decode(parts[5])
                );
            } catch(MalformedURLException | URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public enum Algorithm {
        RSA_SHA256,
        RSA_SHA1;

        private static final Algorithm[] values = values();

        @Override
        public String toString() {
            return name().replace('_', '-');
        }

        public static Algorithm fromId(int id) {
            return Arguments.checkRange(id, 0, null) < values.length ? values[id] : null;
        }
    }



    public static HOBAChallenge parse(String realm, String maxAge, String challenge) {
        return new HOBAChallenge(realm, Integer.parseInt(maxAge), Challenge.parse(challenge));
    }
}

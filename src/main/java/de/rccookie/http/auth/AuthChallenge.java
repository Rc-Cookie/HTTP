package de.rccookie.http.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.rccookie.http.auth.AuthUtils.decode;

/**
 * An authentication challenge as returned by a server in the <code>WWW-Authenticate</code>
 * response header field. Usually, one of the specific subclasses should probably be used,
 * e.g. {@link BasicChallenge} or {@link BearerChallenge}.
 */
public interface AuthChallenge {

    /**
     * The name of the scheme of the challenge.
     *
     * @return The name of the scheme
     */
    @NotNull
    String scheme();

    /**
     * The token property of this authorization challenge; which is not a regular parameter.
     * If the scheme of this challenge does not use a token value in the header after the scheme,
     * this will return <code>null</code>. If this scheme uses the token value, it does not
     * use any parameters, thus {@link #realm()} and {@link #param(String)} will return <code>null</code>
     * and {@link #params()} will return an empty map.
     *
     * @return The token of the challenge, or <code>null</code> if not applicable for this scheme
     */
    String token();

    /**
     * The realm parameter of the challenge, as returned by <code>param("realm")</code>.
     * May be <code>null</code> if not specified.
     *
     * @return The realm of this authentication challenge, or <code>null</code>
     */
    String realm();

    /**
     * Returns the value of the given parameter of this challenge, or <code>null</code>
     * if not present. The returned string will not be escaped like it might have been
     * in the header field. If this scheme doesn't use any parameters, this method returns
     * <code>null<</code> for all values of <code>name</code>.
     *
     * @param name The name of the parameter to get, insensitive
     * @return The value of the parameter, or <code>null</code> if not present
     */
    String param(String name);

    /**
     * Returns a map of all parameters of this challenge. The values will not be escaped
     * like they may have been in the header field. All keys in the returned map are in
     * lowercase. If this scheme doesn't use parameters, this returns an empty map.
     *
     * @return The parameters of this challenge
     */
    @NotNull
    Map<String, String> params();



    static List<AuthChallenge> parse(String str) {
        List<AuthChallenge> challenges = new ArrayList<>();

        int index = 0;
        int len = str.length();

        while(index < len) {

            int end = str.indexOf(' ', index);
            if(end == -1)
                end = len;
            String scheme = str.substring(index, end);
            index = end;

            boolean hasParams;
            if(index == len || str.charAt(index) == ',')
                hasParams = true;
            else if(scheme.equalsIgnoreCase("Negotiate")) {
                index++;
                hasParams = false;
            }
            else {
                index++;
                int equals = str.indexOf('=', index);
                int space = str.indexOf(' ', index);
                hasParams = equals == -1 || (space != -1 && space < equals);
            }

            AuthChallenge challenge;
            if(hasParams) {
                Map<String, String> params = new HashMap<>();
                if(index != len && str.charAt(index) == ',')
                    index += 2;
                else while(index != len) {
                    int equals = str.indexOf('=', index);
                    int space = str.indexOf(' ', index);
                    if(space == -1) space = len + 1;
                    if(equals == -1 || space < equals) break;

                    params.put(
                            str.substring(index, equals).toLowerCase(),
                            str.substring(equals + 1, space - 1)
                    );

                    index = Math.min(space + 1, len);
                }

                switch(scheme.toLowerCase()) {
                    case "negotiate": {
                        challenge = new NegotiateChallenge(null);
                        break;
                    }
                    case "basic": {
                        challenge = BasicChallenge.parse(
                                decode(params.get("realm")),
                                decode(params.get("charset"))
                        );
                        break;
                    }
                    case "bearer": {
                        challenge = BearerChallenge.parse(
                                decode(params.get("realm")),
                                decode(params.get("scope")),
                                decode(params.get("error")),
                                decode(params.get("error_description")),
                                decode(params.get("error_uri"))
                        );
                        break;
                    }
                    case "digest": {
                        challenge = DigestChallenge.parse(
                                decode(params.get("realm")),
                                decode(params.get("domain")),
                                decode(params.get("nonce")),
                                decode(params.get("opaque")),
                                decode(params.get("stale")),
                                decode(params.get("algorithm")),
                                decode(params.get("qup")),
                                decode(params.get("charset")),
                                decode(params.get("userhash"))
                        );
                        break;
                    }
                    case "hoba": {
                        challenge = HOBAChallenge.parse(
                                decode(params.get("realm")),
                                decode(params.get("max-age")),
                                decode(params.get("challenge"))
                        );
                        break;
                    }
                    case "mutual": {
                        challenge = MutualChallenge.parse(
                                decode(params.get("version")),
                                decode(params.get("realm")),
                                decode(params.get("algorithm")),
                                decode(params.get("validation")),
                                decode(params.get("auth-scope")),
                                decode(params.get("reason"))
                        );
                        break;
                    }
                    default: {
                        params.replaceAll((k,v) -> decode(v));
                        challenge = new GenericParameterChallenge(scheme, params);
                    }
                }
            }
            else {
                int comma = str.indexOf(',', index);
                if(comma == -1) comma = len;

                String token = str.substring(index, comma);
                index = Math.min(comma + 2, len);

                if(scheme.equalsIgnoreCase("negotiate"))
                    challenge = new NegotiateChallenge(token);
                else challenge = new GenericTokenChallenge(scheme, token);
            }
            challenges.add(challenge);
        }
        return challenges;
    }
}

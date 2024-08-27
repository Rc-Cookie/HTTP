package de.rccookie.http.auth;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

public interface Authorization {

    /**
     * The name of the scheme of the authorization.
     *
     * @return The name of the scheme
     */
    @NotNull
    String scheme();

    /**
     * The credential property of this authorization; which is not a regular parameter. If the scheme
     * of this authorization does not use a token value in the header after the scheme, this will
     * return <code>null</code>. If this scheme uses the credential value, it does not use any
     * parameters, thus {@link #realm()} and {@link #param(String)} will return <code>null</code>
     * and {@link #params()} will return an empty map.
     *
     * @return The credential of the authorization, or <code>null</code> if not applicable for this scheme
     */
    String credential();

    /**
     * The realm parameter of the authorization, as returned by <code>param("realm")</code>.
     * May be <code>null</code> if not specified.
     *
     * @return The realm of this authentication challenge, or <code>null</code>
     */
    String realm();

    /**
     * Returns the value of the given parameter of this authorization, or <code>null</code>
     * if not present. The returned string will not be escaped like it might have been
     * in the header field. If this scheme doesn't use any parameters, this method returns
     * <code>null<</code> for all values of <code>name</code>.
     *
     * @param name The name of the parameter to get, insensitive
     * @return The value of the parameter, or <code>null</code> if not present
     */
    String param(String name);

    /**
     * Returns a map of all parameters of this authorization. The values will not be escaped
     * like they may have been in the header field. All keys in the returned map are in
     * lowercase. If this scheme doesn't use parameters, this returns an empty map.
     *
     * @return The parameters of this authorization
     */
    @NotNull
    Map<String, String> params();
}

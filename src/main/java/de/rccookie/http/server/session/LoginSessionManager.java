package de.rccookie.http.server.session;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.Route;
import de.rccookie.http.server.HttpControlFlowException;
import de.rccookie.http.server.HttpRedirect;
import de.rccookie.http.server.HttpRequestFailure;
import de.rccookie.http.server.StaticHttpHandler;
import de.rccookie.util.Arguments;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoginSessionManager<T> implements SessionManager<T> {

    private final SessionManager<T> sessionManager;
    private final Function<HttpRequest, String> redirect;
    private final Authenticator<T>[] authenticators;

    @SafeVarargs
    public LoginSessionManager(SessionStorage<UUID,T> storage, String cookieName, Function<? super URL, ? extends String> redirect, Authenticator<T>... authenticators) {
        this.redirect = r -> redirect.apply(r.url());
        this.authenticators = Arguments.deepCheckNull(authenticators, "authenticators");
        sessionManager = new SimpleSessionManager<>(storage, cookieName, r -> {
            for(Authenticator<T> a : authenticators) {
                T session = a.getSessionFromAuth(r);
                if(session != null)
                    return session;
            }
            r.addResponseConfigurator(res -> {
                for(Authenticator<?> a : authenticators)
                    a.configure401(res);
            });
            throw HttpRedirect.temporary(this.redirect.apply(r));
        });
    }

    @SafeVarargs
    public LoginSessionManager(SessionStorage<UUID,T> storage, Function<? super URL, ? extends String> redirect, Authenticator<T>... authenticators) {
        this(storage, "session", redirect, authenticators);
    }

    @SafeVarargs
    public LoginSessionManager(SessionStorage<UUID,T> storage, String cookieName, String redirect, Authenticator<T>... authenticators) {
        this(storage, cookieName, url -> {
            boolean includeHost = !redirect.startsWith("/");
            if(redirect.contains("?"))
                return redirect + (redirect.endsWith("?") ? "" : "&")+"redirect="+encodedRoute(url, includeHost);
            return redirect + "?redirect="+encodedRoute(url, includeHost);
        }, authenticators);
    }

    @SafeVarargs
    public LoginSessionManager(SessionStorage<UUID,T> storage, String redirect, Authenticator<T>... authenticators) {
        this(storage, "session", redirect, authenticators);
    }

    private static String encodedRoute(URL url, boolean includeHost) {
        String s = (includeHost && (s = url.getAuthority()) != null && !s.isEmpty() ? "//" + s : "")
               + ((s = url.getPath()) != null ? s : "")
               + ((s = url.getQuery()) != null ? '?' + s : "")
               + ((s = url.getRef()) != null ? '#' + s : "");
        return URLEncoder.encode(s.startsWith("/") ? s : "/"+s, StandardCharsets.US_ASCII);
    }



    @Override
    public @NotNull T getSession(HttpRequest.Received request) {
        return sessionManager.getSession(request);
    }

    @Override
    public @Nullable T getSessionIfPresent(HttpRequest.Received request) {
        return sessionManager.getSessionIfPresent(request);
    }

    @Override
    public @NotNull T createSession(@NotNull T session, HttpRequest.Received context) {
        return sessionManager.createSession(session, context);
    }

    @Override
    public void deleteSession(HttpRequest.Received request) {
        sessionManager.deleteSession(request);
    }



    public SessionManager<T> failIfNotFound() {
        return failIfNotFound(HttpRequestFailure.unauthorized("Not logged in"));
    }

    public SessionManager<T> failIfNotFound(HttpControlFlowException failure) {
        Arguments.checkNull(failure, "failure");
        return failIfNotFound(r -> failure);
    }

    public SessionManager<T> failIfNotFound(Supplier<? extends HttpControlFlowException> failure) {
        Arguments.checkNull(failure, "failure");
        return failIfNotFound(r -> failure.get());
    }

    public SessionManager<T> failIfNotFound(Function<? super Route, ? extends HttpControlFlowException> failure) {
        Arguments.checkNull(failure, "failure");
        return new SessionManager<>() {
            @Override
            public @NotNull T getSession(HttpRequest.Received request) {
                T t = LoginSessionManager.this.getSessionIfPresent(request);
                if(t != null)
                    return t;
                for(Authenticator<T> a : authenticators) {
                    t = a.getSessionFromAuth(request);
                    if(t != null)
                        return sessionManager.createSession(t, request);
                }
                request.addResponseConfigurator(r -> {
                    for(Authenticator<?> a : authenticators)
                        a.configure401(r);
                });
                throw failure.apply(request.route());
            }

            @Override
            public @Nullable T getSessionIfPresent(HttpRequest.Received request) {
                return LoginSessionManager.this.getSessionIfPresent(request);
            }

            @Override
            public @NotNull T createSession(@NotNull T session, HttpRequest.Received context) {
                return LoginSessionManager.this.createSession(session, context);
            }

            @Override
            public void deleteSession(HttpRequest.Received request) {
                LoginSessionManager.this.deleteSession(request);
            }
        };
    }


    public StaticHttpHandler.Mapper asRedirectMapper(@Language("RegExp") String noRedirectPattern) {
        return asRedirectMapper(noRedirectPattern, true);
    }

    /**
     * Returns a {@link StaticHttpHandler.Mapper} that redirects to the login page if not authenticated.
     *
     * @param pattern A whitelist or blacklist pattern to specify routes to exclude or include having to be authenticated.
     *                This should especially include the login page itself, and possibly js and css file for it.
     * @param isExclude If <code>true</code>, <code>pattern</code> specifies the pages which <i>don't</i> require
     *                  being authenticated. If <code>false</code>, <code>pattern</code> specifies the pages which do
     *                  require authentication.
     * @return The corresponding mapper
     */
    public StaticHttpHandler.Mapper asRedirectMapper(@Language("RegExp") String pattern, boolean isExclude) {
        Pattern pat = Pattern.compile(pattern);
        return asRedirectMapper(r -> pat.matcher(r.toString()).matches() != isExclude);
    }

    /**
     * Returns a {@link StaticHttpHandler.Mapper} that redirects to the login page if not authenticated.
     *
     * @param loginRequired Determines whether a given route requires login. This should especially return
     *                      <code>false</code> for the login page itself, and possibly js and css file for it.
     * @return The corresponding mapper
     */
    public StaticHttpHandler.Mapper asRedirectMapper(Predicate<? super Route> loginRequired) {
        return r -> {
            Route route = r.route();
            if(getSessionIfPresent(r) != null || !loginRequired.test(route))
                return route;
            throw HttpRedirect.temporary(redirect.apply(r));
        };
    }
}

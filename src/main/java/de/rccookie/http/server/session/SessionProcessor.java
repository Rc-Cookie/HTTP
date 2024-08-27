package de.rccookie.http.server.session;

import java.lang.reflect.Constructor;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.server.HttpServer;
import de.rccookie.http.server.IllegalHttpProcessorException;
import de.rccookie.http.server.ServerLocal;
import de.rccookie.http.server.SimpleHttpProcessor;
import de.rccookie.util.Utils;

class SessionProcessor extends SimpleHttpProcessor<LoginRequired> {

    private final ServerLocal<SessionManager<?>> sessionManagers;

    public SessionProcessor(LoginRequired config) {
        super(config);
        Constructor<? extends LoginSessionManager<?>> ctor;
        if(config.manager() == NoTypeSpecified.class)
            ctor = null;
        else {
            try {
                ctor = config.manager().getDeclaredConstructor();
                ctor.setAccessible(true);
            } catch(NoSuchMethodException e) {
                throw new IllegalHttpProcessorException("Illegal @LoginRequired: " + config.manager() + " has no parameterless constructor");
            }
        }
        sessionManagers = new ServerLocal<>(s -> {
            LoginSessionManager<?> loginManager;
            if(ctor == null) {
                if(s instanceof HttpServer)
                    loginManager = ((HttpServer) s).getImplementation(LoginSessionManager.class);
                else
                    throw new IllegalHttpProcessorException("Illegal @LoginRequired: manager type not specified (automatic implementation finding only works for de.rccookie.http.server.HttpServer, but is " + s.getClass() + ")");
            } else try {
                loginManager = ctor.newInstance();
            } catch(Exception e) {
                throw Utils.rethrow(e);
            }
            return config.redirect() ? loginManager : loginManager.failIfNotFound();
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void preprocess(HttpRequest.Received request) {
        Object session = sessionManagers.get(request.serverObject()).getSession(request);

        Class sessionType = config.session();
        if(sessionType == NoTypeSpecified.class)
            sessionType = session.getClass();
        else sessionType.cast(session);

        request.bindOptionalParam(sessionType, session);
    }
}

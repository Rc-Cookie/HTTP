package de.rccookie.http.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.rccookie.http.HttpRequest;
import de.rccookie.http.Method;
import de.rccookie.http.Route;
import de.rccookie.http.server.session.LoginRequired;
import de.rccookie.http.server.session.LoginSessionManager;
import de.rccookie.util.Arguments;
import de.rccookie.util.Console;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A simple http server with routing support and http processor stack ("middleware") support.
 */
public class HttpServer extends RawHttpServer {


    Handler _404Handler = new Handler(this::default404Handler, true);
    HttpHeadHandler headHandler = HttpHeadHandler.DEFAULT;

    private final RootProcessor rootProcessor = new RootProcessor();
    private final List<HttpProcessor> processors = new ArrayList<>();

    private final ReadWriteLock handlersLock = new ReentrantReadWriteLock();
    private final Map<Method, Map<Route, Handler>> concreteHandlers = new EnumMap<>(Method.class);
    private final Map<Method, Map<RoutePattern, Handler>> patternHandlers = new EnumMap<>(Method.class);
    private final Map<Method, Map<RoutePattern, Handler>> doubleWildcardPatternHandlers = new EnumMap<>(Method.class);
    {
        for(Method method : Method.values()) {
            concreteHandlers.put(method, new HashMap<>());
            patternHandlers.put(method, new HashMap<>());
            doubleWildcardPatternHandlers.put(method, new HashMap<>());
        }
    }

    private final Map<Class<?>, Object> implementations = new ConcurrentHashMap<>();


    /**
     * Creates a new http server not yet bound to any port. The server should
     * first be configured, then bound to one or more ports using one of the
     * {@link #listen(int)} methods.
     */
    public HttpServer() { }


    @Override
    protected void respond(HttpRequest.Respondable request) throws Exception {
        Handler handler = findHandler(request);
        handler.execute(request);
        if(request.getResponse() == null)
            throw new AssertionError();
        request.getResponse().send();
    }

    @NotNull
    private Handler findHandler(HttpRequest.Received request) {

        Route route = request.route();

        handlersLock.readLock().lock();
        try {
            Handler handler = findHandler(route, request.method());
            if(handler != null) return handler;

            if(request.method() == Method.HEAD && (handler = findHandler(route, Method.GET)) != null)
                return new Handler(headHandler.getHandler(handler.handler), handler.useCommonProcessors, handler.extraProcessors);
        } finally {
            handlersLock.readLock().unlock();
        }

        return _404Handler;
    }

    @Nullable
    private Handler findHandler(Route route, Method method) {
        if(concreteHandlers.get(method).containsKey(route))
            return concreteHandlers.get(method).get(route);
        for(RoutePattern pattern : patternHandlers.get(method).keySet())
            if(pattern.matches(route))
                return patternHandlers.get(method).get(pattern);
        for(RoutePattern pattern : doubleWildcardPatternHandlers.get(method).keySet())
            if(pattern.matches(route))
                return doubleWildcardPatternHandlers.get(method).get(pattern);
        return null;
    }

    /**
     * Adds the given http processor to the http processors stack of this http server.
     * Multiple http processors will be executed in the order they were registered.
     *
     * @param processor The processor to register
     */
    public void addProcessor(HttpProcessor processor) {
        Arguments.checkNull(processor, "processor");
        synchronized(processors) {
            processors.add(processor);
        }
    }

    /**
     * Removes the given http processor for the http processors stack of this http server.
     *
     * @param processor The processor to remove
     * @return Whether the processor was registered previously
     */
    public boolean removeProcessor(HttpProcessor processor) {
        synchronized(processors) {
            return processors.remove(processor);
        }
    }

    /**
     * Registers the given http request handler on the specified route pattern.
     *
     * @param route The pattern to bind to. In general, handlers for specific routes will be prioritized
     *              over ones for a pattern with wildcards, which themselves will be prioritized over ones
     *              for a wildcard pattern including <code>"**"</code>.
     * @param handler The handler to register for the given route pattern
     * @param extraProcessors Additional http processors to execute before / after the given handler only
     * @param useCommonProcessors Setting this to <code>false</code> allows to bypass the http processors
     *                            registered now and in the future for the whole server
     * @param methods The methods to register the handler on
     */
    public void addHandler(String route, HttpRequestHandler handler, Collection<? extends HttpProcessor> extraProcessors, boolean useCommonProcessors, Method... methods) {
        Arguments.checkNull(handler, "handler");
        Arguments.checkNull(route, "route");
        if(Arguments.deepCheckNull(methods, "methods").length == 0)
            throw new IllegalArgumentException("At least one request method is required");

        Handler h = new Handler(handler, useCommonProcessors, extraProcessors.toArray(new HttpProcessor[0]));

        if(RoutePattern.containsPattern(route)) {
            RoutePattern pattern = RoutePattern.parse(route);
            handlersLock.writeLock().lock();
            try {
                if(pattern.containsDoubleWildcard()) {
                    for(Method method : methods)
                        doubleWildcardPatternHandlers.get(method).put(pattern, h);
                } else {
                    for(Method method : methods)
                        patternHandlers.get(method).put(pattern, h);
                }
            }
            finally {
                handlersLock.writeLock().unlock();
            }
        }
        else{
            Route routeObj = Route.of(route);
            handlersLock.writeLock().lock();
            try {
                for(Method method : methods)
                    concreteHandlers.get(method).put(routeObj, h);
            } finally {
                handlersLock.writeLock().unlock();
            }
        }
        String methodsStr = Arrays.stream(methods).map(Object::toString).collect(Collectors.joining("|"));
        methodsStr = methodsStr + Utils.repeat(" ", Math.max(0, 7 - methodsStr.length()));
        Console.mapDebug("Registered", methodsStr + " " + route + " - " + handler);
    }

    /**
     * Registers the given http request handler on the specified route pattern.
     *
     * @param route The pattern to bind to. In general, handlers for specific routes will be prioritized
     *              over ones for a pattern with wildcards, which themselves will be prioritized over ones
     *              for a wildcard pattern including <code>"**"</code>.
     * @param handler The handler to register for the given route pattern
     * @param useCommonProcessors Setting this to <code>false</code> allows to bypass the http processors
     *                            registered now and in the future for the whole server
     * @param methods The methods to register the handler on
     */
    public void addHandler(String route, HttpRequestHandler handler, boolean useCommonProcessors, Method... methods) {
        addHandler(route, handler, List.of(), useCommonProcessors, methods);
    }

    /**
     * Registers the given http request handler on the specified route pattern.
     *
     * @param route The pattern to bind to. In general, handlers for specific routes will be prioritized
     *              over ones for a pattern with wildcards, which themselves will be prioritized over ones
     *              for a wildcard pattern including <code>"**"</code>.
     * @param handler The handler to register for the given route pattern
     * @param extraProcessors Additional http processors to execute before / after the given handler only
     * @param methods The methods to register the handler on
     */
    public void addHandler(String route, HttpRequestHandler handler, Collection<? extends HttpProcessor> extraProcessors, Method... methods) {
        addHandler(route, handler, extraProcessors, true, methods);
    }

    /**
     * Registers the given http request handler on the specified route pattern.
     *
     * @param route The pattern to bind to. In general, handlers for specific routes will be prioritized
     *              over ones for a pattern with wildcards, which themselves will be prioritized over ones
     *              for a wildcard pattern including <code>"**"</code>.
     * @param handler The handler to register for the given route pattern
     * @param methods The methods to register the handler on. At least one is required.
     */
    public void addHandler(String route, HttpRequestHandler handler, Method... methods) {
        addHandler(route, handler, true, methods);
    }

    /**
     * Registers the http request listener on this server, that is, it registers all appropriate methods
     * declared in it as request handlers.
     *
     * @param listener The listener to register
     */
    public void addHandler(HttpRequestListener listener) {
        addHandler("", listener);
    }

    /**
     * Registers the http request listener on this server, that is, it registers all appropriate methods
     * declared in it as request handlers.
     *
     * @param listener The listener to register
     * @param routePrefix The route prefix to prepend to each route specified by the listener
     */
    public void addHandler(String routePrefix, HttpRequestListener listener) {
        Arguments.checkNull(listener, "listener");
        Arguments.checkNull(routePrefix, "routePrefix");
        if(!routePrefix.isEmpty() && !routePrefix.startsWith("/"))
            throw new IllegalArgumentException("Route prefix must start with '/'");

        de.rccookie.http.server.annotation.Route clsPrefix = listener.getClass().getAnnotation(de.rccookie.http.server.annotation.Route.class);
        routePrefix = HttpRequestListenerHandler.validateAndNormalize(routePrefix, true)
                      + (clsPrefix != null ? HttpRequestListenerHandler.validateAndNormalize(clsPrefix.value(), true) : "");

        for(java.lang.reflect.Method method : listener.getClass().getDeclaredMethods()) {
            HttpRequestListenerHandler handler = HttpRequestListenerHandler.forMethod(listener, method, routePrefix);
            if(handler != null)
                addHandler(handler.route, handler, handler.extraProcessors, handler.useCommonProcessors, handler.methods);
        }

        for(HttpRequestListener subRoute : listener.subRoutes())
            addHandler(routePrefix, subRoute);
    }

    /**
     * Unregisters the given handler from this server.
     *
     * @param handler The handler to unregister
     * @return Whether the handler was previously registered
     */
    public boolean removeHandler(HttpRequestHandler handler) {
        boolean change = false;
        handlersLock.writeLock().lock();
        try {
            for(Method method : Method.values()) {
                change |= concreteHandlers.get(method).values().removeIf(h -> h.handler.equals(handler));
                change |= patternHandlers.get(method).values().removeIf(h -> h.handler.equals(handler));
                change |= doubleWildcardPatternHandlers.get(method).values().removeIf(h -> h.handler.equals(handler));
            }
        } finally {
            handlersLock.writeLock().unlock();
        }
        return change;
    }

    /**
     * Sets the error formatter to be used to format {@link HttpRequestFailure}s.
     *
     * @param errorFormatter The error formatter to use, or <code>null</code> for the default formatter
     */
    public void setErrorFormatter(HttpErrorFormatter errorFormatter) {
        rootProcessor.errorFormatter = errorFormatter != null ? errorFormatter : HttpErrorFormatter.DEFAULT;
    }

    /**
     * Sets the head handler which handles http <code>HEAD</code> requests.
     *
     * @param headHandler The handler to use, or <code>null</code> for the default handler
     */
    public void setHeadHandler(HttpHeadHandler headHandler) {
        this.headHandler = headHandler != null ? headHandler : HttpHeadHandler.DEFAULT;
    }

    /**
     * Sets the http handler to use if no handler matched the route of an http request.
     *
     * @param _404Handler The 404 handler to use, or <code>null</code> for the default handler
     */
    public void set404Handler(HttpRequestHandler _404Handler) {
        this._404Handler = new Handler(_404Handler != null ? _404Handler : this::default404Handler, true);
    }

    private void default404Handler(HttpRequest request) {
//        if(request.method() == de.rccookie.http.Method.GET || request.method() == de.rccookie.http.Method.HEAD)
//            throw HttpRequestFailure.notFound();

        Route route = request.route();
        Set<Method> allowed = EnumSet.noneOf(Method.class);

        handlersLock.readLock().lock();
        try {
            for(Method method : Method.values()) {
                if(concreteHandlers.get(method).containsKey(route)
                   || patternHandlers.get(method).keySet().stream().anyMatch(p -> p.matches(route))
                   || doubleWildcardPatternHandlers.get(method).keySet().stream().anyMatch(p -> p.matches(route))) {
                    allowed.add(method);
                }
            }
        } finally {
            handlersLock.readLock().unlock();
        }
        if(allowed.isEmpty())
            throw HttpRequestFailure.notFound();
        if(allowed.contains(Method.GET) && headHandler.listHeadWithGet())
            allowed.add(Method.HEAD);
        throw HttpRequestFailure.methodNotAllowed(request.method(), allowed);
    }


    /**
     * Binds the given implementation instance to be used by classes requiring them, e.g. the {@link LoginSessionManager}
     * instance to be used with <code>@{@link LoginRequired}</code>. This serves as a unified interface to configure
     * implementation instances especially for annotations to avoid having to specify the configuration every time.
     *
     * @param type The type to register an implementation instance for
     * @param implementation The instance to use for the given type
     */
    public <T> void bindImplementation(Class<T> type, T implementation) {
        implementations.put(Arguments.checkNull(type, "type"), type.cast(implementation));
    }

    /**
     * Returns whether an implementation for the given type has been registered on this server.
     *
     * @param type The type of implementation to test if present
     * @return Whether such an implementation has been registered
     */
    public boolean hasImplementation(Class<?> type) {
        return implementations.containsKey(type);
    }

    /**
     * Returns the bound implementation instance for the given type, registered previously with {@link #bindImplementation(Class, Object)}.
     *
     * @param type The type of implementation to receive
     * @return The implementation instance used by the server
     * @throws NoSuchElementException If no implementation has been registered for that type
     */
    public <T> T getImplementation(Class<T> type) throws NoSuchElementException {
        return type.cast(implementations.computeIfAbsent(type, t -> {
            throw new NoSuchElementException("No implementation specified for "+type);
        }));
    }

    /**
     * Returns the bound implementation instance for the given type, if one has previously been registered using
     * {@link #bindImplementation(Class, Object)}, or returns the instance returned by the given supplier. If the
     * default supplied is used, that implementation will not be bound to the server.
     *
     * @param type The type of implementation to receive
     * @param defaultImplementation A supplier generating a default implementation if none has been bound to the server
     * @return The implementation bound to the server, or the instance returned by the default value function
     */
    public <T> T getImplementation(Class<T> type, Supplier<? extends T> defaultImplementation) {
        synchronized(implementations) {
            if(implementations.containsKey(type))
                return type.cast(implementations.get(type));
        }
        return defaultImplementation.get();
    }



    private final class Handler {
        private final HttpRequestHandler handler;
        private final boolean useCommonProcessors;
        private final HttpProcessor[] extraProcessors;

        private Handler(HttpRequestHandler handler, boolean useCommonProcessors, HttpProcessor... extraProcessors) {
            this.handler = Arguments.checkNull(handler, "handler");
            this.useCommonProcessors = useCommonProcessors;
            this.extraProcessors = Arguments.deepCheckNull(extraProcessors, "extraProcessors");
        }

        void execute(HttpRequest.Received request) {

            List<HttpProcessor> processors = new ArrayList<>();
            processors.add(rootProcessor);
            if(useCommonProcessors) {
                synchronized(HttpServer.this.processors) {
                    processors.addAll(HttpServer.this.processors);
                }
            }
            processors.addAll(Arrays.asList(extraProcessors));
            processors.add(new HandlerProcessor(handler));

            ThrowingRunnable[] executors = new ThrowingRunnable[processors.size() + 2];
            executors[executors.length - 1] = () -> { throw new AssertionError("Request handler called next function"); };
            for(int i=processors.size() - 1; i >= 0; i--) {
                HttpProcessor processor = processors.get(i);
                ThrowingRunnable next = executors[i+1];
                executors[i] = () -> {
                    try {
                        processor.process(request, next);
                        if(request.getResponse() == null)
                            throw new IllegalStateException("Http processor "+processor+" caught exception but did not write response");
                    } catch(Exception e) {
                        if(request.getResponse() != null && e instanceof HttpControlFlowException)
                            Console.warn("Http processor "+processor+" configured response but threw control flow exception. Response will be discarded");
                        request.invalidateResponse();
                        throw e;
                    }
                };
            }
            try {
                executors[0].run();
            } catch(RuntimeException e) {
                throw e;
            } catch(Exception e) {
                throw new AssertionError("Root processor threw checked exception: "+e, e);
            }
        }
    }
}

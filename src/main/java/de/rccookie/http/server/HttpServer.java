package de.rccookie.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.diogonunes.jcolor.Attribute;
import com.sun.net.httpserver.HttpExchange;
import de.rccookie.http.Body;
import de.rccookie.http.ContentType;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.Path;
import de.rccookie.http.Query;
import de.rccookie.http.ResponseCode;
import de.rccookie.json.JsonElement;
import de.rccookie.util.Arguments;
import de.rccookie.util.Console;
import de.rccookie.util.Utils;
import de.rccookie.util.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpServer {

    private final com.sun.net.httpserver.HttpServer server;


    HttpHeadHandler headHandler = HttpHeadHandler.DEFAULT;
    HttpRequestHandler _404Handler = this::default404Handler;
    HttpErrorHandler errorHandler = HttpErrorHandler.DEFAULT;
    HttpErrorFormatter errorFormatter = HttpErrorFormatter.DEFAULT;

    Function<? super String, ? extends String> pathPreprocessor = Function.identity();
    private final Map<HttpRequest.Method, Map<String, HttpRequestHandler>> concreteHandlers = new HashMap<>();
    private final Map<HttpRequest.Method, Map<PathPattern, HttpRequestHandler>> patternHandlers = new HashMap<>();
    {
        for(HttpRequest.Method method : HttpRequest.Method.values()) {
            concreteHandlers.put(method, new HashMap<>());
            patternHandlers.put(method, new HashMap<>());
        }
    }

    public HttpServer(int port) {
        this(port, 0);
    }

    public HttpServer(int port, int backlog) {
        try {
            server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), backlog);
        } catch(IOException e) {
            throw Utils.rethrow(e);
        }
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", this::handle);
        server.start();
    }

    private void handle(HttpExchange connection) {
        try {
            sendResponseToRequest(connection);
        } catch(Exception e) {
            Console.error("Failed to transfer response:");
            Console.error(e);
        }
    }

    private void sendResponseToRequest(HttpExchange connection) throws HttpSendException {
        Wrapper<HttpRequest.Received> request = new Wrapper<>();
        if(!executeUserCode(null, connection, () -> request.value = new ReceivedHttpRequest(this, connection, true), null))
            return;

        logRequest(request.value);
        HttpRequestHandler handler = findHandler(request.value);

        executeUserCode(request.value, connection, () -> handler.respondAsync(request.value), null);
    }

    private boolean executeUserCode(HttpRequest.Received request, HttpExchange connection, UserCode code, Consumer<Exception> exceptionHandler) throws HttpSendException {
        try {
            code.run();
            return true;
        } catch(HttpRedirect redirect) {
            executeUserCode(request, connection, () -> sendRedirect(request, redirect), exceptionHandler);
        } catch(HttpRequestFailure failure) {
            sendFailureResponse(request, connection, failure);
        } catch(Exception e) {
            if(exceptionHandler != null)
                exceptionHandler.accept(e);
            else handleUserException(request, connection, e);
        }
        return false;
    }

    private void handleUserException(HttpRequest.Received request, HttpExchange connection, Exception e) throws HttpSendException {
        executeUserCode(request, connection, () -> errorHandler.respondToErrorAsync(request, e), ex -> {
            Console.error("Exception in exception handler:");
            Console.error(ex);
            sendFailureResponse(request, connection, HttpRequestFailure.internal(e));
        });
    }

    private void sendFailureResponse(HttpRequest.Received request, HttpExchange connection, HttpRequestFailure failure) throws HttpSendException {
        HttpResponse.Sendable response;
        if(request != null) {
            response = request.respond(failure.code());
            try {
                failure.format(errorFormatter, response);
            } catch (Exception e) {
                Console.error("Error in error formatter:");
                Console.error(e);
                HttpErrorFormatter.DEFAULT.format(response, failure);
            }
        }
        else {
            response = new SendableHttpResponse(connection, null, failure.code());
            HttpErrorFormatter.DEFAULT.format(response, failure);
        }
        response.send();
    }

    private void sendRedirect(HttpRequest.Received request, HttpRedirect redirect) throws HttpSendException {
        HttpResponse.Sendable response = request.respond(redirect.code());
        redirect.format(response);
        response.send();
    }

    @NotNull
    private HttpRequestHandler findHandler(HttpRequest.Received request) {

        String path = request.path().toString();

        HttpRequestHandler handler = findHandler(path, request.method());
        if(handler != null) return handler;

        if(request.method() == HttpRequest.Method.HEAD && (handler = findHandler(path, HttpRequest.Method.GET)) != null)
            return headHandler.getHandler(handler);

        return _404Handler;
    }

    @Nullable
    private HttpRequestHandler findHandler(String path, HttpRequest.Method method) {
        if(concreteHandlers.get(method).containsKey(path))
            return concreteHandlers.get(method).get(path);
        for(PathPattern pattern : patternHandlers.get(method).keySet())
            if(pattern.matches(path))
                return patternHandlers.get(method).get(pattern);
        return null;
    }

    public void addHandler(HttpRequestHandler handler, String path, HttpRequest.Method... methods) {
        Arguments.checkNull(handler, "handler");
        Arguments.checkNull(path, "path");
        if(Arguments.deepCheckNull(methods, "methods").length == 0)
            throw new IllegalArgumentException("At least one request method is required");

        if(PathPattern.containsPattern(path)) {
            PathPattern pattern = PathPattern.parse(path);
            for(HttpRequest.Method method : methods)
                patternHandlers.get(method).put(pattern, handler);
        }
        else for(HttpRequest.Method method : methods)
            concreteHandlers.get(method).put(path, handler);
    }

    @SuppressWarnings("unchecked")
    public void addHandler(HttpRequestListener listener) {
        for(Method method : listener.getClass().getDeclaredMethods()) {
            method.setAccessible(true);

            On[] ons = getOns(method);
            if(ons.length == 0) continue;
            if(method.getReturnType() != void.class)
                throw new IllegalHttpRequestListenerException(method+": illegal return type - must be void");

            boolean async = method.getDeclaredAnnotation(AsyncResponse.class) != null;

            Class<?>[] paramTypes = method.getParameterTypes();
            Function<HttpRequest.Received,?>[] paramGenerators = new Function[paramTypes.length];
            Arrays.setAll(paramGenerators, i -> getGenerator(method, i));

            HttpRequestHandler handler = r -> {
                Object[] args = new Object[paramTypes.length];
                for (int i = 0; i < args.length; i++)
                    args[i] = paramGenerators[i].apply(r);
                try {
                    method.invoke(listener, args);
                } catch(InvocationTargetException e) {
                    throw Utils.rethrow(e.getCause() != null ? e.getCause() : e);
                }

                if(!async) {
                    HttpResponse.Sendable response = r.getResponse();
                    if(response == null)
                        throw new IllegalStateException("Non-async handler did not write response");
                    if(response.state() == HttpResponse.State.EDITABLE)
                        response.send();
                }
            };

            for(On on : ons) {
                addHandler(handler, on.path(), on.method());
                Console.mapDebug("Registered listener", Arrays.stream(on.method()).map(Object::toString).collect(Collectors.joining("|")), on.path(), "-", method);
            }
        }
    }

    public boolean removeHandler(HttpRequestHandler handler) {
        boolean change = false;
        for(HttpRequest.Method method : HttpRequest.Method.values()) {
            change |= concreteHandlers.get(method).values().remove(handler);
            change |= patternHandlers.get(method).values().remove(handler);
        }
        return change;
    }

    public void setHeadHandler(HttpHeadHandler headHandler) {
        this.headHandler = Arguments.checkNull(headHandler, "headHandler");
    }

    public void set404Handler(HttpRequestHandler _404Handler) {
        this._404Handler = Arguments.checkNull(_404Handler, "_404Handler");
    }

    private void default404Handler(HttpRequest request) {
        if(request.method() == HttpRequest.Method.GET || request.method() == HttpRequest.Method.HEAD)
            throw HttpRequestFailure.notFound();

        String path = request.path().toString();
        Set<HttpRequest.Method> allowed = new HashSet<>();

        for(HttpRequest.Method method : HttpRequest.Method.values()) {
            if(concreteHandlers.get(method).containsKey(path)
                || patternHandlers.get(method).keySet().stream().anyMatch(p -> p.matches(path))) {
                allowed.add(method);
            }
        }
        throw allowed.isEmpty() ? HttpRequestFailure.notFound() : HttpRequestFailure.methodNotAllowed(request.method(), allowed);
    }

    public void setErrorHandler(HttpErrorHandler errorHandler) {
        this.errorHandler = Arguments.checkNull(errorHandler, "errorHandler");
    }

    public Function<? super String, ? extends String> getPathPreprocessor() {
        return pathPreprocessor;
    }

    public void setPathPreprocessor(Function<? super String, ? extends String> preprocessor) {
        this.pathPreprocessor = Arguments.checkNull(preprocessor, "preprocessor");
    }

    public void appendPathPreprocessor(Function<? super String, ? extends String> preprocessor) {
        Arguments.checkNull(preprocessor, "preprocessor");
        Function<? super String, ? extends String> other = this.pathPreprocessor;
        this.pathPreprocessor = p -> preprocessor.apply(other.apply(p));
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }


    private static On[] getOns(Method method) {
        On.Multiple onsContainer = method.getAnnotation(On.Multiple.class);
        if(onsContainer != null) return onsContainer.value();
        return method.isAnnotationPresent(On.class) ?
                new On[] { method.getAnnotation(On.class) } :
                new On[0];
    }

    private static Function<HttpRequest,?> getGenerator(Method method, int param) {
        Class<?> type = method.getParameterTypes()[param];
        if(type == HttpRequest.class || type == HttpRequest.Received.class) return Function.identity();
        if(type == Body.class) return HttpRequest::body;
        if(type == Body.Multipart.class) return r -> r.body() != null ? Body.Multipart.parse(r.body()) : null;
        if(type == InputStream.class) return HttpRequest::stream;
        if(type == byte[].class) return HttpRequest::data;
        if(type == String.class) return HttpRequest::text;
        if(type == JsonElement.class) return HttpRequest::json;
        if(type == Header.class) return HttpRequest::header;
        if(type == Query.class) return HttpRequest::query;
        if(type == Method.class) return HttpRequest::method;
        if(type == Path.class) return HttpRequest::path;
        if(type == InetSocketAddress.class) return HttpRequest::client;

        Parse parse = Arrays.stream(method.getParameterAnnotations()[param])
                .filter(Parse.class::isInstance).map(Parse.class::cast).findAny()
                .orElseThrow(() -> new IllegalHttpRequestListenerException(method+" parameter "+(param+1)+": illegal type, no component of http request and not annotated with @Parse"));

        Parser parser = Parsers.getParser(parse);
        return r -> {
            ContentType contentType = r.contentType();
            if((contentType == null && !parser.supportsUnknownMIMEType()) || (contentType != null && !parser.getMIMETypes().contains(contentType)))
                throw HttpRequestFailure.unsupportedMediaType(contentType, parser.getMIMETypes());
            return parser.parse(r, type);
        };
    }


    private static final Object LOG_LOCK = new Object();

    static void logRequest(HttpRequest request) {
        String str = request.toString();
        String msg = "<<"+surroundWithLine(str)+"<< "+request.client().toString().substring(1);
        msg = msg.replace(str, Console.colored(str, Attribute.BOLD()));
        synchronized(LOG_LOCK) {
            Console.debug(msg);
        }
    }

    static void logResponse(HttpResponse response, boolean forceError) {
        String str = response.toString();
        String msg = ">>"+surroundWithLine(str)+">> "+response.client().toString().substring(1);
        msg = msg.replace(str, Console.colored(str, getColor(forceError ? ResponseCode.Type.SERVER_ERROR : response.code().type()), Attribute.BOLD()));
        synchronized(LOG_LOCK) {
            if(forceError || response.code().type() == ResponseCode.Type.SERVER_ERROR)
                Console.error(msg);
            else Console.debug(msg);
        }
    }

    private static Attribute getColor(ResponseCode.Type type) {
        switch(type) {
            case INFORMATIONAL: return Attribute.BLUE_TEXT();
            case SUCCESS: return Attribute.GREEN_TEXT();
            case REDIRECT: return Attribute.WHITE_TEXT();
            case CLIENT_ERROR: return Attribute.MAGENTA_TEXT();
            case SERVER_ERROR: return Attribute.RED_TEXT();
            default: throw new NullPointerException();
        }
    }

    private static final char LINE_CHAR = '=';
    private static final int MIN_LINE_SIZE = 4;
    private static final int PREFERRED_LOG_SIZE = 90;

    private static String surroundWithLine(String str) {
        int l,r;

        int min = str.length() + 2 * (MIN_LINE_SIZE + 1);
        if(min >= PREFERRED_LOG_SIZE)
            l = r = MIN_LINE_SIZE;
        else {
            l = MIN_LINE_SIZE + (PREFERRED_LOG_SIZE - min) / 2;
            r = MIN_LINE_SIZE + (PREFERRED_LOG_SIZE - min) - l;
        }

        return Utils.repeat(LINE_CHAR, l)+" "+str+" "+Utils.repeat(LINE_CHAR, r);
    }

    private interface UserCode {
        void run() throws Exception;
    }
}

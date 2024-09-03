package de.rccookie.http.server;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.rccookie.http.Body;
import de.rccookie.http.ContentType;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.Method;
import de.rccookie.http.Query;
import de.rccookie.http.ResponseCode;
import de.rccookie.http.server.annotation.HttpProcessorType;
import de.rccookie.http.server.annotation.NoCommonProcessors;
import de.rccookie.http.server.annotation.NullResponse;
import de.rccookie.http.server.annotation.Parse;
import de.rccookie.http.server.annotation.PathVar;
import de.rccookie.http.server.annotation.QueryParam;
import de.rccookie.http.server.annotation.Response;
import de.rccookie.http.server.annotation.Route;
import de.rccookie.http.server.annotation.methods.CONNECT;
import de.rccookie.http.server.annotation.methods.DELETE;
import de.rccookie.http.server.annotation.methods.GET;
import de.rccookie.http.server.annotation.methods.HEAD;
import de.rccookie.http.server.annotation.methods.OPTIONS;
import de.rccookie.http.server.annotation.methods.PATCH;
import de.rccookie.http.server.annotation.methods.POST;
import de.rccookie.http.server.annotation.methods.PUT;
import de.rccookie.http.server.annotation.methods.TRACE;
import de.rccookie.json.JsonDeserializer;
import de.rccookie.json.JsonElement;
import de.rccookie.json.JsonObject;
import de.rccookie.util.Console;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.Nullable;

class HttpRequestListenerHandler implements HttpRequestHandler {

    private static final Map<Class<? extends Annotation>, de.rccookie.http.Method> METHOD_ANNOTATIONS = Map.of(
            GET.class, de.rccookie.http.Method.GET,
            POST.class, de.rccookie.http.Method.POST,
            PUT.class, de.rccookie.http.Method.PUT,
            HEAD.class, de.rccookie.http.Method.HEAD,
            DELETE.class, de.rccookie.http.Method.DELETE,
            CONNECT.class, de.rccookie.http.Method.CONNECT,
            OPTIONS.class, de.rccookie.http.Method.OPTIONS,
            TRACE.class, de.rccookie.http.Method.TRACE,
            PATCH.class, de.rccookie.http.Method.PATCH
    );

    final String route;
    final Method[] methods;

    private final java.lang.reflect.Method method;
    private final HttpRequestListener listener;
    private final RoutePattern routePattern;
    final boolean useCommonProcessors;
    final List<HttpProcessor> extraProcessors = new ArrayList<>();
    private final ResponseCode responseCode;
    @SuppressWarnings("rawtypes")
    private final Serializer serializer;
    private final ContentType contentType;
    @Nullable
    private final ResponseCode nullResponseCode;
    private final Function<HttpRequest.Received, String> nullResponse;
    private final ContentType nullResponseContentType;
    private final Function<HttpRequest.Received,?>[] paramGenerators;


    @SuppressWarnings("unchecked")
    private HttpRequestListenerHandler(HttpRequestListener listener, java.lang.reflect.Method method, String route) {
        this.route = route;
        this.method = method;
        this.listener = listener;
        this.methods = getMethods(method);
        this.routePattern = RoutePattern.parse(route);
        this.useCommonProcessors = !method.isAnnotationPresent(NoCommonProcessors.class) && !method.getDeclaringClass().isAnnotationPresent(NoCommonProcessors.class);

        Response resp = method.getAnnotation(Response.class);
        if(resp == null) resp = DefaultResponse.INSTANCE;

        Class<?> returnType = method.getReturnType();
        if(returnType == void.class) {
            responseCode = resp.code() == ResponseCode.OK ? ResponseCode.NO_CONTENT : resp.code();
            serializer = null;
            contentType = null;
        }
        else {
            responseCode = resp.code();
            serializer = instantiateSerializer(resp.serializer());
            Class<?>[] allowedReturnTypes = serializer.requiredTypes();
            if(allowedReturnTypes.length != 0 && !Utils.anyMatch(allowedReturnTypes, t -> t.isAssignableFrom(returnType))) {
                throw new IllegalHttpRequestListenerException(
                        method + ": invalid return type for serializer " + resp.serializer().getSimpleName()
                        + ": must extend " + (allowedReturnTypes.length == 1 ? "" : "one of ")
                        + Arrays.stream(allowedReturnTypes).map(Objects::toString).collect(Collectors.joining(", "))
                );
            }

            if(resp.contentType().isEmpty())
                contentType = null;
            else try {
                contentType = ContentType.of(resp.contentType()).withoutParams();
            } catch(Exception e) {
                throw new IllegalHttpRequestListenerException("Invalid mime type: "+resp.contentType());
            }
        }

        for(Annotation a : method.getDeclaringClass().getAnnotations()) {
            HttpProcessorType processorType = a instanceof HttpProcessorType ? (HttpProcessorType) a : a.annotationType().getAnnotation(HttpProcessorType.class);
            if(processorType == null)
                continue;
            extraProcessors.add(instantiateProcessor(a, processorType.value(), method, false));
        }
        for(Annotation a : method.getAnnotations()) {
            HttpProcessorType processorType = a instanceof HttpProcessorType ? (HttpProcessorType) a : a.annotationType().getAnnotation(HttpProcessorType.class);
            if(processorType == null)
                continue;
            extraProcessors.add(instantiateProcessor(a, processorType.value(), method, true));
        }

        List<HttpProcessor> returnedExtraProcessors = Arrays.asList(listener.extraProcessors());
        extraProcessors.addAll(returnedExtraProcessors);
        if(listener instanceof HttpProcessor && !returnedExtraProcessors.contains(listener))
            extraProcessors.add((HttpProcessor) listener);

        NullResponse nullResponse = method.getAnnotation(NullResponse.class);
        if(nullResponse == null)
            nullResponse = method.getDeclaringClass().getAnnotation(NullResponse.class);
        else if(method.getReturnType() == void.class)
            throw new IllegalHttpRequestListenerException(method+": @NullResponse not allowed on void methods");
        NullResponse _nullResponse = nullResponse;

        if(nullResponse == null || method.getReturnType() == void.class) {
            nullResponseCode = null;
            this.nullResponse = null;
            nullResponseContentType = null;
        }
        else {
            nullResponseCode = nullResponse.code();
            if(nullResponse.contentType().isEmpty())
                nullResponseContentType = null;
            else try {
                nullResponseContentType = ContentType.of(nullResponse.contentType()).withoutParams();
            } catch(Exception e) {
                throw new IllegalHttpRequestListenerException("Invalid mime type: "+resp.contentType());
            }

            if(nullResponse.code().success())
                this.nullResponse = r -> _nullResponse.message();
            else if(nullResponse.message().isEmpty())
                this.nullResponse = r -> { throw HttpRequestFailure.defaultForCode(_nullResponse.code(), r); };
            else this.nullResponse = r -> { throw new HttpRequestFailure(_nullResponse.code(), _nullResponse.message()); };
        }

        paramGenerators = new Function[method.getParameterCount()];
        Arrays.setAll(paramGenerators, i -> getGenerator(method, routePattern, i));
    }

    @Override
    public String toString() {
        return method.getReturnType().getSimpleName()+" "+method.getName() +
               "(" + Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", ")) + ")";
    }

    @SuppressWarnings("unchecked")
    @Override
    public void respond(HttpRequest.Received request) throws Exception {
        Object[] args = Arrays.stream(paramGenerators).map(g -> g.apply(request)).toArray();
        Object result;

        CurrentHttpServerContext.pushRequest(request);
        try {
            result = method.invoke(listener, args);
        } catch(InvocationTargetException e) {
            throw Utils.rethrow(e.getCause() != null ? e.getCause() : e);
        } finally {
            CurrentHttpServerContext.popRequest();
        }

        HttpResponse.Editable response = request.getResponse();
        if(response != null && response.state() == HttpResponse.State.SENT)
            return;
        if(response == null)
            response = request.respond(responseCode);

        boolean contentTypeSet = response.header().containsKey("Content-Type");
        if(result != null) {
            if(response.body() == null) {
                assert serializer != null; // method cannot be void because result != null -> serializer present
                serializer.write(response, result);
            }
            else Console.warn("Http listener returned non-null value, but has already set response().body(). Discarding returned value.");
        }
        else if(nullResponseCode != null) {
            response.setText(nullResponse.apply(request));
            if(!contentTypeSet && nullResponseContentType != null) {
                response.setContentType(nullResponseContentType);
                contentTypeSet = true;
            }
        }


        if(!contentTypeSet) {
            if(contentType != null)
                response.setContentType(contentType);
            else if(serializer != null) {
                ContentType c = serializer.contentType(result);
                if(c != null)
                    response.setContentType(c);
            }
        }

        if(response.code() == ResponseCode.OK)
            response.setCode(responseCode == ResponseCode.OK && response.body() == null ? ResponseCode.NO_CONTENT : responseCode);
    }


    public static HttpRequestListenerHandler forMethod(HttpRequestListener listener, java.lang.reflect.Method method, String routePrefix) {
        method.setAccessible(true);

        String routeEnd;
        Route routeA = method.getDeclaredAnnotation(Route.class);
        if(routeA != null)
            routeEnd = routeA.value();
        else {
            routeEnd = null;
            for(Class<? extends Annotation> type : METHOD_ANNOTATIONS.keySet()) {
                if(method.getAnnotation(type) != null) {
                    routeEnd = "/"+method.getName();
                    break;
                }
            }
            if(routeEnd == null) return null;
        }

        return new HttpRequestListenerHandler(
                listener,
                method,
                validateAndNormalize(validateAndNormalize(routePrefix, true) + validateAndNormalize(routeEnd, false), false)
        );
    }

    static String validateAndNormalize(String route, boolean allowEmpty) {
        if(route.isEmpty()) {
            if(allowEmpty) return "";
            throw new IllegalHttpRequestListenerException("Illegal route: '' does not start with '/'");
        }
        if(!route.startsWith("/"))
            throw new IllegalHttpRequestListenerException("Illegal route: '"+route+"' does not start with '/'");
        return route.length() != 1 && route.endsWith("/") ? route.substring(0, route.length() - 1) : route;
    }

    private static Method[] getMethods(java.lang.reflect.Method method) {
        List<Method> methods = new ArrayList<>();
        METHOD_ANNOTATIONS.forEach((type, m) -> {
            if(method.isAnnotationPresent(type))
                methods.add(m);
        });
        return methods.isEmpty() ? new Method[] { Method.GET } : methods.toArray(new Method[0]);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Function<? super HttpRequest.Received, ?> getGenerator(java.lang.reflect.Method method, RoutePattern routePattern, int param) {

        Type type = method.getGenericParameterTypes()[param];

        Annotation[] annotations = method.getParameterAnnotations()[param];
        Parse parse = getAnnotation(Parse.class, annotations);
        PathVar pathVar = getAnnotation(PathVar.class, annotations);
        QueryParam queryParam = getAnnotation(QueryParam.class, annotations);

        int count = (parse != null ? 1 : 0) + (pathVar != null ? 1 : 0) + (queryParam != null ? 1 : 0);
        if(count > 1)
            throw new IllegalHttpRequestListenerException(method+" parameter "+(param+1)+": cannot be annotated with multiple of @Parse, @PathVar and @QueryParam");

        if(pathVar != null) {
            if(!routePattern.containsVariable(pathVar.value()))
                throw new IllegalHttpRequestListenerException(method+" parameter "+(param+1)+" '"+pathVar.value()+": path variable does not exist in route");
            return r -> {
                String val = routePattern.getVariable(r.route(), pathVar.value());
                try {
                    return JsonElement.wrap(val, JsonDeserializer.STRING_CONVERSION).as(type);
                } catch(Exception e) {
                    if(pathVar.reportErrorAsNotFound())
                        throw HttpRequestFailure.notFound();
                    throw HttpRequestFailure.badRequest(
                            "Bad path parameter value",
                            new JsonObject("expectedType", method.getParameterTypes()[param].getSimpleName(), "found", val, "fullRoute", r.route()),
                            e
                    );
                }
            };
        }
        if(queryParam != null) {
            Parser parser;
            if(!queryParam.json())
                parser = Parsers.createParser(queryParam.parser());
            else if(queryParam.parser() == Parser.JsonString.class || queryParam.parser() == Parser.Json.class)
                parser = new Parser.Json();
            else throw new IllegalHttpRequestListenerException(method+" parameter "+(param+1)+" '"+queryParam.value()+": conflicting parser types: json=true but different parser type specified");

            if(!queryParam.required() && queryParam.defaultVal().equals(QueryParam.NULL) && type instanceof Class && ((Class<?>) type).isPrimitive())
                throw new IllegalHttpRequestListenerException(method+" parameter "+(param+1)+" '"+queryParam.value()+": primitive parameter cannot be optional without default value");

            return r -> {
                String val = r.query().get(queryParam.value());
                if(val == null) {
                    if(!queryParam.defaultVal().equals(QueryParam.NULL)) {
                        try {
                            return parser.parse(Body.of(queryParam.defaultVal()), type, r);
                        } catch(Exception e) {
                            throw HttpRequestFailure.internal(new IllegalHttpProcessorException("Failed to parse default value '"+queryParam.defaultVal()+"' of "+method+" param "+(param+1)+" '"+queryParam.value()+"' into "+type, e));
                        }
                    }
                    if(!queryParam.required())
                        return null;
                    throw new HttpRequestFailure(
                            queryParam.errorCode(),
                            "Missing query parameter '"+queryParam.value()+"'",
                            queryParam.errorMsg() != null ? queryParam.errorMsg() : null
                    );
                }
                try {
                    return parser.parse(Body.of(val), type, r);
                } catch(HttpRequestFailure e) {
                    throw e;
                } catch(Exception e) {
                    throw HttpRequestFailure.badRequest(
                            "Bad query parameter value",
                            new JsonObject("parameterName", queryParam.value(), "expectedType", method.getParameterTypes()[param].getSimpleName(), "found", val),
                            e
                    );
                }
            };
        }

        if(parse == null) {
            if(type == HttpRequest.class || type == HttpRequest.Received.class) return Function.identity();
            if(type == Body.class) return HttpRequest::body;
            if(type == Body.Multipart.class) return r -> r.body() != null ? Body.Multipart.parse(r.body()) : null;
            if(type == InputStream.class) return HttpRequest::stream;
            if(type == byte[].class) return HttpRequest::data;
            if(type == String.class) return HttpRequest::text;
            if(type == JsonElement.class) return HttpRequest::json;
            if(type == Header.class) return HttpRequest::header;
            if(type == Query.class) return HttpRequest::query;
            if(type == java.lang.reflect.Method.class) return HttpRequest::method;
            if(type == de.rccookie.http.Route.class) return HttpRequest::route;
            if(type == InetSocketAddress.class) return HttpRequest::client;

            parse = method.getDeclaringClass().getDeclaredAnnotation(Parse.class);
            if(parse == null) {
                Function<? super HttpRequest.Received, ?> parser = createParser(Parser.Default.INSTANCE, type);
                if(type instanceof Class)
                    return r -> r.getOptionalParam((Class) type, () -> parser.apply(r));
                else return parser;
            }
        }

        return createParser(Parsers.getParser(parse), type);
    }

    private static Function<HttpRequest,?> createParser(Parser parser, Type type) {
        return r -> {
            ContentType contentType = r.contentType();
            if((contentType == null && !parser.supportsUnknownMIMEType()) || (contentType != null && !parser.getMIMETypes().contains(contentType)))
                throw HttpRequestFailure.unsupportedMediaType(contentType, parser.getMIMETypes());
            return parser.parse(r.body(), type, r);
        };
    }

    private static <T> T getAnnotation(Class<T> type, Annotation[] annotations) {
        return Arrays.stream(annotations).filter(type::isInstance).map(type::cast).findAny().orElse(null);
    }

    private static Serializer<?> instantiateSerializer(Class<? extends Serializer<?>> type) {
        try {
            Constructor<? extends Serializer<?>> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch(NoSuchMethodException e) {
            throw new IllegalHttpRequestListenerException("Invalid serializer: "+type+" has no parameterless constructor");
        } catch(InvocationTargetException e) {
            throw Utils.rethrow(e);
        } catch(InstantiationException e) {
            throw new IllegalHttpRequestListenerException("Could not instantiate serializer: "+e);
        } catch(IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static HttpProcessor instantiateProcessor(Annotation annotation, Class<? extends HttpProcessor> type, java.lang.reflect.Method method, boolean isOnMethod) {
        Constructor<? extends HttpProcessor> ctor;
        try {
            ctor = type.getDeclaredConstructor(annotation.annotationType());
        } catch(NoSuchMethodException e) {
            try {
                ctor = type.getDeclaredConstructor(annotation.annotationType(), java.lang.reflect.Method.class);
            } catch(NoSuchMethodException f) {
                try {
                    ctor = type.getDeclaredConstructor(annotation.annotationType(), java.lang.reflect.Method.class, boolean.class);
                } catch(NoSuchMethodException g) {
                    try {
                        ctor = type.getDeclaredConstructor();
                    } catch(NoSuchMethodException h) {
                        throw new IllegalHttpProcessorException(type+" neither has a constructor taking an instance of "+annotation.annotationType()+", nor ("+annotation.annotationType()+", java.lang.reflect.Method), nor ("+annotation.annotationType()+", java.lang.reflect.Method, boolean), nor a parameterless constructor");
                    }
                }
            }
        }
        ctor.setAccessible(true);
        try {
            if(ctor.getParameterCount() == 3)
                return ctor.newInstance(annotation, method, isOnMethod);
            else if(ctor.getParameterCount() == 2)
                return ctor.newInstance(annotation, method);
            else if(ctor.getParameterCount() == 1)
                return ctor.newInstance(annotation);
            else return ctor.newInstance();
        } catch(InvocationTargetException e) {
            throw Utils.rethrow(e);
        } catch(InstantiationException e) {
            throw new IllegalHttpProcessorException(type+" could not be instantiated: "+e);
        } catch(IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}

package de.rccookie.http.server;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import de.rccookie.http.ContentType;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.Method;
import de.rccookie.http.ResponseCode;
import de.rccookie.http.header.RateLimit;
import de.rccookie.json.JsonObject;
import de.rccookie.util.Arguments;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * A special type of {@link RuntimeException} which can / should be thrown by
 * http handlers when they fail to process the request (both because of client
 * or server error cause). They are caught by the http server and formatted to
 * an appropriate error response containing the supplied error details. Regular
 * exceptions thrown from handlers will cause an <code>500 INTERNAL SERVER ERROR</code>
 * response.
 */
public class HttpRequestFailure extends HttpControlFlowException {

    /**
     * Defines whether to read the current stack trace when creating a http request failure
     * for a client error code (4XX). Since these may happen regularly and reading the stack
     * trace takes a long time, much time can be saved by not reading the stack trace for
     * errors which are generally not the servers fault.
     */
    public static boolean STACKTRACE_ON_CLIENT_ERROR = false;

    private final ResponseCode code;
    private final String message;
    private final Object detail;
    private final HttpErrorFormatter formatter;

    public HttpRequestFailure(ResponseCode code, String message, Object detail, Throwable cause, @Nullable HttpErrorFormatter formatter) {
        super(Arguments.checkNull(code, "code")+(message != null ? ": "+message : ""), cause, true,
                STACKTRACE_ON_CLIENT_ERROR || code.type() == ResponseCode.Type.SERVER_ERROR);
        if(code.success())
            throw new IllegalArgumentException("Response code is not an error code: "+code);
        this.code = code;
        this.message = message;
        this.detail = detail;
        this.formatter = formatter;
    }

    public HttpRequestFailure(ResponseCode code, String message, Object detail, Throwable cause) {
        this(code, message, detail, cause, null);
    }

    public HttpRequestFailure(ResponseCode code, String message, Object detail) {
        this(code, message, detail, null);
    }

    public HttpRequestFailure(ResponseCode code, String message) {
        this(code, message, null);
    }

    public HttpRequestFailure(ResponseCode code) {
        this(code, null, null);
    }

    public ResponseCode code() {
        return code;
    }

    public String message() {
        return message;
    }

    public Object detail() {
        return detail;
    }

    @Override
    public void format(HttpResponse.Editable response) {
        format(null, response);
    }

    public void format(HttpErrorFormatter defaultFormatter, HttpResponse.Editable response) {
        (formatter != null ? formatter : Arguments.checkNull(defaultFormatter, "defaultFormatter"))
                .format(response, this);
    }


    public static HttpRequestFailure unsupportedMediaType(ContentType found, @Nullable Set<? extends ContentType> expected) {
        return new HttpRequestFailure(
                ResponseCode.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported type: "+(found != null ? found : "<none>"),
                expected != null ? new JsonObject("supportedTypes", expected) : null
        );
    }

    public static HttpRequestFailure parsingError(Throwable cause) {
        return new HttpRequestFailure(
                ResponseCode.BAD_REQUEST,
                getMessage(cause),
                null,
                cause
        );
    }

    private static String getMessage(Throwable t) {
        String msg = t.getMessage();
        Throwable cause = t.getCause();
        if(cause == null) return msg;
        String causeMsg = getMessage(cause);
        if(causeMsg == null) return msg;
        if(msg == null) return causeMsg;

        causeMsg = ": " + causeMsg;
        for(int i=causeMsg.length(); i>0; i--) {
            String common = causeMsg.substring(0, i);
            if(msg.endsWith(common))
                return msg + causeMsg.substring(i);
        }
        return msg + causeMsg;
    }

    public static HttpRequestFailure parsingError(Collection<? extends Throwable> cause) {
        if(cause.size() == 1)
            return parsingError(Utils.getAny(cause));
        HttpRequestFailure e = new HttpRequestFailure(
                ResponseCode.BAD_REQUEST,
                "Unable to parse request"
        );
        cause.forEach(e::addSuppressed);
        return e;
    }

    public static HttpRequestFailure notFound() {
        return notFound("The requested resource could not be located");
    }

    public static HttpRequestFailure notFound(String message) {
        return new HttpRequestFailure(ResponseCode.NOT_FOUND, message);
    }

    public static HttpRequestFailure methodNotAllowed(Method method, @Nullable Collection<? extends Method> allowed) {
        return new HttpRequestFailure(
                ResponseCode.METHOD_NOT_ALLOWED,
                "Method "+method+" not allowed",
                allowed != null ? new JsonObject("allowedMethods", allowed) : null
        ) {
            @Override
            public void format(HttpErrorFormatter defaultFormatter, HttpResponse.Editable response) {
                if(allowed != null && !allowed.isEmpty())
                    response.setHeaderField("Allow", allowed.stream().distinct().sorted().map(Method::toString).collect(Collectors.joining(", ")));
                super.format(defaultFormatter, response);
            }
        };
    }

    public static HttpRequestFailure badRequest(String message, @Nullable Object detail, @Nullable Throwable cause) {
        return new HttpRequestFailure(ResponseCode.BAD_REQUEST, message, detail, cause);
    }

    public static HttpRequestFailure badRequest(String message) {
        return badRequest(message, null, null);
    }

    public static HttpRequestFailure unauthorized(String message) {
        return new HttpRequestFailure(ResponseCode.UNAUTHORIZED, message);
    }

    public static HttpRequestFailure forbidden(String message) {
        return new HttpRequestFailure(ResponseCode.FORBIDDEN, message);
    }

    public static HttpRequestFailure conflict(String message) {
        return new HttpRequestFailure(ResponseCode.CONFLICT, message);
    }

    public static HttpRequestFailure tooManyRequests(@Range(from = 0) int limit, @NotNull Instant reset, @NotNull RateLimit.Naming naming) {
        return tooManyRequests(new RateLimit(limit, 0, reset), naming);
    }

    public static HttpRequestFailure tooManyRequests(@NotNull RateLimit rateLimit, @NotNull RateLimit.Naming naming) {
        return tooManyRequests(rateLimit, Arguments.checkNull(naming, "naming").headerPrefix());
    }

    public static HttpRequestFailure tooManyRequests(@NotNull RateLimit rateLimit, @NotNull String headerPrefix) {
        Arguments.checkNull(rateLimit, "rateLimit");
        return tooManyRequests("Request limit exceeded. Please wait until "+rateLimit.reset()+".", rateLimit, headerPrefix);
    }

    public static HttpRequestFailure tooManyRequests(@Nullable String message, @NotNull RateLimit rateLimit, @NotNull String headerPrefix) {
        Arguments.checkNull(rateLimit, "rateLimit");
        Arguments.checkNull(headerPrefix, "headerPrefix");
        return new HttpRequestFailure(
                ResponseCode.TOO_MANY_REQUESTS,
                message,
                rateLimit,
                null,
                HttpErrorFormatter.DEFAULT.and((r,f) -> r.header().setRateLimit(rateLimit, headerPrefix))
        );
    }

    public static HttpRequestFailure tooManyRequests(@Nullable String message) {
        return new HttpRequestFailure(ResponseCode.TOO_MANY_REQUESTS, message);
    }

    public static HttpRequestFailure tooManyRequests() {
        return tooManyRequests("Request limit exceeded. Try again later.");
    }

    public static HttpRequestFailure internal(String message) {
        return internal(message, null);
    }

    public static HttpRequestFailure internal(Throwable cause) {
        return internal(null, cause);
    }

    public static HttpRequestFailure internal(String message, Throwable cause) {
        return new HttpRequestFailure(ResponseCode.INTERNAL_SERVER_ERROR, message, null, cause);
    }

    public static HttpRequestFailure serviceUnavailable() {
        return serviceUnavailable((Throwable) null);
    }

    public static HttpRequestFailure serviceUnavailable(String message) {
        return serviceUnavailable(message, null);
    }

    public static HttpRequestFailure serviceUnavailable(Throwable cause) {
        return serviceUnavailable("The requested service is temporarily unavailable", cause);
    }

    public static HttpRequestFailure serviceUnavailable(String message, Throwable cause) {
        return new HttpRequestFailure(ResponseCode.SERVICE_UNAVAILABLE, message, null, cause);
    }


    public static HttpRequestFailure defaultForCode(ResponseCode code, HttpRequest request) {
        switch(code) {
            case NOT_FOUND: return notFound();
            case METHOD_NOT_ALLOWED: return methodNotAllowed(request.method(), null);
            case UNSUPPORTED_MEDIA_TYPE: return unsupportedMediaType(request.contentType(), null);
            case TOO_MANY_REQUESTS: return tooManyRequests();
            case SERVICE_UNAVAILABLE: return serviceUnavailable();
        }
        return new HttpRequestFailure(code);
    }
}

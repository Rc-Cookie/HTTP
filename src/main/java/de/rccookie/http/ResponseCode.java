package de.rccookie.http;

import java.util.Objects;

import de.rccookie.json.Json;
import de.rccookie.json.JsonSerializable;
import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * Represents a http status code constant.
 */
public final class ResponseCode implements JsonSerializable {

    static {
        Json.registerDeserializer(ResponseCode.class, json -> ResponseCode.get(json.asInt()));
    }

    private static final ResponseCode[] CODES = new ResponseCode[500];
    static {
        for(int i=0; i<CODES.length; i++)
            CODES[i] = new ResponseCode(i + 100, null);

        register(100, "CONTINUE");
        register(101, "SWITCHING PROTOCOLS");
        register(102, "PROCESSING");
        register(103, "EARLY HINTS");

        register(200, "OK");
        register(201, "CREATED");
        register(202, "ACCEPTED");
        register(203, "NON AUTHORITATIVE INFORMATION");
        register(204, "NO CONTENT");
        register(205, "RESET CONTENT");
        register(206, "PARTIAL CONTENT");
        register(207, "MULTI STATUS");
        register(208, "ALREADY REPORTED");
        register(226, "IM USED");

        register(300, "MULTIPLE CHOICE");
        register(301, "MOVED PERMANENTLY");
        register(302, "FOUND");
        register(303, "SEE OTHER");
        register(304, "NOT MODIFIED");
        register(305, "USE PROXY");
        register(306, "UNUSED");
        register(307, "TEMPORARY REDIRECT");
        register(308, "PERMANENT REDIRECT");

        register(400, "BAD REQUEST");
        register(401, "UNAUTHORIZED");
        register(402, "PAYMENT REQUIRED");
        register(403, "FORBIDDEN");
        register(404, "NOT FOUND");
        register(405, "METHOD NOT ALLOWED");
        register(406, "NOT ACCEPTABLE");
        register(407, "PROXY AUTHENTICATION REQUIRED");
        register(408, "REQUEST TIMEOUT");
        register(409, "CONFLICT");
        register(410, "GONE");
        register(411, "LENGTH REQUIRED");
        register(412, "PRECONDITION FAILED");
        register(413, "PAYLOAD TOO LARGE");
        register(414, "URI TOO LONG");
        register(415, "UNSUPPORTED MEDIA TYPE");
        register(416, "RANGE NOT SATISFIABLE");
        register(417, "EXPECTATION FAILED");
        register(418, "IM A TEAPOT");
        register(421, "MISDIRECTED REQUEST");
        register(422, "UNPROCESSABLE ENTITY");
        register(423, "LOCKED");
        register(424, "FAILED DEPENDENCY");
        register(425, "TOO EARLY");
        register(426, "UPGRADE REQUIRED");
        register(428, "PRECONDITION REQUIRED");
        register(429, "TOO MANY REQUESTS");
        register(431, "REQUEST HEADER FIELDS TOO LARGE");
        register(451, "UNAVAILABLE FOR LEGAL REASONS");

        register(500, "INTERNAL SERVER ERROR");
        register(501, "NOT IMPLEMENTED");
        register(502, "BAD GATEWAY");
        register(503, "SERVICE UNAVAILABLE");
        register(504, "GATEWAY TIMEOUT");
        register(505, "HTTP VERSION NOT SUPPORTED");
        register(506, "VARIANT ALSO NEGOTIATES");
        register(507, "INSUFFICIENT STORAGE");
        register(508, "LOOP DETECTED");
        register(510, "NOT EXTENDED");
        register(511, "NETWORK AUTHENTICATION REQUIRED");
    }

    public static final ResponseCode CONTINUE = get(100);
    public static final ResponseCode SWITCHING_PROTOCOLS = get(101);
    public static final ResponseCode PROCESSING = get(102);
    public static final ResponseCode EARLY_HINTS = get(103);

    public static final ResponseCode OK = get(200);
    public static final ResponseCode CREATED = get(201);
    public static final ResponseCode ACCEPTED = get(202);
    public static final ResponseCode NON_AUTHORITATIVE_INFORMATION = get(203);
    public static final ResponseCode NO_CONTENT = get(204);
    public static final ResponseCode RESET_CONTENT = get(205);
    public static final ResponseCode PARTIAL_CONTENT = get(206);
    public static final ResponseCode MULTI_STATUS = get(207);
    public static final ResponseCode ALREADY_REPORTED = get(208);
    public static final ResponseCode IM_USED = get(226);

    public static final ResponseCode MULTIPLE_CHOICE = get(300);
    public static final ResponseCode MOVED_PERMANENTLY = get(301);
    public static final ResponseCode FOUND = get(302);
    public static final ResponseCode SEE_OTHER = get(303);
    public static final ResponseCode NOT_MODIFIED = get(304);
    @Deprecated
    public static final ResponseCode USE_PROXY = get(305);
    public static final ResponseCode UNUSED = get(306);
    public static final ResponseCode TEMPORARY_REDIRECT = get(307);
    public static final ResponseCode PERMANENT_REDIRECT = get(308);

    public static final ResponseCode BAD_REQUEST = get(400);
    public static final ResponseCode UNAUTHORIZED = get(401);
    public static final ResponseCode PAYMENT_REQUIRED = get(402);
    public static final ResponseCode FORBIDDEN = get(403);
    public static final ResponseCode NOT_FOUND = get(404);
    public static final ResponseCode METHOD_NOT_ALLOWED = get(405);
    public static final ResponseCode NOT_ACCEPTABLE = get(406);
    public static final ResponseCode PROXY_AUTHENTICATION_REQUIRED = get(407);
    public static final ResponseCode REQUEST_TIMEOUT = get(408);
    public static final ResponseCode CONFLICT = get(409);
    public static final ResponseCode GONE = get(410);
    public static final ResponseCode LENGTH_REQUIRED = get(411);
    public static final ResponseCode PRECONDITION_FAILED = get(412);
    public static final ResponseCode PAYLOAD_TOO_LARGE = get(413);
    public static final ResponseCode URI_TOO_LONG = get(414);
    public static final ResponseCode UNSUPPORTED_MEDIA_TYPE = get(415);
    public static final ResponseCode RANGE_NOT_SATISFIABLE = get(416);
    public static final ResponseCode EXPECTATION_FAILED = get(417);
    public static final ResponseCode IM_A_TEAPOT = get(418);
    public static final ResponseCode MISDIRECTED_REQUEST = get(421);
    public static final ResponseCode UNPROCESSABLE_ENTITY = get(422);
    public static final ResponseCode LOCKED = get(423);
    public static final ResponseCode FAILED_DEPENDENCY = get(424);
    public static final ResponseCode TOO_EARLY = get(425);
    public static final ResponseCode UPGRADE_REQUIRED = get(426);
    public static final ResponseCode PRECONDITION_REQUIRED = get(428);
    public static final ResponseCode TOO_MANY_REQUESTS = get(429);
    public static final ResponseCode REQUEST_HEADER_FIELDS_TOO_LARGE = get(431);
    public static final ResponseCode UNAVAILABLE_FOR_LEGAL_REASONS = get(451);

    public static final ResponseCode INTERNAL_SERVER_ERROR = get(500);
    public static final ResponseCode NOT_IMPLEMENTED = get(501);
    public static final ResponseCode BAD_GATEWAY = get(502);
    public static final ResponseCode SERVICE_UNAVAILABLE = get(503);
    public static final ResponseCode GATEWAY_TIMEOUT = get(504);
    public static final ResponseCode HTTP_VERSION_NOT_SUPPORTED = get(505);
    public static final ResponseCode VARIANT_ALSO_NEGOTIATES = get(506);
    public static final ResponseCode INSUFFICIENT_STORAGE = get(507);
    public static final ResponseCode LOOP_DETECTED = get(508);
    public static final ResponseCode NOT_EXTENDED = get(510);
    public static final ResponseCode NETWORK_AUTHENTICATION_REQUIRED = get(511);


    @Range(from = 100, to = 599)
    private final int code;
    private String name;

    private ResponseCode(@Range(from = 100, to = 599) int code, String name) {
        this.code = Arguments.checkRange(code, 100, 600);
        this.name = name;
    }

    /**
     * Returns the code and name of the response code, for example "200 OK".
     *
     * @return A string representation of this response code
     */
    @Override
    public String toString() {
        return code + " " + (name != null ? name : "<unknown>");
    }

    @Override
    public Object toJson() {
        return code;
    }

    /**
     * Returns the integer code of this response code.
     *
     * @return The integer status code
     */
    @Range(from = 100, to = 599)
    public int code() {
        return code;
    }

    /**
     * Returns the all-upper name of this response code, for example "OK", or
     * <code>null</code> if this status code has no known name.
     *
     * @return The name of this code
     */
    public String name() {
        return name;
    }

    /**
     * Returns the type of status code this response code is.
     *
     * @return The type of this response code
     */
    public Type type() {
        return Type.forCode(code);
    }

    /**
     * Returns whether this response code indicates a successful connection, that is,
     * the response code is less than 400.
     *
     * @return Whether this response code indicates a successful connection
     */
    public boolean success() {
        return code < 400;
    }

    /**
     * Returns whether this response code has a known name.
     *
     * @return Whether this response code is known
     */
    public boolean isKnown() {
        return name != null;
    }


    /**
     * Returns the response code for the given response code integer.
     *
     * @param code The integer response code to get
     * @return The status code object for that code
     */
    public static ResponseCode get(@Range(from = 100, to = 599) int code) {
        return CODES[Arguments.checkRange(code, 100, 600) - 100];
    }

    /**
     * Returns the response code with the given name.
     *
     * @param name The name of the response code to find, case-insensitive
     * @return The response code with that name
     * @throws IllegalArgumentException If no response code with that name was found
     */
    @NotNull
    public static ResponseCode find(String name) {
        name = Arguments.checkNull(name, "name").toUpperCase();
        for(ResponseCode code : CODES)
            if(code.name.equals(name))
                return code;
        throw new IllegalArgumentException("Unknown response code '"+name+"'");
    }

    /**
     * Registers a name for a currently unknown response code.
     *
     * @param code The code to register a name for
     * @param name The name to set
     * @throws IllegalArgumentException If the response code with that code already
     *                                  has a name which is different, or if another
     *                                  response code already has the name to be set
     */
    public static void register(@Range(from = 100, to = 599) int code, String name) {
        Arguments.checkRange(code, 100, 600);
        name = Arguments.checkNull(name, "name").toUpperCase();
        for(ResponseCode c : CODES)
            if(c.code != code && Objects.equals(c.name, name))
                throw new IllegalArgumentException("A response code with name '"+name+"' already exists("+c.code+")");
        ResponseCode c = CODES[code - 100];
        if(c.name == null)
            c.name = name;
        else if(!c.name.equals(name))
            throw new IllegalArgumentException("Response code "+code+" already exists and has different name "+c.name+" != "+name);
    }


    /**
     * Different categories of http status codes.
     */
    public enum Type {
        /**
         * Response codes 100 - 199. Not an error, but generally the connection is not done yet.
         */
        INFORMATIONAL(100, 200),
        /**
         * Response codes 200 - 299. The request has fully completed successfully.
         */
        SUCCESS(200, 300),
        /**
         * Response codes 300 - 399. The individual request succeeded, but further requests will be necessary
         * to achieve the result intended by the initial request.
         */
        REDIRECT(300, 400),
        /**
         * Response codes 400 - 499. The server could not process the request because of an input error
         * caused by the client.
         */
        CLIENT_ERROR(400, 500),
        /**
         * Response codes 500 - 599. The server experienced an unexpected internal error and could not
         * process the request.
         */
        SERVER_ERROR(500, 600);

        /**
         * The minimum code for response codes of this type, inclusive.
         */
        public final int min;
        /**
         * The maximum code for response codes of this type, exclusive.
         */
        public final int max;

        Type(int min, int max) {
            this.min = min;
            this.max = max;
        }

        /**
         * Returns the response code type for the given integer code.
         *
         * @param code The response code to get the type of
         * @return The type of the response code
         */
        public static Type forCode(@Range(from = 100, to = 599) int code) {
            Arguments.checkRange(code, 100, 600);
            if(code < 200) return Type.INFORMATIONAL;
            if(code < 300) return Type.SUCCESS;
            if(code < 400) return Type.REDIRECT;
            if(code < 500) return Type.CLIENT_ERROR;
            return Type.SERVER_ERROR;
        }
    }
}

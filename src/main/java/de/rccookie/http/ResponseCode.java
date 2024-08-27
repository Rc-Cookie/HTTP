package de.rccookie.http;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import de.rccookie.json.Json;
import de.rccookie.json.JsonSerializable;
import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * Represents a http status code constant.
 */
public enum ResponseCode implements JsonSerializable {
    /*100*/ CONTINUE(),
    /*101*/ SWITCHING_PROTOCOLS,
    /*102*/ PROCESSING,
    /*103*/ EARLY_HINTS,
    _104, _105, _106, _107, _108, _109, _110, _111, _112, _113, _114, _115, _116, _117, _118, _119, _120, _121, _122, _123, _124, _125, _126, _127, _128, _129, _130, _131, _132, _133, _134, _135, _136, _137, _138, _139, _140, _141, _142, _143, _144, _145, _146, _147, _148, _149, _150, _151, _152, _153, _154, _155, _156, _157, _158, _159, _160, _161, _162, _163, _164, _165, _166, _167, _168, _169, _170, _171, _172, _173, _174, _175, _176, _177, _178, _179, _180, _181, _182, _183, _184, _185, _186, _187, _188, _189, _190, _191, _192, _193, _194, _195, _196, _197, _198, _199,
    /*200*/ OK("OK"),
    /*201*/ CREATED,
    /*202*/ ACCEPTED,
    /*203*/ NON_AUTHORITATIVE_INFORMATION,
    /*204*/ NO_CONTENT,
    /*205*/ RESET_CONTENT,
    /*206*/ PARTIAL_CONTENT,
    /*207*/ MULTI_STATUS("Multi-Status"),
    /*208*/ ALREADY_REPORTED,
    _209, _210, _211, _212, _213, _214, _215, _216, _217, _218, _219, _220, _221, _222, _223, _224, _225,
    /*226*/ IM_USED("IM_Used"),
    _227, _228, _229, _230, _231, _232, _233, _234, _235, _236, _237, _238, _239, _240, _241, _242, _243, _244, _245, _246, _247, _248, _249, _250, _251, _252, _253, _254, _255, _256, _257, _258, _259, _260, _261, _262, _263, _264, _265, _266, _267, _268, _269, _270, _271, _272, _273, _274, _275, _276, _277, _278, _279, _280, _281, _282, _283, _284, _285, _286, _287, _288, _289, _290, _291, _292, _293, _294, _295, _296, _297, _298, _299,
    /*300*/ MULTIPLE_CHOICES,
    /*301*/ MOVED_PERMANENTLY,
    /*302*/ FOUND,
    /*303*/ SEE_OTHER,
    /*304*/ NOT_MODIFIED,
    /*305*/ USE_PROXY,
    _306,
    /*307*/ TEMPORARY_REDIRECT,
    /*308*/ PERMANENT_REDIRECT,
    _309, _310, _311, _312, _313, _314, _315, _316, _317, _318, _319, _320, _321, _322, _323, _324, _325, _326, _327, _328, _329, _330, _331, _332, _333, _334, _335, _336, _337, _338, _339, _340, _341, _342, _343, _344, _345, _346, _347, _348, _349, _350, _351, _352, _353, _354, _355, _356, _357, _358, _359, _360, _361, _362, _363, _364, _365, _366, _367, _368, _369, _370, _371, _372, _373, _374, _375, _376, _377, _378, _379, _380, _381, _382, _383, _384, _385, _386, _387, _388, _389, _390, _391, _392, _393, _394, _395, _396, _397, _398, _399,
    /*400*/ BAD_REQUEST,
    /*401*/ UNAUTHORIZED,
    /*402*/ PAYMENT_REQUIRED,
    /*403*/ FORBIDDEN,
    /*404*/ NOT_FOUND,
    /*405*/ METHOD_NOT_ALLOWED,
    /*406*/ NOT_ACCEPTABLE,
    /*407*/ PROXY_AUTHENTICATION_REQUIRED,
    /*408*/ REQUEST_TIMEOUT,
    /*409*/ CONFLICT,
    /*410*/ GONE,
    /*411*/ LENGTH_REQUIRED,
    /*412*/ PRECONDITION_FAILED,
    /*413*/ PAYLOAD_TOO_LARGE,
    /*414*/ URI_TOO_LONG("URI Too Long"),
    /*415*/ UNSUPPORTED_MEDIA_TYPE,
    /*416*/ RANGE_NOT_SATISFIABLE,
    /*417*/ EXPECTATION_FAILED,
    /*418*/ IM_A_TEAPOT("I'm A Teapot"),
    _419, _420,
    /*421*/ MISDIRECTED_REQUEST,
    /*422*/ UNPROCESSABLE_ENTITY,
    /*423*/ LOCKED,
    /*424*/ FAILED_DEPENDENCY,
    /*425*/ TOO_EARLY,
    /*426*/ UPGRADE_REQUIRED,
    _427,
    /*428*/ PRECONDITION_REQUIRED,
    /*429*/ TOO_MANY_REQUESTS,
    _430,
    /*431*/ REQUEST_HEADER_FIELDS_TOO_LARGE,
    _432, _433, _434, _435, _436, _437, _438, _439, _440, _441, _442, _443, _444, _445, _446, _447, _448, _449, _450,
    /*451*/ UNAVAILABLE_FOR_LEGAL_REASONS,
    _452, _453, _454, _455, _456, _457, _458, _459, _460, _461, _462, _463, _464, _465, _466, _467, _468, _469, _470, _471, _472, _473, _474, _475, _476, _477, _478, _479, _480, _481, _482, _483, _484, _485, _486, _487, _488, _489, _490, _491, _492, _493, _494, _495, _496, _497, _498, _499,
    /*500*/ INTERNAL_SERVER_ERROR,
    /*501*/ NOT_IMPLEMENTED,
    /*502*/ BAD_GATEWAY,
    /*503*/ SERVICE_UNAVAILABLE,
    /*504*/ GATEWAY_TIMEOUT,
    /*505*/ HTTP_VERSION_NOT_SUPPORTED,
    /*506*/ VARIANT_ALSO_NEGOTIATES,
    /*507*/ INSUFFICIENT_STORAGE,
    /*508*/ LOOP_DETECTED,
    _509,
    /*510*/ NOT_EXTENDED,
    /*511*/ NETWORK_AUTHENTICATION_REQUIRED,
    _512, _513, _514, _515, _516, _517, _518, _519, _520, _521, _522, _523, _524, _525, _526, _527, _528, _529, _530, _531, _532, _533, _534, _535, _536, _537, _538, _539, _540, _541, _542, _543, _544, _545, _546, _547, _548, _549, _550, _551, _552, _553, _554, _555, _556, _557, _558, _559, _560, _561, _562, _563, _564, _565, _566, _567, _568, _569, _570, _571, _572, _573, _574, _575, _576, _577, _578, _579, _580, _581, _582, _583, _584, _585, _586, _587, _588, _589, _590, _591, _592, _593, _594, _595, _596, _597, _598, _599;


    static {
        Json.registerDeserializer(ResponseCode.class, json -> ResponseCode.get(json.asInt()));
    }
    private static final ResponseCode[] CODES = ResponseCode.values();


    private String name;

    ResponseCode() {
        String name = name();
        if(name.startsWith("_"))
            this.name = null;
        else {
            this.name = Arrays.stream(name.split("_"))
                    .filter(p -> !p.isEmpty())
                    .map(p -> Character.toUpperCase(p.charAt(0)) + p.substring(1).toLowerCase())
                    .collect(Collectors.joining(" "));
        }
    }

    ResponseCode(String name) {
        this.name = name;
    }

    /**
     * Returns the code and name of the response code, for example "200 OK".
     *
     * @return A string representation of this response code
     */
    @Override
    public String toString() {
        return code() + " " + (name != null ? name : "<unknown>");
    }

    @Override
    public Object toJson() {
        return code();
    }

    /**
     * Returns the integer code of this response code.
     *
     * @return The integer status code
     */
    @Range(from = 100, to = 599)
    public int code() {
        return ordinal() + 100;
    }

    /**
     * Returns the name of this response code, for example "OK" or "Not Found", or
     * <code>null</code> if this status code has no known name.
     *
     * @return The name of this code
     */
    public String httpName() {
        return name;
    }

    /**
     * Returns the type of status code this response code is.
     *
     * @return The type of this response code
     */
    public Type type() {
        return Type.forCode(code());
    }

    /**
     * Returns whether this response code indicates a successful connection, that is,
     * the response code is less than 400.
     *
     * @return Whether this response code indicates a successful connection
     */
    public boolean success() {
        return code() < 400;
    }

    /**
     * Returns whether this response code indicates that the requested task was performed
     * successfully, that is, the response code is between <code>200</code> and <code>299</code>.
     *
     * <p>This method differs from {@link #success()} in that it returns <code>false</code> for
     * <code>1XX</code> and <code>3XX</code> response codes.</p>
     *
     * @return Whether this response code indicates a successful, completed connection
     */
    public boolean ok() {
        return code() >= 200 && code() < 300;
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
        name = Arguments.checkNull(name, "name");
        for(ResponseCode code : CODES)
            if(code.name.equalsIgnoreCase(name))
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
        name = Arguments.checkNull(name, "name");
        for(ResponseCode c : CODES)
            if(c.code() != code && Objects.equals(c.name, name))
                throw new IllegalArgumentException("A response code with name '"+name+"' already exists("+c.code()+")");
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

package de.rccookie.http;

import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.rccookie.json.Json;
import de.rccookie.json.JsonSerializable;
import de.rccookie.util.Arguments;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * Represents precise MIME types and MIME type patterns (such as <code>text/*</code>).
 */
public final class ContentType implements JsonSerializable {

    private static final NumberFormat WEIGHT_FORMAT = NumberFormat.getNumberInstance(Locale.ROOT);
    static {
        WEIGHT_FORMAT.setMinimumFractionDigits(0);
        WEIGHT_FORMAT.setMaximumFractionDigits(3);
        WEIGHT_FORMAT.setRoundingMode(RoundingMode.HALF_UP);
        Json.registerDeserializer(ContentType.class, json -> ContentType.of(json.toString()));
    }

    private static final Map<String, ContentType> REMAPPING = new HashMap<>();


    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();
    private static final Map<String, ContentType> SUFFIX_TO_TYPE = new HashMap<>();
    private static final Map<ContentType, Set<String>> TYPE_TO_SUFFIX = new HashMap<>();



    public static final ContentType ANY = register("*/*", "Anything");
    public static final ContentType APPLICATION = register("application/*", "Any binary data that does not fit any other category");
    public static final ContentType AUDIO = register("audio/*", "Audio or music data");
    public static final ContentType FONT = register("font/*", "Font data");
    public static final ContentType IMAGE = register("image/*", "Graphics (including animated)");
    public static final ContentType MODEL = register("model/*", "3d geometry or scene data");
    public static final ContentType ANY_TEXT = register("text/*", "Text-only human-readable data");
    public static final ContentType VIDEO = register("video/*", "Video data or files");


    public static final ContentType AAC_AUDIO = register("audio/aac", "ACC audio data", "aac");
    public static final ContentType ABIWORD_DOCUMENT = register("application/x-abiword", "AbiWord document", "abw");
    public static final ContentType ARCHIVE = register("application/x-freearc", "Archive (multiple embedded files)", "arc");
    public static final ContentType AVIF = register("image/avif", "AVIF image", "avif");
    public static final ContentType AVI = register("video/x-msvideo", "Audio Video Interleave", "avi");
    public static final ContentType KINDLE_E_BOOK = register("application/vnd.amazon.ebook", "Amazon Kindle eBook", "azw");
    public static final ContentType BINARY = register("application/octet-stream", "Arbitrary binary data", "bin", "dll");
    public static final ContentType BITMAP = register("image/bmp", "Windows bitmap image", "bmp");
    public static final ContentType BZIP = register("application/x-bzip", "BZip archive", "bz");
    public static final ContentType BZIP2 = register("application/x-bzip2", "BZip2 archive", "bz2");
    public static final ContentType CD_AUDIO = register("application/x-cdf", "CD audio", "cda");
    public static final ContentType C_SHELL_SCRIPT = register("application/x-csh", "C-Shell script", "csh");
    public static final ContentType CSS = register("text/css", "CSS stylesheet", "css");
    public static final ContentType CSV = register("text/csv", "CSV table", "csv");
    public static final ContentType DLL = BINARY;
    public static final ContentType EXE = register("application/x-msdownload", "Windows executable", "exe");
    public static final ContentType MS_WORD = register("application/msword", "Microsoft Word document", "doc");
    public static final ContentType MS_WORD_XML = register("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "OpenXML Microsoft Word document", "docx");
    public static final ContentType MS_OPENTYPE_FONT = register("application/vnd.ms-fontobject", "Microsoft OpenType fonts", "eot");
    public static final ContentType E_PUBLICATION = register("application/epub+zip", "Electronic publication", "epub");
    public static final ContentType GZIP = register("application/gzip", "GZip archive", "gz");
    public static final ContentType GIF = register("image/gif", "GIF image", "gif");
    public static final ContentType HTML = register("text/html", "HTML document", "html", "htm");
    public static final ContentType ICON = register("image/vnd.microsoft.icon", "Icon image", "ico", "icon");
    public static final ContentType I_CALENDAR = register("text/calendar", "iCalendar file", "ics");
    public static final ContentType JAR = register("application/java-archive", "Jar archive", "jar");
    public static final ContentType JPG = register("image/jpeg", "JPG image", "jpg", "jpeg");
    public static final ContentType JAVA = register("application/java", "Java", "java", "jav");
    public static final ContentType JAVASCRIPT = register("text/javascript", "JavaScript", "js");
    public static final ContentType JAVASCRIPT_MODULE = JAVASCRIPT;
    public static final ContentType TYPESCRIPT = register("text/x-typescript", "TypeScript", "ts");
    public static final ContentType JSON = register("application/json", "Json", "json");
    public static final ContentType JSON_LD = register("application/ld+json", "Json-LD", "jsonld");
    public static final ContentType MARKDOWN = register("text/markdown", "Markdown document", "md");
    public static final ContentType MIDI_X = register("audio/x-midi", "Musical Instrument Digital Interface (MIDI)", "mid", "midi");
    public static final ContentType MIDI = register("audio/midi", "Musical Instrument Digital Interface (MIDI)", "mid", "midi"); // Override x-midi suffix lookup
    public static final ContentType MP3 = register("audio/mpeg", "MP3 audio", "mp3");
    public static final ContentType MP4 = register("video/mp4", "MP4 video", "mp4");
    public static final ContentType MPEG = register("video/mpeg", "MPEG video", "mpeg");
    public static final ContentType MPKG = register("application/vnd.apple.installer+xml", "Apple installer package", "apk");
    public static final ContentType OPENDOCUMENT_PRESENTATION = register("application/vnd.oasis.opendocument.presentation", "OpenDocument presentation", "odp");
    public static final ContentType OPENDOCUMENT_SPREADSHEET = register("application/vnd.oasis.opendocument.spreadsheet", "OpenDocument spreadsheet", "ods");
    public static final ContentType OPENDOCUMENT_TEXT = register("application/vnd.oasis.opendocument.text", "OpenDocument text document", "odt");
    public static final ContentType OGG_AUDIO = register("audio/ogg", "OGG audio", "oga");
    public static final ContentType OGG_VIDEO = register("video/ogg", "OGG video", "ogv");
    public static final ContentType OGG = register("application/ogg", "OGG data", "ogx");
    public static final ContentType OPUS_AUDIO = register("audio/opus", "Opus audio", "opus");
    public static final ContentType OPENTYPE_FONT = register("font/otf", "OpenType font", "otf");
    public static final ContentType PNG = register("image/png", "PNG image", "png");
    public static final ContentType PDF = register("application/pdf", "PDF document", "pdf");
    public static final ContentType PHP = register("application/x-httpd-php", "PHP", "php");
    public static final ContentType POWERPOINT = register("application/vnd.ms-powerpoint", "Powerpoint presentation", "ppt");
    public static final ContentType POWERPOINT_XML = register("application/vnd.openxmlformats-officedocument.presentationml.presentation", "OpenXML Powerpoint presentation", "pptx");
    public static final ContentType RAR = register("application/vnd.rar", "RAR archive", "rar");
    public static final ContentType RICH_TEXT = register("application/rtf", "Rich text (RTF)", "rtf");
    public static final ContentType BOURNE_SHELL_SCRIPT = register("application/x-sh", "Bourne shell script", "sh");
    public static final ContentType SVG = register("image/svg+xml", "SVG image", "svg");
    public static final ContentType TAR = register("application/x-tar", "TAR archive", "tar");
    public static final ContentType TIFF = register("application/tiff", "TIFF image", "tif", "tiff");
    public static final ContentType MPEG_STREAM = register("video/mp2t", "MPEG video stream");
    public static final ContentType TRUE_TYPE_FONT = register("font/ttf", "TrueType font", "ttf");
    public static final ContentType PLAINTEXT = register("text/plain", "Plaintext", "txt", "text");
    public static final ContentType MS_VISIO = register("application/vnd.visio", "Microsoft Visio", "vsd");
    public static final ContentType WAV = register("audio/wav", "WAV audio", "wav");
    public static final ContentType W_BITMAP = register("image/vnd.wap.wbmp", "Wireless Application Bitmap Format", "wbmp");
    public static final ContentType WEBM_AUDIO = register("audio/webm", "WEBM audio", "weba");
    public static final ContentType WEBM_VIDEO = register("video/webm", "WEBM video", "webm");
    public static final ContentType WEBP_IMAGE = register("image/webp", "WEBM image", "webp");
    public static final ContentType OPEN_FONT_FORMAT = register("font/woff", "Open Font Format", "woff");
    public static final ContentType OPEN_FONT_FORMAT_2 = register("font/woff2", "Open Font Format", "woff2");
    public static final ContentType XHTML = register("application/xhtml+xml", "XHTML document", "xhtml");
    public static final ContentType EXCEL = register("application/vnd.ms-excel", "Excel document", "xls");
    public static final ContentType EXCEL_XML = register("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "OpenXML Excel document", "xlsx");
    public static final ContentType XML = register("application/xml", "XML", "xml");
    public static final ContentType XUL = register("application/vnd.mozilla.xul+xml", "XUL", "xul");
    public static final ContentType ZIP = register("application/zip", "Zip archive", "zip");
    public static final ContentType _3GPP_AUDIO = register("audio/3gpp", "3GPP audio container", "3gp");
    public static final ContentType _3GPP_VIDEO = register("video/3gpp", "3GPP video container", "3gp");
    public static final ContentType _3GPP_AUDIO2 = register("audio/3gpp2", "3GPP2 audio container", "3g2");
    public static final ContentType _3GPP_VIDEO2 = register("video/3gpp2", "3GPP2 video container", "3g2");
    public static final ContentType _7_ZIP = register("application/x-7z-compressed", "7-Zip archive", "7z");

    public static final ContentType URL_ENCODED = register("application/x-www-form-urlencoded", "URL parameter encoding");
    public static final ContentType MULTIPART = register("multipart/form-data", "Multiple data types");

    static {
        register("text/xml", XML.description(), XML.suffixes().toArray(new String[0]));
        register("application/xml", XML.description(), XML.suffixes().toArray(new String[0])); // Set in lookup
        REMAPPING.put("text/xml", XML);
        SUFFIX_TO_TYPE.put("vscode", ContentType.JSON);
        SUFFIX_TO_TYPE.put("gitignore", ContentType.PLAINTEXT);
    }

    private final String type;
    private final String subtype;
    private final Map<String, String> params;

    private ContentType(String mimeStr) {
        Arguments.checkNull(mimeStr, "mimeStr");

        int semi = mimeStr.indexOf(';');
        String typeStr = semi == -1 ? mimeStr : mimeStr.substring(0, semi).stripTrailing();

        if(typeStr.equals("*") || typeStr.equals("/")) // Used by java itself :/
            this.type = this.subtype = "*";
        else {
            int slash = typeStr.indexOf('/');
            if(slash == -1) throw new IllegalArgumentException("Illegal mime type syntax: '/' expected: " + typeStr);
            this.type = typeStr.substring(0, slash);
            this.subtype = typeStr.substring(slash + 1);
        }

        if(semi == -1) params = Map.of();
        else {
            Map<String, String> params = new HashMap<>();
            String str = mimeStr;
            do {
                str = str.substring(semi+1);

                int eq = str.indexOf('=');
                if(eq == -1) throw new IllegalArgumentException("Illegal mime type syntax: '=' expected in params: "+mimeStr);
                String k = str.substring(0, eq).trim();

                semi = str.indexOf(';', eq);
                String v = str.substring(eq+1, semi==-1 ? str.length() : semi).trim();

                params.put(k,v);
            } while(semi != -1);
            this.params = Utils.view(params);
        }
        validate();
    }

    private ContentType(String type, String subtype, Map<? extends String, ? extends String> params) {
        Arguments.checkNull(type, "type");
        Arguments.checkNull(subtype, "subtype");
        ContentType remapped = REMAPPING.get(type+'/'+subtype);
        if(remapped != null) {
            this.type = remapped.type;
            this.subtype = remapped.subtype;
        }
        else {
            this.type = type;
            this.subtype = subtype;
        }
        this.params = params == null ? Map.of() : Map.copyOf(params);
        validate();
    }

    private void validate() {
        validate(type);
        validate(subtype);

        if(type.isEmpty()) throw new IllegalArgumentException("Illegal mime type syntax: main type missing: "+this);
        if(subtype.isEmpty()) throw new IllegalArgumentException("Illegal mime type syntax: subtype missing: "+this);

        if(type.equals("*") && !subtype.equals("*"))
            throw new IllegalArgumentException("Illegal mime type: subtype must be wildcard if main type is wildcard: "+this);

        params.forEach((k,v) -> {
            validate(k);
            validate(v);
        });
    }

    private void validate(String value) {
        if(value.contains(" ") || value.contains(",") || value.contains(";") || value.contains("\n") || value.contains("\r"))
            throw new IllegalArgumentException("Illegal character in mime type: "+this);
    }

    /**
     * Returns the mime type string, including parameters.
     *
     * @return This mime type as string, e.g. <code>"text/html;charset=UTF-8"</code>
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(type).append('/').append(subtype);
        params.forEach((k,v) -> str.append(';').append(k).append('=').append(v));
        return str.toString();
    }

    /**
     * Returns the mime type's string without parameters, i.e. <code>[type]/[subtype]</code>.
     *
     * @return This mime type as string without parameters, e.g. <code>"text/html"</code>
     */
    public String toTypeString() {
        return type+'/'+subtype;
    }

    /**
     * Tests for equality. Parameters are ignored.
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ContentType && ((ContentType) obj).type.equals(type) && ((ContentType) obj).subtype.equals(subtype));
    }

    @Override
    public int hashCode() {
        return type.hashCode() ^ subtype.hashCode();
    }

    @Override
    public Object toJson() {
        return toString();
    }

    /**
     * Returns the major type of this mime type, or <code>"*"</code> for any. If the major
     * type of the mime type is the wildcard <code>"*"</code>, the subtype must be, too.
     *
     * @return The major type of this mime type
     */
    public String type() {
        return type;
    }

    /**
     * Returns the minor type of this mime type, or <code>"*"</code> for any.
     *
     * @return The minor type of this mime type
     */
    public String subtype() {
        return subtype;
    }

    /**
     * Returns the parameters of this mime type, if any. If no parameters are set, an empty map
     * is returned.
     *
     * @return The parameters of this mime type
     */
    public Map<String, String> params() {
        return params;
    }

    /**
     * Returns the weight of this content type, that is, the value of the <code>"q"</code> parameter,
     * or <code>1</code>.
     *
     * @return The weight of this content type, a value between 0 and 1
     */
    @Range(from = 0, to = 1)
    public double weight() {
        return Arguments.checkInclusive(Double.parseDouble(params.getOrDefault("q", "1")), 0.0, 1.0);
    }

    /**
     * Returns the charset of this content type specified by the <code>"charset"</code> parameter,
     * or <code>UTF-8</code> if that parameter is not present.
     *
     * @return The charset for this content type
     */
    @NotNull
    public Charset charset() {
        if(params.containsKey("charset"))
            return Charset.forName(params.get("charset"));
        return StandardCharsets.UTF_8;
    }

    /**
     * Returns a descriptive name for this mime type. If no description is known for this mime
     * type, the mime type string itself will be returned.
     *
     * @return A description for this mime type
     */
    public String description() {
        return DESCRIPTIONS.get(toTypeString());
    }

    /**
     * Returns the suffixes known to belong to this content type (which might be none).
     *
     * @return The known file extensions (without leading dot) for this content type
     */
    @NotNull
    public Set<String> suffixes() {
        return TYPE_TO_SUFFIX.getOrDefault(this, Set.of());
    }

    /**
     * Returns the most common file extension for this content type, if known, otherwise
     * <code>null</code>.
     *
     * @return The most common file extension, identical to the first entry returned in
     *         <code>suffixes()</code>
     */
    public String extension() {
        return suffixes().stream().findFirst().orElse(null);
    }

    /**
     * Returns whether this mime type is the "any type" pattern <code>"*&#47;*"</code>.
     *
     * @return Whether this pattern is the wildcard mime type
     */
    public boolean isAny() {
        return type.equals("*");
    }

    /**
     * Returns whether this mime type represents a category pattern, that is, its subtype is
     * <code>"*"</code> but its major type is not a wildcard.
     *
     * @return Whether this mime type is a type category
     */
    public boolean isCategory() {
        return !isAny() && !isPrecise();
    }

    /**
     * Returns whether this mime type is a concrete type, or is a pattern like <code>text/*</code>.
     *
     * @return Whether this mime type is a concrete type
     */
    public boolean isPrecise() {
        return !subtype.equals("*");
    }

    /**
     * Returns whether this mime type is a subtype of the given type, that is, it is the
     * same type as the given type, or the given type is a mime type pattern which matches
     * this type. The parameters of both types are ignored. This is the reverse of
     * {@link #contains(ContentType)}.
     *
     * @param type The type to test whether this type is a specification of
     * @return Whether this type matches the given type
     */
    public boolean matches(ContentType type) {
        return Arguments.checkNull(type, "type").contains(this);
    }

    /**
     * Returns whether the given mime type is a subtype of this type, that is, it is the
     * same type as this type, or this type is a mime type pattern which matches the
     * given type. The parameters of both types are ignored. This is the reverse of
     * {@link #matches(ContentType)}.
     *
     * @param type The type to test for containment
     * @return Whether the given type is a specification of this type
     */
    public boolean contains(ContentType type) {
        Arguments.checkNull(type, "type");
        if(equals(type) || isAny()) return true;
        return this.type.equals(type.type) && (subtype.equals("*") || subtype.equals(type.subtype));
    }

    /**
     * Returns an equivalent mime type without any parameters.
     *
     * @return A mime type without parameters, with the same major and minor type
     */
    public ContentType withoutParams() {
        return params.isEmpty() ? this : new ContentType(type, subtype, null);
    }

    /**
     * Returns an equivalent mime type with the specified parameters. The current
     * parameters are ignored.
     *
     * @param params The parameters to set
     * @return A mime type with the specified parameters, with the same major and minor type as this one
     */
    public ContentType withParams(@NotNull Map<? extends String, ? extends String> params) {
        return this.params.equals(Arguments.checkNull(params, "params")) ? this :
                new ContentType(type, subtype, params);
    }

    /**
     * Returns an equivalent mime type with the <code>"q"</code> parameter set to the given weight
     * between 0 and 1. All other parameters will be copied over.
     *
     * @param weight The weight to set, a number between 0 and 1
     * @return A mime type identical to this one except the value of the <code>"q"</code> parameter
     */
    public ContentType withWeight(double weight) {
        return withParam("q", WEIGHT_FORMAT.format(Arguments.checkRange(weight, 0.0, 1.0)));
    }

    /**
     * Returns an equivalent mime type with the <code>"charset"</code> parameter set to the given charset,
     * or removes the parameter if the charset is <code>null</code>.
     *
     * @param charset The charset to set, or <code>null</code> to remove the parameter
     * @return A mime type identical to this one except the value of the <code>"charset"</code> parameter
     */
    public ContentType withCharset(@Nullable Charset charset) {
        return withParam("charset", charset != null ? charset.name() : null);
    }

    /**
     * Returns an equivalent mime type with the <code>"charset"</code> parameter set to the given charset,
     * or removes the parameter if the charset is <code>null</code>.
     *
     * @param charset The charset to set, or <code>null</code> to remove the parameter
     * @return A mime type identical to this one except the value of the <code>"charset"</code> parameter
     */
    public ContentType withCharset(@Nullable String charset) {
        return withParam("charset", charset);
    }

    /**
     * Returns an equivalent mime type with the specified parameter set to the given value,
     * or removed if the value to set is <code>null</code>. All other parameters will be
     * copied over.
     *
     * @param name The name of the parameter to set or remove
     * @param value The value for the parameter to set, or <code>null</code> to remove the parameter
     * @return A mime type identical to this one except the value of the specified parameter
     */
    public ContentType withParam(@NotNull String name, @Nullable String value) {
        Arguments.checkNull(name, "name");
        if(params.isEmpty())
            return value != null ? new ContentType(type, subtype, Map.of(name, value)) : this;

        Map<String, String> params = new HashMap<>(this.params);
        if(value != null) params.put(name, value);
        else if(params.remove(name) == null) return this;

        return new ContentType(type, subtype, params);
    }

    /**
     * Returns the parent mime type pattern, or <code>null</code> if this type already represents
     * <code>"*&#47;*"</code>. The parameters of this type are ignored, the returned type will
     * have no parameters.
     *
     * @return The parent mime type
     */
    public ContentType parent() {
        if(isAny()) return null;
        if(!isPrecise()) return ANY;
        return new ContentType(type, "*", null);
    }

    /**
     * Returns the category mime type pattern of this type. If this mime type already is a category,
     * it will be returned itself. If this is the wildcard pattern <code>"*&#47;*"</code>, it will also
     * be returned itself.
     *
     * @return The category of this mime type
     */
    public ContentType category() {
        return isPrecise() ? parent() : this;
    }


    /**
     * Parses the given mime string, including any parameters.
     *
     * @param mimeTypeStr The mime string to parse
     * @return The content type parsed from the string
     */
    @NotNull
    public static ContentType of(String mimeTypeStr) {
        return new ContentType(mimeTypeStr);
    }

    /**
     * Returns a content type object for the given mime pattern.
     *
     * @param type The major type value, or <code>"*"</code> (if the wildcard is used, the
     *             subtype must also be the wildcard)
     * @param subtype The minor type value, or <code>"*"</code>
     * @param params Any parameters for the content type, or null for none
     * @return A content type representing the specified mime type
     */
    public static ContentType of(String type, String subtype, @Nullable Map<? extends String, ? extends String> params) {
        return new ContentType(type, subtype, params);
    }

    /**
     * Returns a content type object for the given mime pattern, with a single parameter.
     *
     * @param type The major type value, or <code>"*"</code> (if the wildcard is used, the
     *             subtype must also be the wildcard)
     * @param subtype The minor type value, or <code>"*"</code>
     * @param paramName The name of the single parameter
     * @param paramValue The value of the single parameter
     * @return A content type representing the specified mime type
     */
    public static ContentType of(String type, String subtype, String paramName, String paramValue) {
        return of(type, subtype, Map.of(paramName, paramValue));
    }

    /**
     * Returns a content type object for the given mime pattern, without parameters.
     *
     * @param type The major type value, or <code>"*"</code> (if the wildcard is used, the
     *             subtype must also be the wildcard)
     * @param subtype The minor type value, or <code>"*"</code>
     * @return A content type representing the specified mime type
     */
    public static ContentType of(String type, String subtype) {
        return of(type, subtype, null);
    }

    /**
     * Registers description and/or file suffixes for the given content type.
     *
     * @param mimeTypeStr The mime string to register, parameters are ignored
     * @param description The description to set for the type, if any
     * @param suffixes The file extensions to register to this content type
     * @return The parsed content type (with parameters)
     */
    @NotNull
    public static ContentType register(String mimeTypeStr, String description, String... suffixes) {
        Arguments.checkNull(suffixes, "suffixes");

        ContentType type = of(mimeTypeStr);
        if(suffixes.length != 0 && !type.isPrecise())
            throw new IllegalArgumentException("Cannot set suffix for mime pattern");

        String typeStr = type.toTypeString();
        if(description != null)
            DESCRIPTIONS.put(typeStr, description);

        for(String suffix : suffixes)
            SUFFIX_TO_TYPE.put(suffix.toLowerCase(), type);
        TYPE_TO_SUFFIX.put(type, suffixes.length != 0 ? Arrays.stream(suffixes).map(String::toLowerCase).collect(Collectors.toCollection(LinkedHashSet::new)) : null);
        return type;
    }

    /**
     * Returns a content type guess for a file with the given name, or <code>null</code>
     * if no good guess could be made.
     *
     * @param filename The name of the file
     * @return The best guess of the content type, if any
     */
    @Nullable
    public static ContentType guessFromName(String filename) {
        if(Arguments.checkNull(filename, "filename").equals("index"))
            return HTML;

        int dot = filename.lastIndexOf('.');
        if(dot == -1 || dot == filename.length()-1) return null;

        String suffix = filename.substring(dot+1).toLowerCase();
        return SUFFIX_TO_TYPE.get(suffix);
    }

    /**
     * Returns <code>multipart/form-data; boundary=[boundary]</code>.
     *
     * @param boundary The boundary to use
     * @return A multipart content type with the given boundary
     */
    public static ContentType multipart(String boundary) {
        return MULTIPART.withParam("boundary", boundary);
    }
}

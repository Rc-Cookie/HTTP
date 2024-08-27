package de.rccookie.http.server;

import java.lang.reflect.Type;

import de.rccookie.http.Body;
import de.rccookie.http.ContentType;
import de.rccookie.http.ContentTypes;
import de.rccookie.http.HttpRequest;
import de.rccookie.json.IllegalJsonDeserializerException;
import de.rccookie.json.JsonDeserializationException;
import de.rccookie.json.JsonDeserializer;
import de.rccookie.json.JsonElement;
import de.rccookie.json.JsonParseException;
import de.rccookie.json.MissingFieldException;
import de.rccookie.json.TypeMismatchException;
import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;

public interface Parser {

    <T> T parse(@NotNull Body data, @NotNull Type targetType, @NotNull HttpRequest request);

    ContentTypes getMIMETypes();

    boolean supportsUnknownMIMEType();



    class Default implements Parser {

        static final Default INSTANCE = new Default();

        private final Parser.Json jsonParser;
        private final Parser.URLParams paramsParser;
//        private final Parser.Multipart multipartParser;

        public Default(Parser.Json jsonParser, Parser.URLParams paramsParser/*, Parser.Multipart multipartParser*/) {
            this.jsonParser = Arguments.checkNull(jsonParser, "jsonParser");
            this.paramsParser = Arguments.checkNull(paramsParser, "paramsParser");
//            this.multipartParser = Arguments.checkNull(multipartParser, "multipartParser");
        }

        public Default() {
            this(new Parser.Json(), new Parser.URLParams());//, new Parser.Multipart());
//            multipartParser.contentParser = this;
        }

        @Override
        public <T> T parse(@NotNull Body data, @NotNull Type targetType, @NotNull HttpRequest request) {
            if(request.contentType().equals(ContentType.JSON))
                return jsonParser.parse(data, targetType, request);
            else return paramsParser.parse(data, targetType, request);
        }

        @Override
        public ContentTypes getMIMETypes() {
            return ContentTypes.of(ContentType.JSON, ContentType.URL_ENCODED);
        }

        @Override
        public boolean supportsUnknownMIMEType() {
            return false;
        }
    }

    class Json implements Parser {

        private final JsonDeserializer deserializer;

        public Json(JsonDeserializer deserializer) {
            this.deserializer = deserializer;
        }

        public Json() {
            this(JsonDeserializer.DEFAULT);
        }

        @Override
        public <T> T parse(@NotNull Body data, @NotNull Type targetType, @NotNull HttpRequest request) {
            try {
                return de.rccookie.json.Json.parse(data.stream()).withDeserializer(deserializer).as(targetType);
            } catch(JsonParseException e) {
                throw HttpRequestFailure.badRequest("Illegal JSON string", e.getMessage(), e);
            } catch(TypeMismatchException | MissingFieldException e) {
                throw HttpRequestFailure.badRequest(e.getMessage(), null, e);
            } catch(JsonDeserializationException e) {
                throw HttpRequestFailure.badRequest("Malformed JSON data", e.getMessage(), e);
            } catch(IllegalJsonDeserializerException e) {
                throw HttpRequestFailure.internal(e);
            }
        }

        @Override
        public ContentTypes getMIMETypes() {
            return ContentTypes.of(ContentType.JSON);
        }

        @Override
        public boolean supportsUnknownMIMEType() {
            return true;
        }
    }

    class BestEffortJson extends Json {
        public BestEffortJson() {
            super(JsonDeserializer.BEST_EFFORT);
        }
    }

    class StrictJson extends Json {
        public StrictJson() {
            super(JsonDeserializer.STRICT);
        }

        @Override
        public boolean supportsUnknownMIMEType() {
            return false;
        }
    }

    class JsonString implements Parser {

        private final JsonDeserializer deserializer;

        public JsonString(JsonDeserializer deserializer) {
            this.deserializer = Arguments.checkNull(deserializer, "deserializer");
        }

        public JsonString() {
            this(JsonDeserializer.DEFAULT);
        }

        @Override
        public <T> T parse(@NotNull Body data, @NotNull Type targetType, @NotNull HttpRequest request) {
            try {
                return JsonElement.wrap(data.text()).withDeserializer(deserializer).as(targetType);
            } catch(JsonParseException e) {
                throw HttpRequestFailure.badRequest("Illegal JSON string", e.getMessage(), e);
            } catch(TypeMismatchException | MissingFieldException e) {
                throw HttpRequestFailure.badRequest(e.getMessage(), null, e);
            } catch(JsonDeserializationException e) {
                throw HttpRequestFailure.badRequest("Malformed JSON data", e.getMessage(), e);
            } catch(IllegalJsonDeserializerException e) {
                throw HttpRequestFailure.internal(e);
            }
        }

        @Override
        public ContentTypes getMIMETypes() {
            return ContentTypes.of(ContentType.ANY);
        }

        @Override
        public boolean supportsUnknownMIMEType() {
            return true;
        }
    }

    class URLParams implements Parser {

        private final JsonDeserializer jsonDeserializer;

        public URLParams(JsonDeserializer jsonDeserializer) {
            this.jsonDeserializer = Arguments.checkNull(jsonDeserializer, "jsonDeserializer");
        }

        public URLParams() {
            this(JsonDeserializer.STRING_CONVERSION);
        }

        @Override
        public <T> T parse(@NotNull Body data, @NotNull Type targetType, @NotNull HttpRequest request) {
            try {
                return JsonElement.wrap(data.params()).withDeserializer(jsonDeserializer).as(targetType);
            } catch(TypeMismatchException | MissingFieldException e) {
                throw HttpRequestFailure.badRequest(e.getMessage(), null, e);
            } catch(JsonDeserializationException e) {
                throw HttpRequestFailure.badRequest("Malformed form data", e.getMessage(), e);
            } catch(IllegalJsonDeserializerException e) {
                throw HttpRequestFailure.internal(e);
            }
        }

        @Override
        public ContentTypes getMIMETypes() {
            return ContentTypes.of(ContentType.URL_ENCODED);
        }

        @Override
        public boolean supportsUnknownMIMEType() {
            return true;
        }
    }

//    class Multipart implements Parser {
//
//        private Parser contentParser;
//
//        public Multipart(Parser contentParser) {
//            this.contentParser = Arguments.checkNull(contentParser, "contentParser");
//        }
//
//        private Multipart() { }
//
//        @Override
//        public <T> T parse(@NotNull Body data, @NotNull Type targetType, @NotNull HttpRequest request) {
//            Body.Multipart multipart = Body.Multipart.parse(data);
//            JsonObject json = new JsonObject();
//            for(String name : multipart.parts().keySet())
//                json.put(name, name);
//            return JsonElement.wrap(json, new JsonDeserializer(true) {
//
//            }).as(targetType);
//        }
//
//        @Override
//        public ContentTypes getMIMETypes() {
//            return ContentTypes.of(ContentType.MULTIPART);
//        }
//
//        @Override
//        public boolean supportsUnknownMIMEType() {
//            return false;
//        }
//    }
}

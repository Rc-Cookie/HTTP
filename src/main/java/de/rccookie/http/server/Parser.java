package de.rccookie.http.server;

import de.rccookie.http.ContentType;
import de.rccookie.http.ContentTypes;
import de.rccookie.http.HttpRequest;
import de.rccookie.json.IllegalJsonDeserializerException;
import org.jetbrains.annotations.NotNull;

public interface Parser {

    <T> T parse(@NotNull HttpRequest request, @NotNull Class<T> targetType);

    ContentTypes getMIMETypes();

    boolean supportsUnknownMIMEType();



    class Json implements Parser {

        @Override
        public <T> T parse(@NotNull HttpRequest request, @NotNull Class<T> targetType) {
            try {
                return de.rccookie.json.Json.parse(request.body().stream()).as(targetType);
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
}

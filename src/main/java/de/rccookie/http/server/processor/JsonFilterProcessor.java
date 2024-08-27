package de.rccookie.http.server.processor;

import java.util.Objects;

import de.rccookie.http.Body;
import de.rccookie.http.ContentType;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.server.HttpProcessor;
import de.rccookie.http.server.HttpRequestFailure;
import de.rccookie.json.Json;
import de.rccookie.json.JsonElement;
import de.rccookie.json.JsonObject;
import de.rccookie.json.JsonParseException;
import de.rccookie.util.Console;

public class JsonFilterProcessor implements HttpProcessor {

    private final String fieldName;
    private final boolean fromHeader;
    private final boolean errorOnMissing;
    private final boolean quiet;

    public JsonFilterProcessor(String fieldName, boolean fromHeader, boolean errorOnMissing, boolean quiet) {
        this.errorOnMissing = errorOnMissing;
        this.quiet = quiet;
        this.fieldName = fieldName == null || fieldName.isBlank() ? fromHeader ? "X-Filter" : "filter" : fieldName;
        this.fromHeader = fromHeader;
    }

    public JsonFilterProcessor(boolean quiet) {
        this("", false, false, quiet);
    }

    @SuppressWarnings("unused")
    private JsonFilterProcessor(JsonFilter config) {
        this(config.fieldName(), config.fromHeader(), config.errorOnMissing(), config.quiet());
    }

    @Override
    public void preprocess(HttpRequest.Received request) {
        String json = fromHeader ? request.headerField(fieldName) : request.query().get(fieldName);
        if(json == null)
            request.bindOptionalParam(Filter.class, null);
        else {
            try {
                JsonElement filter = Json.parse(json);
                request.bindOptionalParam(Filter.class, new Filter(filter));
            } catch(JsonParseException e) {
                throw HttpRequestFailure.badRequest("Invalid filter JSON: "+e.getMessage(), null, e);
            }
        }
    }

    @Override
    public void postprocess(HttpResponse.Editable response) throws Exception {
        Filter filter = response.request().getOptionalParam(Filter.class);
        if(filter == null || !(filter.filter.get() instanceof JsonObject))
            return;

        ContentType type = response.contentType();
        if(type == null)
            return;
        if(!type.equals(ContentType.JSON)) {
            if(!quiet)
                Console.warn("Json filter not supported for", type);
            return;
        }

        JsonElement json;
        try(Body body = response.body()) {
            json = body.json();
        }
        try {
            response.setBody(Body.ofJson(json.isStructure() ? json.asStructure().filter(filter.filter.get(), !errorOnMissing) : json));
        } catch(IllegalArgumentException e) {
            if(errorOnMissing) {
                response.request().invalidateResponse();
                throw HttpRequestFailure.badRequest(e.getMessage());
            }
            throw e;
        }
    }

    public static final class Filter {
        public final JsonElement filter;

        public Filter(JsonElement filter) {
            this.filter = filter;
        }

        @Override
        public String toString() {
            return "Filter[" + filter + "]";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Filter && Objects.equals(filter, ((Filter) obj).filter);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(filter);
        }
    }
}

package de.rccookie.http.server.processor;

import java.util.Objects;
import java.util.function.Function;

import de.rccookie.http.Body;
import de.rccookie.http.ContentType;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.http.server.HttpProcessor;
import de.rccookie.http.server.HttpRequestFailure;
import de.rccookie.json.JsonElement;
import de.rccookie.json.JsonObject;
import de.rccookie.util.Arguments;
import de.rccookie.util.Console;
import org.jetbrains.annotations.Nullable;

public class Pagination implements HttpProcessor {

    private final long defaultLimit;
    private final long maxLimit;
    private final Function<HttpRequest, String> limitGetter;
    private final Function<HttpRequest, String> offsetGetter;
    @Nullable
    private final String totalHeaderName;
    private final boolean parseOnly;
    private final boolean quiet;

    public Pagination(long defaultLimit, long maxLimit, String limitName, String offsetName, @Nullable String totalHeaderName, boolean fromHeader, boolean parseOnly, boolean quiet) {
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
        this.quiet = quiet;
        Arguments.checkNull(limitName, "limitName");
        Arguments.checkNull(offsetName, "offsetName");
        this.limitGetter = fromHeader ? r -> r.headerField(limitName.equals("limit") ? "X-Limit" : limitName) : r -> r.query().get(limitName);
        this.offsetGetter = fromHeader ? r -> r.headerField(offsetName.equals("offset") ? "X-Offset" : offsetName) : r -> r.query().get(offsetName);
        this.totalHeaderName = totalHeaderName;
        this.parseOnly = parseOnly;
    }

    public Pagination(boolean quiet) {
        this(0, Long.MAX_VALUE, "limit", "offset", "X-Total-Count", false, false, quiet);
    }

    @SuppressWarnings("unused")
    private Pagination(Paging config) {
        this(config.defaultLimit(), config.maxLimit(), config.limitName(), config.offsetName(), config.totalHeaderName(), config.fromHeader(), config.parseOnly(), config.quiet());
    }


    @Override
    public void preprocess(HttpRequest.Received request) {

        String limitStr = limitGetter.apply(request);
        String offsetStr = offsetGetter.apply(request);

        long limit, offset;
        if(limitStr != null) {
            try {
                limit = Long.parseLong(limitStr);
                if(limit <= 0)
                    throw new HttpRequestFailure(ResponseCode.RANGE_NOT_SATISFIABLE, "Non-positive limit");
                if(limit > maxLimit)
                    throw new HttpRequestFailure(ResponseCode.RANGE_NOT_SATISFIABLE, "Limit too large");
            } catch(NumberFormatException e) {
                throw new HttpRequestFailure(ResponseCode.RANGE_NOT_SATISFIABLE, "Invalid limit: "+limitStr, e);
            }
        }
        else limit = defaultLimit <= 0 ? maxLimit : defaultLimit;

        if(offsetStr != null) {
            try {
                offset = Long.parseLong(offsetStr);
                if(offset < 0)
                    throw new HttpRequestFailure(ResponseCode.RANGE_NOT_SATISFIABLE, "Negative offset");
            } catch(NumberFormatException e) {
                throw new HttpRequestFailure(ResponseCode.RANGE_NOT_SATISFIABLE, "Invalid offset: "+offsetStr, e);
            }
        }
        else offset = 0;

        request.bindOptionalParam(Range.class, new Range(offset, limit));
    }

    @Override
    public void postprocess(HttpResponse.Editable response) throws Exception {
        if(parseOnly)
            return;

        Range range = response.request().getOptionalParam(Range.class);
        if(range.offset == 0 && range.limit == Long.MAX_VALUE)
            return;

        ContentType type = response.contentType();
        if(type == null)
            return;
        if(!type.equals(ContentType.JSON)) {
            if(!quiet)
                Console.warn("Pagination not supported for", type);
            return;
        }

        JsonElement json;
        try(Body body = response.body()) {
            json = body.json();
        }
        if(json.isEmpty() || !json.isStructure()) {
            if(!quiet && !json.isEmpty())
                Console.warn("Pagination only supported for objects and arrays");
            response.setBody(Body.ofJson(json));
            return;
        }

        long total = json.size();
        if(totalHeaderName != null && !totalHeaderName.isBlank())
            response.setHeaderField(totalHeaderName, total+"");

        if(range.offset == 0 && range.limit >= total) {
            response.setBody(Body.ofJson(json));
        }
        else if(json.isArray()) {
            response.setBody(Body.ofJson(json.asArray()
                    .stream()
                    .skip(range.offset)
                    .limit(range.limit)));
        }
        else {
            JsonObject res = new JsonObject();
            json.asObject()
                    .entrySet()
                    .stream()
                    .skip(range.offset)
                    .limit(range.limit)
                    .forEachOrdered(e -> res.put(e.getKey(), e.getValue()));
            response.setBody(Body.ofJson(res));
        }
    }



    public static final class Range {

        public final long offset;
        public final long limit;

        public Range(long offset, long limit) {
            this.offset = Arguments.checkRange(offset, 0L, null);
            this.limit = Arguments.checkRange(limit, 1L, null);
        }

        @Override
        public String toString() {
            return offset + limit < offset ? ">= "+offset : offset+" - "+(offset + limit);
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(!(o instanceof Range)) return false;
            Range range = (Range) o;
            return offset == range.offset && limit == range.limit;
        }

        @Override
        public int hashCode() {
            return Objects.hash(offset, limit);
        }
    }
}

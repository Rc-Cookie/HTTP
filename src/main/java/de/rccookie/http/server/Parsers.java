package de.rccookie.http.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rccookie.http.Body;
import de.rccookie.http.ContentType;
import de.rccookie.http.ContentTypes;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.server.annotation.Parse;
import de.rccookie.util.Arguments;
import org.jetbrains.annotations.NotNull;

final class Parsers {

    private Parsers() {
        throw new UnsupportedOperationException();
    }

    private static final Map<Class<? extends Parser>, Parser> PARSERS = new HashMap<>();

    public static Parser getParser(Parse parsed) {
        MultiParser parser = new MultiParser();
        Class<? extends Parser>[] parserTypes = parsed.value();
        if(parserTypes.length == 0)
            throw new IllegalHttpRequestListenerException("@Parse requires at least one parser class argument");
        for(Class<? extends Parser> parserType : parserTypes)
            parser.addParser(PARSERS.computeIfAbsent(parserType, Parsers::createParser));
        return parser;
    }

    public static Parser createParser(Class<? extends Parser> type) {
        try {
            Constructor<? extends Parser> ctor = type.getConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch(NoSuchMethodException e) {
            throw new BadHttpParserConstructor("Parser type " + type + " has no accessible constructor without arguments");
        } catch(InstantiationException e) {
            throw new BadHttpParserConstructor("Parser type " + type + " is not a normal class");
        } catch(InvocationTargetException e) {
            throw new BadHttpParserConstructor("Exception creating parser of type " + type, e.getCause());
        } catch(IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }


    private static class MultiParser implements Parser {

        private final Set<Parser> parsers = new HashSet<>();
        private final Set<ContentType> mimeTypes = new HashSet<>();
        private final Set<Parser> supportingUnknownType = new HashSet<>();

        @Override
        public <T> T parse(@NotNull Body data, @NotNull Type targetType, @NotNull HttpRequest request) {
            ContentType contentType = request.contentType();
            if(contentType != null) try {
                return parsers.stream()
                        .filter(p -> p.getMIMETypes().contains(contentType))
                        .findAny()
                        .orElseThrow(() -> HttpRequestFailure.unsupportedMediaType(contentType, mimeTypes))
                        .parse(data, targetType, request);
            } catch(HttpRequestFailure f) {
                throw f;
            } catch(Exception e) {
                throw HttpRequestFailure.parsingError(e);
            }
            if(supportingUnknownType.isEmpty())
                throw HttpRequestFailure.unsupportedMediaType(null, mimeTypes);

            List<RuntimeException> exceptions = new ArrayList<>();
            for(Parser parser : supportingUnknownType) try {
                return parser.parse(data, targetType, request);
            } catch(RuntimeException e) {
                exceptions.add(e);
            }
            throw HttpRequestFailure.parsingError(exceptions);
        }

        @Override
        public ContentTypes getMIMETypes() {
            return ContentTypes.of(mimeTypes);
        }

        @Override
        public boolean supportsUnknownMIMEType() {
            return !supportingUnknownType.isEmpty();
        }

        public MultiParser addParser(Parser parser) {
            if(!parsers.add(Arguments.checkNull(parser, "parser"))) return this;
            if(parser.supportsUnknownMIMEType())
                supportingUnknownType.add(parser);
            mimeTypes.addAll(parser.getMIMETypes());
            return this;
        }
    }
}

package de.rccookie.http.server;

import java.lang.annotation.Annotation;

import de.rccookie.http.ResponseCode;
import de.rccookie.http.server.annotation.Response;

@SuppressWarnings("ClassExplicitlyAnnotation")
final class DefaultResponse implements Response {

    public static final DefaultResponse INSTANCE = new DefaultResponse();

    private DefaultResponse() { }

    @Override
    public ResponseCode code() {
        return ResponseCode.OK;
    }

    @Override
    public Class<? extends Serializer<?>> serializer() {
        return Serializer.DEFAULT;
    }

    @Override
    public String contentType() {
        //noinspection InjectedReferences
        return "";
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Response.class;
    }
}

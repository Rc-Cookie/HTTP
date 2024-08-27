package de.rccookie.http.server;

import de.rccookie.http.Body;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;

final class DefaultHeadProcessor implements HttpProcessor {

    public static final DefaultHeadProcessor INSTANCE = new DefaultHeadProcessor();

    private DefaultHeadProcessor() { }

    @Override
    public void postprocess(HttpResponse.Editable response) {
        if(response.code().type() != ResponseCode.Type.SUCCESS) return;
        Body body = response.body();
        if(body == null) return;

        long contentLength = response.body().contentLength();
        response.setBody(Body.EMPTY);
        response.setHeaderField("Content-Length", contentLength+"");
    }
}

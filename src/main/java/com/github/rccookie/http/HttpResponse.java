package com.github.rccookie.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.rccookie.json.Json;
import com.github.rccookie.json.JsonElement;
import com.github.rccookie.util.Future;
import com.github.rccookie.util.FutureImpl;
import com.github.rccookie.util.ThreadedFutureImpl;
import com.github.rccookie.xml.Document;
import com.github.rccookie.xml.XML;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an http response. The response is not necessarily sent yet.
 * The response data gets only fetched if requested.
 */
public class HttpResponse {

    /**
     * Future to when the connection has been established and all data
     * has been sent to the server.
     */
    private final FutureImpl<HttpURLConnection> connection;
    /**
     * A future to when the request data has been fully sent to the server.
     * Returns this http response instance. The response fields don't have
     * to be loaded yet when this future is done.
     */
    public final Future<HttpResponse> sending;

    /**
     * Future to the response code of the request.
     */
    public final Future<Integer> code;
    /**
     * Future to whether the request was successful (true exactly when the
     * response code is < 400). Thus, loading this future will also load the
     * {@link #code} future.
     */
    public final Future<Boolean> success;
    /**
     * Future to the http response headers.
     */
    public final Future<Map<String,String>> header;
    /**
     * Future to the input stream over the response body. This will also load
     * the {@link #code} future.
     */
    public final Future<InputStream> in;

    /**
     * Future to the response body bytes.
     * <p>Requesting this future will consume the input stream, using other
     * input stream consuming actions will have undefined results.</p>
     */
    public final Future<byte[]> bytes;
    /**
     * Future to the response body as string.
     * <p>Requesting this future will consume the input stream using the
     * {@link #bytes} future, using other input stream consuming actions
     * will have undefined results.</p>
     */
    public final Future<String> data;

    /**
     * Future to a fully loaded response object.
     * <p>Requesting this future will consume the input stream using the
     * {@link #bytes} and {@link #data} future, using other input stream
     * consuming actions will have undefined results.</p>
     */
    public final Future<LoadedResponse> response;
    /**
     * Future to the contents of the response parsed as json.
     * <p>Requesting this future will consume the input stream, using other
     * input stream consuming actions will have undefined results.</p>
     */
    public final Future<JsonElement> json;
    /**
     * Future to the contents of the response parsed as XML.
     * <p>Requesting this future will consume the input stream, using other
     * input stream consuming actions will have undefined results.</p>
     */
    public final Future<Document> xml = xml(0);
    /**
     * Future to the contents of the response parsed as HTML.
     * <p>Requesting this future will consume the input stream, using other
     * input stream consuming actions will have undefined results.</p>
     */
    public final Future<Document> html = xml(XML.HTML);


    /**
     * Creates a new HTTP response. This will start sending the specified request.
     *
     * @param url The request url
     * @param request The request
     * @param data The data to send, or null if no data should be sent
     */
    HttpResponse(@NotNull String url, @NotNull HttpRequest request, byte @Nullable [] data) {
        FutureImpl<HttpResponse> sendingImpl = new ThreadedFutureImpl<>();
        sending = sendingImpl;
        connection = new ThreadedFutureImpl<>();
        OnDemandFutureImpl.EXECUTOR.submit(() -> {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                con.setRequestMethod(request.method.toString());
                con.setInstanceFollowRedirects(request.redirects);
                request.header.forEach(con::setRequestProperty);

                if(data != null) {
                    con.setDoOutput(true);
                    con.getOutputStream().write(data);
                }

                connection.complete(con);
                sendingImpl.complete(this);
            } catch (IOException e) {
                sendingImpl.fail(e);
            }
        });

        code = new OnDemandFutureImpl<>(() -> connection.waitFor().getResponseCode());
        success = new OnDemandFutureImpl<>(() -> code.waitFor() < 400);
        header = new OnDemandFutureImpl<>(() -> {
            Map<String, List<String>> resultHeader = connection.waitFor().getHeaderFields();
            Map<String, String> stringHeaders = new HashMap<>(resultHeader.size());
            resultHeader.forEach((k, vs) -> stringHeaders.put(k, String.join(";", vs)));
            return stringHeaders;
        });
        in = new OnDemandFutureImpl<>(() -> success.waitFor() ? connection.get().getInputStream() : connection.get().getErrorStream());
        bytes = new OnDemandFutureImpl<>(() -> in.waitFor().readAllBytes());
        this.data = new OnDemandFutureImpl<>(() -> new String(bytes.waitFor()));
        response = new OnDemandFutureImpl<>(() -> new LoadedResponse(code.waitFor(), bytes.waitFor(), this.data.waitFor(), header.waitFor()));
        json = new OnDemandFutureImpl<>(() -> Json.parse(in.waitFor()));
    }

    /**
     * Returns a future of the contents of this response parsed as xml using
     * the specified parsing flags.
     *
     * @param options Parsing options to pass to the XML parser
     * @return Future to the parsed XML
     *
     * @see XML
     */
    public Future<Document> xml(long options) {
        return new OnDemandFutureImpl<>(() -> XML.parse(in.waitFor(), options));
    }

    /**
     * Blocks until the response of this request is fully loaded and returns it.
     * <p>This is exactly equivalent to {@code response.waitFor()}.</p>
     *
     * @return The loaded response
     */
    public LoadedResponse load() {
        return response.waitFor();
    }

    @Override
    public String toString() {
        if(!sending.isDone()) return "HttpRequest (pending)";
        return "HttpRequest (" + code.waitFor() + ")";
    }
}

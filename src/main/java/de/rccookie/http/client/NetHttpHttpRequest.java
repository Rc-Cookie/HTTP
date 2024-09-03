package de.rccookie.http.client;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.function.Function;

import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.Method;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Arguments;
import de.rccookie.util.Future;
import de.rccookie.util.FutureImpl;
import de.rccookie.util.NoWaitFutureImpl;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;

final class NetHttpHttpRequest extends AbstractSendableHttpRequest {

    static {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "connection,content-length,expect,host,upgrade");
    }

    private String httpVersion = "2";

    NetHttpHttpRequest(URL url) {
        super(url);
    }

    @Override
    public String httpVersion() {
        return httpVersion;
    }

    @Override
    public HttpClient httpClient() {
        return HttpClient.STD_NET_HTTP;
    }

    @Override
    public Unsent setHttpVersion(String version) {
        if(!Arguments.checkNull(version, "version").equals("1.1") && !version.equals("2"))
            throw new UnsupportedOperationException("Unsupported http version: "+version);
        this.httpVersion = version;
        return this;
    }

    @Override
    public Future<HttpResponse> sendAsync() {
        Object lock = new Object();
        FutureImpl<HttpResponse> result = new NoWaitFutureImpl<>() {
            @Override
            public HttpResponse waitFor() throws IllegalStateException, UnsupportedOperationException {
                try {
                    synchronized(lock) {
                        while(!done)
                            lock.wait();
                    }
                } catch(Exception e) {
                    throw Utils.rethrow(e);
                }
                return get();
            }
        };
        createClient().sendAsync(createRequest(), createBodyHandler())
                .thenAccept(r -> result.complete(receive(r)))
                .exceptionally(e -> {
                    result.fail(e instanceof Exception ? (Exception) e : new Exception(e));
                    return null;
                });
        return result;
    }

    @Override
    public HttpResponse send() {
        try {
            return receive(createClient().send(createRequest(), createBodyHandler()));
        } catch(Exception e) {
            throw Utils.rethrow(e);
        }
    }

    private java.net.http.HttpRequest createRequest() {
        java.net.http.HttpRequest.Builder r = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .method(method == Method.CONNECT ? "connect" : method.toString(), body != null ? body.toBodyPublisher() : java.net.http.HttpRequest.BodyPublishers.noBody());
        header.forEach((k,vs) -> vs.forEach(v -> r.header(k,v)));
        return r.build();
    }

    private java.net.http.HttpClient createClient() {
        java.net.http.HttpClient.Builder c = java.net.http.HttpClient.newBuilder()
                .followRedirects(doRedirects ? java.net.http.HttpClient.Redirect.ALWAYS : java.net.http.HttpClient.Redirect.NEVER)
                .version(httpVersion.equals("2") ? java.net.http.HttpClient.Version.HTTP_2 : java.net.http.HttpClient.Version.HTTP_1_1);
        if(connectTimeout > 0)
            c.connectTimeout(Duration.ofMillis(connectTimeout));
        return c.build();
    }

    private HttpResponse receive(java.net.http.HttpResponse<InputStream> r) {
        HttpResponse response = new ReceivedHttpResponse(
                this,
                url,
                ResponseCode.get(r.statusCode()),
                Header.ofReceived(r.headers().map()),
                Body.of(r.body()),
                r.sslSession().isPresent()
        );
        if(preprocessors != null)
            for(Function<? super HttpResponse, ? extends HttpResponse> preprocessor : preprocessors)
                response = preprocessor.apply(response);
        return response;
    }

    private java.net.http.HttpResponse.BodyHandler<InputStream> createBodyHandler() {
        return java.net.http.HttpResponse.BodyHandlers.ofInputStream();
    }

    @Override
    public @NotNull HttpRequest.Unsent clone() {
        NetHttpHttpRequest copy = new NetHttpHttpRequest(url);
        copyTo(copy);
        copy.httpVersion = httpVersion;
        return copy;
    }
}

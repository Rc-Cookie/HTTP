package de.rccookie.http.client;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.Method;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Future;
import de.rccookie.util.ThreadedFutureImpl;
import de.rccookie.util.UncheckedException;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;

final class URLConnectionHttpRequest extends AbstractSendableHttpRequest {

    URLConnectionHttpRequest(URL url) {
        super(url);
    }

    @Override
    public HttpClient httpClient() {
        return HttpClient.STD_NET;
    }

    @Override
    public String httpVersion() {
        return "1.1";
    }

    @Override
    public Unsent setHttpVersion(String version) {
        throw new UnsupportedOperationException("setHttpVersion");
    }

    @Override
    public Unsent setMethod(Method method) {
        if(method == Method.CONNECT || method == Method.PATCH)
            throw new UnsupportedOperationException(method+" method not supported");
        if(method.isWebDAV)
            throw new UnsupportedOperationException("WebDAV methods not supported");
        return super.setMethod(method);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Future<HttpResponse> sendAsync() {
        URL url = this.url;
        Method method = this.method;
        boolean doRedirects = this.doRedirects;
        Map<String, List<String>> header = copyHeader();
        Body body = this.body;
        boolean https = isHttps();
        Function<? super HttpResponse, ? extends HttpResponse>[] preprocessors = this.preprocessors != null ? this.preprocessors.toArray(new Function[0]) : null;
        return new ThreadedFutureImpl<>(() -> send(url, method, doRedirects, header, body, https, preprocessors));
    }

    @Override
    @SuppressWarnings("unchecked")
    public HttpResponse send() {
        return send(url, method, doRedirects, copyHeader(), body, isHttps(), preprocessors != null ? preprocessors.toArray(new Function[0]) : null);
    }

    private HttpResponse send(URL url, Method method, boolean doRedirects, Map<String, ? extends List<String>> header, Body body, boolean https, Function<? super HttpResponse, ? extends HttpResponse>[] preprocessors) {
        HttpResponse response;
        try(body) {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method.toString());
            con.setInstanceFollowRedirects(doRedirects);
            con.setConnectTimeout(Math.max(connectTimeout, 0));
            header.forEach((k, vs) -> vs.forEach(v -> con.addRequestProperty(k, v)));
            if(body != null) {
                con.setDoOutput(true);
                try(OutputStream out = con.getOutputStream()) {
                    body.writeTo(out);
                } catch(IOException | UncheckedIOException e) {
                    throw e;
                } catch(RuntimeException e) {
                    if(e instanceof UncheckedException && e.getCause() instanceof IOException)
                        throw (IOException) e.getCause();
                    throw new RuntimeException("Exception while writing request body to stream", e);
                }
            }
            ResponseCode code = ResponseCode.get(con.getResponseCode());
            response = new ReceivedHttpResponse(
                    this,
                    url,
                    code,
                    Header.ofReceived(con.getHeaderFields()),
                    Body.of(code.success() ? con.getInputStream() : con.getErrorStream()),
                    https
            );
        } catch(SocketTimeoutException e) {
            throw new ConnectTimeoutException(e);
        } catch(IOException e) {
            throw new UncheckedException(e);
        } catch(Exception e) {
            throw Utils.rethrow(e);
        }
        if(preprocessors != null)
            for(Function<? super HttpResponse, ? extends HttpResponse> preprocessor : preprocessors)
                response = preprocessor.apply(response);
        return response;
    }

    @Override
    public @NotNull HttpRequest.Unsent clone() {
        URLConnectionHttpRequest copy = new URLConnectionHttpRequest(url);
        copyTo(copy);
        return copy;
    }
}

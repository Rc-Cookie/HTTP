package de.rccookie.http.client;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.Method;
import de.rccookie.http.ResponseCode;
import de.rccookie.http.UnexpectedResponseCodeException;
import de.rccookie.util.Arguments;
import de.rccookie.util.Future;
import de.rccookie.util.ThreadedFutureImpl;
import de.rccookie.util.UncheckedException;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class URLConnectionHttpRequest implements HttpRequest.Unsent {

    private URL url;
    private Method method = Method.GET;
    private final Header header = Header.newEmpty(this);
    private boolean doRedirects = false;
    private int connectTimeout = -1;
    private Body body = null;
    private List<Function<? super HttpResponse, ? extends HttpResponse>> preprocessors = null;
    private Function<? super HttpResponse, ? extends HttpResponse> responseCodeFilter = null;


    URLConnectionHttpRequest(URL url) {
        setUrl(url);
    }


    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof HttpRequest)) return false;
        HttpRequest r = (HttpRequest) obj;
        if(r instanceof HttpRequest.Unsent && ((Unsent) r).doRedirects() != doRedirects) return false;
        return method == r.method() && url.toString().equals(r.url().toString()) && header.equals(r.header());
    }

    @Override
    public int hashCode() {
        return Objects.hash(url.toString(), method, header, doRedirects);
    }

    @Override
    public String toString() {
        return method + " " + url;
    }

    @Override
    public URL url() {
        return url;
    }

    @Override
    public String httpVersion() {
        return "1.1";
    }

    @Override
    public Method method() {
        return method;
    }

    @Override
    public HttpClient httpClient() {
        return HttpClient.STD_NET;
    }

    @Override
    public Unsent setUrl(URL url) {
        String protocol = Arguments.checkNull(url, "url").getProtocol();
        if(!protocol.equals("http") && !protocol.equals("https"))
            throw new IllegalArgumentException("Incorrect protocol '"+protocol+"', http or https expected");
        this.url = url;
        return this;
    }

    @Override
    public Unsent setHttpVersion(String version) {
        throw new UnsupportedOperationException("setHttpVersion");
    }

    @Override
    public Unsent setMethod(Method method) {
        this.method = Arguments.checkNull(method, "method");
        return this;
    }

    @Override
    public boolean doRedirects() {
        return doRedirects;
    }

    @Override
    public Unsent setDoRedirects(boolean doRedirects) {
        this.doRedirects = doRedirects;
        return this;
    }

    @Override
    public long connectTimeout() {
        return connectTimeout;
    }

    @Override
    public Unsent setConnectTimeout(long timeout) {
        this.connectTimeout = timeout > 0 ? (int) Math.min(timeout, Integer.MAX_VALUE) : -1;
        return this;
    }

    @Override
    public Header header() {
        return header;
    }

    @Override
    public Body body() {
        return body;
    }

    @Override
    public Unsent setBody(@Nullable Body body) {
        this.body = body;
        long length;
        if(body != null && (length = body.contentLength()) >= 0)
            setHeaderField("Content-Length", length+"");
        else header().remove("Content-Length");
        return this;
    }

    @Override
    public Unsent addResponsePreprocessor(@NotNull Function<? super HttpResponse, ? extends HttpResponse> preprocessor) {
        Arguments.checkNull(preprocessor, "preprocessor");
        if(preprocessors == null)
            preprocessors = new ArrayList<>();
        preprocessors.add(preprocessor);
        return this;
    }

    @Override
    public Unsent expectResponseCode(@Nullable Predicate<? super ResponseCode> filter) {
        if(responseCodeFilter != null)
            preprocessors.remove(responseCodeFilter);
        if(filter == null)
            return this;
        return addResponsePreprocessor(r -> {
            if(filter.test(r.code()))
                return r;
            throw new UnexpectedResponseCodeException(r);
        });
    }

    @Override
    public InetSocketAddress client() {
        return new InetSocketAddress(0);
    }

    @Override
    public InetSocketAddress server() {
        try {
            return new InetSocketAddress(InetAddress.getByName(url.getHost()), url.getPort() != -1 ? url.getPort() : url.getDefaultPort());
        } catch(UnknownHostException e) {
            throw new UncheckedException(e);
        }
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
                    Header.of(con.getHeaderFields()),
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

    private Map<String, List<String>> copyHeader() {
        Map<String, List<String>> copy = new HashMap<>();
        header.forEach((k,vs) -> copy.put(k, new ArrayList<>(vs)));
        return copy;
    }

    @Override
    public @NotNull HttpRequest.Unsent clone() {
        URLConnectionHttpRequest copy = new URLConnectionHttpRequest(url);
        copy.method = method;
        header.forEach((k,vs) -> copy.header.put(k, Header.mutableValues(vs)));
        copy.doRedirects = doRedirects;
        copy.body = body;
        copy.preprocessors = preprocessors != null ? new ArrayList<>(preprocessors) : null;
        copy.responseCodeFilter = responseCodeFilter;
        return copy;
    }
}

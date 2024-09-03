package de.rccookie.http.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
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
import de.rccookie.util.UncheckedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class AbstractSendableHttpRequest implements HttpRequest.Unsent {

    protected URL url;
    protected Method method = Method.GET;
    protected final Header header = Header.newEmpty(this);
    protected boolean doRedirects = false;
    protected int connectTimeout = -1;
    protected Body body = null;
    protected List<Function<? super HttpResponse, ? extends HttpResponse>> preprocessors = null;
    protected Function<? super HttpResponse, ? extends HttpResponse> responseCodeFilter = null;


    protected AbstractSendableHttpRequest(URL url) {
        setUrl(url);
    }


    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof HttpRequest)) return false;
        HttpRequest r = (HttpRequest) obj;
        if(r instanceof Unsent && ((Unsent) r).doRedirects() != doRedirects) return false;
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
    public Method method() {
        return method;
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

    protected Map<String, List<String>> copyHeader() {
        Map<String, List<String>> copy = new HashMap<>();
        header.forEach((k,vs) -> copy.put(k, new ArrayList<>(vs)));
        return copy;
    }

    @Override
    @NotNull
    public abstract HttpRequest.Unsent clone();

    protected void copyTo(AbstractSendableHttpRequest copy) {
        copy.url = url;
        copy.method = method;
        header.forEach((k,vs) -> copy.header.put(k, Header.mutableValues(vs)));
        copy.doRedirects = doRedirects;
        copy.body = body;
        copy.preprocessors = preprocessors != null ? new ArrayList<>(preprocessors) : null;
        copy.responseCodeFilter = responseCodeFilter;
    }
}

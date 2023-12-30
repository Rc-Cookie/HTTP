package de.rccookie.http.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Objects;

import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Arguments;
import de.rccookie.util.Future;
import de.rccookie.util.ThreadedFutureImpl;
import de.rccookie.util.UncheckedException;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.Nullable;

final class SendableHttpRequest implements HttpRequest.Unsent {

    private URL url;
    private Method method = Method.GET;
    private final Header header = Header.newEmpty(this);
    private boolean doRedirects = true;
    private Body body = null;


    SendableHttpRequest(URL url) {
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
    public InetSocketAddress client() {
        return new InetSocketAddress(0);
    }

    @Override
    public InetSocketAddress server() {
        try {
            return new InetSocketAddress(InetAddress.getByName(url.getHost()), url.getPort() != -1 ? url.getPort() : url.getDefaultPort());
        } catch (UnknownHostException e) {
            throw new UncheckedException(e);
        }
    }

    @Override
    public Future<HttpResponse> sendAsync() {
        return new ThreadedFutureImpl<>(this::send);
    }

    @Override
    public HttpResponse send() {
        try {
            try(Body body = this.body) {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod(method.toString());
                con.setInstanceFollowRedirects(doRedirects);
                header.forEach((k, vs) -> vs.forEach(v -> con.addRequestProperty(k, v)));
                if(body != null) {
                    con.setDoOutput(true);
                    try(OutputStream out = con.getOutputStream()) {
                        body.stream().transferTo(out);
                    }
                }
                ResponseCode code = ResponseCode.get(con.getResponseCode());
                return new ReceivedHttpResponse(
                        this,
                        server(),
                        code,
                        Header.of(con.getHeaderFields()),
                        Body.of(code.success() ? con.getInputStream() : con.getErrorStream()),
                        isHttps()
                );
            } catch(IOException e) {
                throw new UncheckedException(e);
            }
        } catch(Exception e) {
            throw Utils.rethrow(e);
        }
    }
}

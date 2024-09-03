package de.rccookie.http.server.raw;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.diogonunes.jcolor.Attribute;
import de.rccookie.http.Body;
import de.rccookie.http.Header;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.Method;
import de.rccookie.http.ResponseCode;
import de.rccookie.http.server.HttpControlFlowException;
import de.rccookie.http.server.HttpErrorFormatter;
import de.rccookie.http.server.HttpRedirect;
import de.rccookie.http.server.HttpRequestFailure;
import de.rccookie.http.server.HttpSendException;
import de.rccookie.util.Console;
import de.rccookie.util.Utils;
import de.rccookie.util.Wrapper;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * A bare-bones http server using the {@link HttpRequest} and {@link HttpResponse} API.
 * Does not include routing, method-specific special handling (e.g. HEAD requests) or
 * other more advanced stuff.
 */
public abstract class RawHttpServer {

    static {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    private static final Object LOG_LOCK = new Object();

    Executor executor;
    private final List<ServerSocket> servers = new ArrayList<>();

    @Nullable
    private String logLevel = "debug";

    @Nullable
    String name = "RcCookie";


    /**
     * Creates a new http server not yet bound to any port. The server should
     * first be configured, then bound to one or more ports using one of the
     * {@link #listen(int)} methods.
     */
    public RawHttpServer() { }


    /**
     * Binds the server to the given port and starts listening for incoming requests.
     * This method does not block further execution. A server can listen to multiple
     * ports and addresses. If the port can't be bound to (e.g. because it is already
     * in use or the application does not have sufficient rights to do so) an exception
     * will be thrown. The server will use the platform default backlog.
     *
     * @param port The port to bind to
     */
    public void listen(@Range(from = 0, to = 65536) int port) {
        listen(port, 0);
    }

    /**
     * Binds the server to the given port and starts listening for incoming requests.
     * This method does not block further execution. A server can listen to multiple
     * ports and addresses. If the port can't be bound to (e.g. because it is already
     * in use or the application does not have sufficient rights to do so) an exception
     * will be thrown.
     *
     * @param port The port to bind to
     * @param backlog The maximum number of requests to queue for execution, or 0 for platform default
     */
    public void listen(@Range(from = 0, to = 65536) int port, int backlog) {
        listen(new InetSocketAddress(port), backlog);
    }

    /**
     * Binds the server to the given address and port and starts listening for
     * incoming requests. This method does not block further execution. A server
     * can listen to multiple ports and addresses. If the address can't be bound
     * to (e.g. because it is already in use) an exception will be thrown.
     *
     * @param address The address and port to bind the server to
     * @param backlog The maximum number of requests to queue for execution, or 0 for platform default
     */
    public void listen(InetSocketAddress address, int backlog) {
        try {
            synchronized(this) {
                if(executor == null)
                    executor = createExecutor();
            }

            ServerSocket server = new ServerSocket(address.getPort(), backlog, address.getAddress());
            new Thread(() -> {
                while(!server.isClosed()) try {
                    Socket client = server.accept();
                    Console.map("New connection from", client.getInetAddress() + ":" + client.getPort());
                    handleAsync(client);
                } catch(IOException e) {
                    Console.error(e);
                }
            }, address+" dispatcher thread").start();
            synchronized(servers) {
                servers.add(server);
            }

        } catch(Exception e) {
            throw Utils.rethrow(e);
        }
        Console.write(logLevel, address.getAddress().isAnyLocalAddress() ? "Server listening on port "+address.getPort() : "Server listening on "+address);
    }


    /**
     * Returns the executor to be used to execute request handlers. This method will
     * only be called once. The default implementation returns {@link Executors#newCachedThreadPool()}.
     *
     * @return The executor to use in the server
     */
    protected Executor createExecutor() {
        return Executors.newCachedThreadPool();
    }

    /**
     * Called for each incoming http request, on no particular thread. The method should generate
     * and send a response to the request using one of the {@link HttpRequest.Respondable#respond()}
     * methods (and must also call {@link HttpResponse.Sendable#send()} or {@link HttpResponse.Sendable#sendAsync()}
     * eventually). The response can be sent from a different thread, after this method has already
     * returned.
     *
     * @param request The request to process and respond to
     * @throws HttpControlFlowException When thrown, a default response for the given response code will
     *                                  be generated and sent. For Internal Server Errors, the stack trace
     *                                  will also be logged (not in the response).
     * @throws HttpRedirect When thrown, a default response for the given response code will be generated
     *                      and sent.
     * @throws Exception Any other exceptions will cause an Internal Server Error to be sent to the client.
     *                   Note that this obviously only works if the exception was thrown from the calling
     *                   thread and not on a different thread processing this request.
     */
    protected abstract void respond(HttpRequest.Respondable request) throws Exception;

    void handleAsync(Socket client) {
        executor.execute(() -> {
            try {
                sendResponseToRequest(client);
            } catch(Exception e) {
                Console.error("Failed to transfer response:");
                Console.error(e);
            }
        });
    }

    private void sendResponseToRequest(Socket client) throws HttpSendException {
        HttpRequest.Respondable request = null;
        Wrapper<String> version = new Wrapper<>();
        try {
            request = receiveRequest(client, version);
        } catch(IOException e) {
            Console.error(e);
        } catch(HttpRequestFailure f) {
            if(version.value != null) {
                HttpResponse.Sendable response = new HttpResponseImpl(
                        this,
                        null,
                        version.value.contains("/") ? version.value.substring(version.value.indexOf('/')) : version.value,
                        new InetSocketAddress(client.getLocalAddress(), client.getLocalPort()),
                        new InetSocketAddress(client.getInetAddress(), client.getPort()),
                        client,
                        f.code()
                );
                HttpErrorFormatter.DEFAULT.format(response, f);
                response.header().setKeepAlive(false);
                response.send();
            }
        }

        if(request == null) {
            try {
                if(!client.isClosed())
                    client.close();
            } catch(IOException ignored) { }
            Console.debug("Connection to", client.getInetAddress()+":"+client.getPort(), "closed");
            return;
        }

        try {
            logRequest(request);
        } catch(Exception e) {
            Console.error("Error in request logger:");
            Console.error(e);
        }

        try {
            respond(request);
        } catch(HttpRedirect r) {
            HttpResponse.Sendable response = request.respond(r.code());
            r.format(response);
            response.send();
        } catch(Exception e) {
            if(request.getResponse() != null && request.getResponse().state() == HttpResponse.State.SENT)
                throw Utils.rethrow(e);
            HttpRequestFailure f = e instanceof HttpRequestFailure ? (HttpRequestFailure) e : HttpRequestFailure.internal(e);
            if(f.code() == ResponseCode.INTERNAL_SERVER_ERROR && f.getCause() != null)
                Console.error(f.getCause());
            HttpErrorFormatter.DEFAULT.format(request.respond(f.code()), f);
            request.getResponse().send();
        }
    }

    private HttpRequest.Respondable receiveRequest(Socket client, Wrapper<String> protocolOut) throws IOException, HttpRequestFailure {
        try {

            InputStream in = client.getInputStream();
            StringBuilder method = new StringBuilder(7);
            int c;
            while((c = in.read()) > ' ')
                method.append((char) c);
            if(c == -1) return null;

            c = skipWhitespaces(in);

            StringBuilder uri = new StringBuilder();
            do uri.append((char) c);
            while((c = in.read()) > ' ');
            if(c == -1) return null;

            c = skipWhitespaces(in);

            StringBuilder version = new StringBuilder(8);
            do version.append(Character.toUpperCase((char) c));
            while((c = in.read()) > ' ');
            if(c == -1) return null;
            String versionStr = version.toString().toUpperCase();
            protocolOut.value = versionStr;

            if(!versionStr.startsWith("HTTP/"))
                throw new HttpRequestFailure(ResponseCode.HTTP_VERSION_NOT_SUPPORTED, versionStr);

            Method methodObj;
            try {
                methodObj = Method.valueOf(method.toString().toUpperCase());
            } catch(IllegalArgumentException e) {
                Console.warn("Client sent illegal http method:", method);
                throw new HttpRequestFailure(ResponseCode.METHOD_NOT_ALLOWED, "Illegal HTTP method: "+method);
            }

            while((c = in.read()) != '\n')
                if(c == -1) return null;

            Map<String, List<String>> header = new HashMap<>();
            Body body = parseHeaderAndBody(methodObj, in, header);
            if(body == null)
                throw HttpRequestFailure.badRequest("Incomplete header");

            String url = uri.toString();
            if(url.startsWith("/") || url.equals("*"))
                url = "http://" + header.getOrDefault("host", List.of("")).get(0) + (url.equals("*") ? "/" : "") + url;

            return new HttpRequestImpl(
                    this,
                    client,
                    new URL(url),
                    versionStr.substring(5),
                    methodObj,
                    Header.ofReceived(header),
                    body,
                    new InetSocketAddress(client.getInetAddress(), client.getPort()),
                    new InetSocketAddress(client.getLocalAddress(), client.getLocalPort())
            );
        } catch(MalformedURLException e) {
            Console.warn("Client sent invalid url:", e.getMessage());
            throw HttpRequestFailure.badRequest("Invalid URI");
        }
    }

    private static Body parseHeaderAndBody(Method method, InputStream in, Map<String, List<String>> headerOut) throws IOException {
        long length = -1;
        boolean chunked = false;
        int c = in.read();
        while(true) {
            int d = in.read();
            if(c == '\r' && d == '\n') break;
            if(c == -1 || d == -1)
                return Body.EMPTY;

            StringBuilder name = new StringBuilder();
            name.append(Character.toLowerCase((char) c));

            c = d;
            while(c != ':') {
                if(c == -1) return null;
                name.append(Character.toLowerCase((char) c));
                c = in.read();
            }

            while((c = in.read()) <= ' ' && c != '\r')
                if(c == -1) return null;

            StringBuilder value = new StringBuilder();
            while(true) {
                d = in.read();
                while(c != '\r' || d != '\n') {
                    if(d == -1) return null;
                    value.append((char) c);
                    c = d;
                    d = in.read();
                }
                if((c = in.read()) == ' ' || c == '\t')
                    value.append(' ');
                else break;
            }

            String nameStr = name.toString();
            String valueStr = value.toString();
            headerOut.computeIfAbsent(nameStr, $ -> new ArrayList<>()).add(valueStr);

            if(nameStr.equals("content-length")) try {
                length = Long.parseLong(valueStr);
            } catch(NumberFormatException ignored) { }
            else if(nameStr.equals("transfer-encoding"))
                chunked = valueStr.equalsIgnoreCase("chunked");
        }

        if(chunked)
            return Body.of(new ChunkedInputStream(in));
        if(length > 0)
            return Body.of(new FixedLengthInputStream(in, length));
        if(length == 0)
            return Body.EMPTY;

        if(method == Method.GET || method == Method.HEAD || method == Method.CONNECT || method == Method.OPTIONS || method == Method.TRACE)
            return Body.EMPTY;

        throw new HttpRequestFailure(ResponseCode.LENGTH_REQUIRED);
    }

    private static char skipWhitespaces(InputStream in) throws IOException {
        int c;
        while((c = in.read()) <= ' ')
            if(c == -1)
                throw HttpRequestFailure.badRequest("Incomplete header");
        return (char) c;
    }


    /**
     * Stops listening to all ports currently listened to. The server can later be
     * re-bound to these or other ports.
     *
     * @param maxDelaySeconds The maximum time in seconds to wait for active requests to
     *                        be finished before force terminating
     */
    public void stop(int maxDelaySeconds) {
        synchronized(servers) {
            servers.stream().parallel().forEach(s -> {
                try {
                    s.close();
                } catch(IOException e) {
                    Console.warn("Error closing socket:", e);
                }
            });
            servers.clear();
        }
    }

    /**
     * Sets the console output type for request logging, or disables any non-error logs
     * using <code>null</code>. The default level is <code>"debug"</code>.
     *
     * @param level The level to set, or <code>null</code>
     */
    public void setLogLevel(@Nullable String level) {
        this.logLevel = level;
    }

    /**
     * Returns the current console output type for logging requests, or <code>null</code>
     * if non-error logs are disabled.
     *
     * @return The current log level, or <code>null</code>
     */
    @Nullable
    public String getLogLevel() {
        return logLevel;
    }

    /**
     * Sets the server name, the default value for the 'Server' response header
     * field. May be <code>null</code>, in which case the Server field won't be set
     * by default.
     *
     * @param name The server name to set
     */
    public void setServerName(@Nullable String name) {
        this.name = name;
    }

    /**
     * Returns the server name, the default value for the 'Server' response header
     * field. May be <code>null</code>, in which case the Server field won't be set
     * by default.
     *
     * @return The server's name
     */
    public String getServerName() {
        return name;
    }

    /**
     * Called once per received request, before it gets processed.
     *
     * @param request The request to log
     */
    protected void logRequest(HttpRequest request) {
        String level = logLevel;
        if(level == null) return;

        String str = request.toString();
        String msg = "<<"+surroundWithLine(str)+"<< "+request.client().toString().substring(1);
        msg = msg.replace(str, Console.colored(str, Attribute.BOLD()));
        synchronized(LOG_LOCK) {
            Console.write(level, msg);
        }
    }

    /**
     * Called once per responded request, after it has been sent back to the client.
     *
     * @param response The response to log
     * @param forceError Whether to force this log to be an error, e.g. because there was a network error
     */
    protected void logResponse(HttpResponse response, boolean forceError) {
        String level = logLevel;
        if(level == null && !forceError && response.code().type() != ResponseCode.Type.SERVER_ERROR) return;

        String str = response.toString();
        String msg = ">>"+surroundWithLine(str)+">> "+response.client().toString().substring(1);
        msg = msg.replace(str, Console.colored(str, getColor(forceError ? ResponseCode.Type.SERVER_ERROR : response.code().type()), Attribute.BOLD()));
        synchronized(LOG_LOCK) {
            if(forceError || response.code().type() == ResponseCode.Type.SERVER_ERROR)
                Console.error(msg);
            else Console.write(level, msg);
        }
    }

    private static Attribute getColor(ResponseCode.Type type) {
        switch(type) {
            case INFORMATIONAL: return Attribute.BLUE_TEXT();
            case SUCCESS: return Attribute.GREEN_TEXT();
            case REDIRECT: return Attribute.WHITE_TEXT();
            case CLIENT_ERROR: return Attribute.MAGENTA_TEXT();
            case SERVER_ERROR: return Attribute.RED_TEXT();
            default: throw new NullPointerException();
        }
    }

    private static final char LINE_CHAR = '=';
    private static final int MIN_LINE_SIZE = 4;
    private static final int PREFERRED_LOG_SIZE = 90;

    private static String surroundWithLine(String str) {
        int l,r;

        int min = str.length() + 2 * (MIN_LINE_SIZE + 1);
        if(min >= PREFERRED_LOG_SIZE)
            l = r = MIN_LINE_SIZE;
        else {
            l = MIN_LINE_SIZE + (PREFERRED_LOG_SIZE - min) / 2;
            r = MIN_LINE_SIZE + (PREFERRED_LOG_SIZE - min) - l;
        }

        return Utils.repeat(LINE_CHAR, l)+" "+str+" "+Utils.repeat(LINE_CHAR, r);
    }
}

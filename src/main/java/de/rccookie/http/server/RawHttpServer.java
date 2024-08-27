package de.rccookie.http.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.diogonunes.jcolor.Attribute;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.HttpResponse;
import de.rccookie.http.ResponseCode;
import de.rccookie.util.Console;
import de.rccookie.util.Utils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * A bare-bones http server using the {@link HttpRequest} and {@link HttpResponse} API.
 * Does not include routing, method-specific special handling (e.g. HEAD requests) or
 * other more advanced stuff.
 */
public abstract class RawHttpServer {

    private static final Object LOG_LOCK = new Object();

    private Executor executor;
    private final List<HttpServer> servers = new ArrayList<>();

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
            HttpServer server = HttpServer.create(address, backlog);
            server.setExecutor(executor);
            server.createContext("/", this::handle);
            server.start();
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

    private void handle(HttpExchange connection) {
        try {
            sendResponseToRequest(connection);
        } catch(Exception e) {
            Console.error("Failed to transfer response:");
            Console.error(e);
        }
    }

    private void sendResponseToRequest(HttpExchange connection) throws HttpSendException {
        ReceivedHttpRequest request;
        try {
            request = new ReceivedHttpRequest(this, connection);
        } catch(HttpRequestFailure f) {
            HttpResponse.Sendable response = new SendableHttpResponse(this, connection, null, f.code());
            HttpErrorFormatter.DEFAULT.format(response, f);
            response.send();
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
            HttpResponse.Sendable response = request.respond();
            r.format(response);
            response.send();
        } catch(Exception e) {
            if(request.getResponse() != null && request.getResponse().state() == HttpResponse.State.SENT)
                throw Utils.rethrow(e);
            HttpRequestFailure f = e instanceof HttpRequestFailure ? (HttpRequestFailure) e : HttpRequestFailure.internal(e);
            if(f.code() == ResponseCode.INTERNAL_SERVER_ERROR && f.getCause() != null)
                Console.error(f.getCause());
            DefaultErrorFormatter.INSTANCE.format(request.respond(f.code()), f);
            request.getResponse().send();
        }
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
            servers.stream().parallel().forEach(s -> s.stop(maxDelaySeconds));
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

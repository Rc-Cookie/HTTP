package de.rccookie.http.server.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import de.rccookie.http.Body;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.ThrowingConsumer;
import de.rccookie.http.server.CurrentHttpServerContext;
import de.rccookie.http.util.BodyWriter;
import de.rccookie.http.util.HttpStream;
import de.rccookie.util.Console;
import de.rccookie.util.UncheckedException;
import de.rccookie.util.Utils;
import de.rccookie.xml.Document;
import de.rccookie.xml.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpPipe extends HttpStream {

    @Nullable
    private Connection connection = null;

    public ConnectionHandle register(HttpRequest.Received request) {
        return new ConnectionHandle(request);
    }

    public ConnectionHandle register() {
        return register(CurrentHttpServerContext.request());
    }

    @NotNull
    public synchronized Connection connection() throws NoConnectionException {
        if(connection == null)
            throw new NoConnectionException();
        return connection;
    }

    public synchronized boolean hasConnection() {
        return connection != null;
    }

    void broadcast(ThrowingConsumer<HttpStream, IOException> action) throws NoConnectionException {
        Connection connection = connection();
        try {
            action.accept(connection.out);
        } catch(IOException | UncheckedIOException | UncheckedException e) {
            if(e instanceof UncheckedException && !(e.getCause() instanceof IOException))
                throw (UncheckedException) e;
            Console.debug("IOException during broadcast action, closing connection:", e);
            connection.close();
        }
    }

    @Override
    public void write(int b) throws NoConnectionException {
        broadcast(o -> o.write(b));
    }

    @Override
    public void write(byte @NotNull [] b) throws NoConnectionException {
        broadcast(o -> o.write(b));
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws NoConnectionException {
        broadcast(o -> o.write(b, off, len));
    }

    @Override
    public void flush() throws NoConnectionException {
        broadcast(HttpStream::flush);
    }

    @Override
    public void close() throws NoConnectionException {
        Connection connection;
        synchronized(this) {
            connection = this.connection;
        }
        if(connection != null)
            connection.close();
    }

    @Override
    public void write(Body body) throws NoConnectionException {
        broadcast(o -> o.write(body));
    }

    @Override
    public void writeData(byte[] bytes) throws NoConnectionException {
        broadcast(o -> o.writeData(bytes));
    }

    @Override
    public void writeText(String text) throws NoConnectionException {
        broadcast(o -> o.writeText(text));
    }

    @Override
    public void writeJson(Object json, boolean formatted) throws NoConnectionException {
        broadcast(o -> o.writeJson(json, formatted));
    }

    @Override
    public void writeJson(Object json) throws NoConnectionException {
        broadcast(o -> o.writeJson(json));
    }

    @Override
    public void writeXML(Node xml, long options) throws NoConnectionException {
        broadcast(o -> o.writeXML(xml, options));
    }

    @Override
    public void writeXML(Node xml) throws NoConnectionException {
        broadcast(o -> o.writeXML(xml));
    }

    @Override
    public void writeHTML(Document html) throws NoConnectionException {
        broadcast(o -> o.writeHTML(html));
    }

    @Override
    public void writeFile(Path file) throws NoConnectionException {
        broadcast(o -> o.writeFile(file));
    }

    @Override
    public void writeFile(File file) throws NoConnectionException {
        broadcast(o -> o.writeFile(file));
    }

    @Override
    public void writeFile(String file) throws NoConnectionException {
        broadcast(o -> o.writeFile(file));
    }

    @Override
    public void writeStream(InputStream in) throws NoConnectionException {
        broadcast(o -> o.writeStream(in));
    }

    public class Connection implements AutoCloseable {

        private final HttpStream out;
        private final HttpRequest.Received request;
        private boolean connected = true;

        Connection(HttpStream out, HttpRequest.Received request) {
            this.out = out;
            this.request = request;
        }

        public boolean connected() {
            return connected;
        }

        public HttpRequest.Received request() {
            return request;
        }

        public synchronized void waitUntilClosed() throws InterruptedException {
            while(connected)
                wait();
        }

        @Override
        public synchronized void close() {
            if(!connected) return;
            connected = false;
            synchronized(HttpPipe.this) {
                connection = null;
            }
            this.notifyAll();
        }
    }

    public class ConnectionHandle implements BodyWriter {

        private final HttpRequest.Received request;
        @Nullable
        private Connection connection = null;
        private boolean open = false;
        private boolean closed = false;

        private Runnable onConnect = () -> {
            synchronized(this) {
                open = true;
                notifyAll();
            }
        };

        private ConnectionHandle(HttpRequest.Received request) {
            this.request = request;
        }

        public boolean open() {
            return open && !closed;
        }

        public boolean closed() {
            return closed;
        }

        public synchronized boolean waitUntilOpen() {
            try {
                while(!open && !closed)
                    wait();
                return !closed;
            } catch(InterruptedException e) {
                throw Utils.rethrow(e);
            }
        }

        public ConnectionHandle onConnect(Runnable action) {
            if(closed)
                return this;
            if(open)
                action.run();
            else {
                Runnable others = onConnect;
                onConnect = () -> {
                    others.run();
                    action.run();
                };
            }
            return this;
        }

        @Override
        public void write(HttpStream out) throws IOException, InterruptedException {
            if(closed) return;

            Connection connection;
            synchronized(HttpPipe.this) {
                if(HttpPipe.this.connection != null)
                    HttpPipe.this.connection.close();
                HttpPipe.this.connection = this.connection = connection = new Connection(out, request);
            }
            try(connection) {
                if(closed) return;

                try {
                    onConnect.run();
                } catch(Exception e) {
                    Console.error("Exception in HttpPipe.ConnectionHandle.onConnect:", e);
                    return;
                }
                connection.waitUntilClosed();
            }
        }

        @Override
        public void close() throws Exception {
            closed = true;
            if(connection != null)
                connection.close();
        }
    }
}

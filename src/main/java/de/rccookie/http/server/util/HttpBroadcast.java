package de.rccookie.http.server.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import de.rccookie.http.Body;
import de.rccookie.http.HttpRequest;
import de.rccookie.http.ThrowingConsumer;
import de.rccookie.http.server.CurrentHttpServerContext;
import de.rccookie.http.util.BodyWriter;
import de.rccookie.http.util.HttpStream;
import de.rccookie.util.Console;
import de.rccookie.util.UncheckedException;
import de.rccookie.xml.Document;
import de.rccookie.xml.Node;
import org.jetbrains.annotations.NotNull;

public class HttpBroadcast extends HttpStream {

    private final Set<Connection> connections = Collections.newSetFromMap(new IdentityHashMap<>());

    public BodyWriter register(HttpRequest.Received request) {
        return out -> {
            try(Connection connection = new Connection(out, request)) {
                connection.waitUntilClosed();
            }
        };
    }

    public BodyWriter register() {
        return register(CurrentHttpServerContext.request());
    }

    public synchronized Collection<Connection> connections() {
        synchronized(connections) {
            return new ArrayList<>(connections);
        }
    }

    void broadcast(ThrowingConsumer<HttpStream, IOException> action) {
        for(Connection c : connections) {
            try {
                action.accept(c.out);
            } catch(IOException | UncheckedIOException | UncheckedException e) {
                if(e instanceof UncheckedException && !(e.getCause() instanceof IOException))
                    throw (UncheckedException) e;
                Console.debug("IOException during broadcast action, closing connection:", e);
                c.close();
            }
        }
    }

    @Override
    public void write(int b) {
        broadcast(o -> o.write(b));
    }

    @Override
    public void write(byte @NotNull [] b) {
        broadcast(o -> o.write(b));
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) {
        broadcast(o -> o.write(b, off, len));
    }

    @Override
    public void flush() {
        broadcast(HttpStream::flush);
    }

    @Override
    public void close() {
        for(Connection c : connections)
            c.close();
    }

    @Override
    public void write(Body body) {
        body.buffer();
        broadcast(o -> o.write(body));
    }

    @Override
    public void writeData(byte[] bytes) {
        broadcast(o -> o.writeData(bytes));
    }

    @Override
    public void writeText(String text) {
        broadcast(o -> o.writeText(text));
    }

    @Override
    public void writeJson(Object json, boolean formatted) {
        broadcast(o -> o.writeJson(json, formatted));
    }

    @Override
    public void writeJson(Object json) {
        broadcast(o -> o.writeJson(json));
    }

    @Override
    public void writeXML(Node xml, long options) {
        broadcast(o -> o.writeXML(xml, options));
    }

    @Override
    public void writeXML(Node xml) {
        broadcast(o -> o.writeXML(xml));
    }

    @Override
    public void writeHTML(Document html) {
        broadcast(o -> o.writeHTML(html));
    }

    @Override
    public void writeFile(Path file) {
        broadcast(o -> o.writeFile(file));
    }

    @Override
    public void writeFile(File file) {
        broadcast(o -> o.writeFile(file));
    }

    @Override
    public void writeFile(String file) {
        broadcast(o -> o.writeFile(file));
    }

    @Override
    public void writeStream(InputStream in) {
        // Only reads in if needed at least once
        Body body = Body.of(in);
        body.buffer();
        broadcast(o -> o.writeStream(body.stream()));
    }

    public class Connection implements AutoCloseable {

        private final HttpStream out;
        private final HttpRequest.Received request;
        private boolean connected = true;

        Connection(HttpStream out, HttpRequest.Received request) {
            this.out = out;
            this.request = request;
            synchronized(connections) {
                connections.add(this);
            }
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
            synchronized(connections) {
                connections.remove(this);
            }
            this.notifyAll();
        }
    }
}

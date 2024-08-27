package de.rccookie.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.rccookie.http.Body;
import de.rccookie.http.ResponseCode;
import de.rccookie.http.server.annotation.Response;
import de.rccookie.http.server.annotation.Route;
import de.rccookie.http.server.util.HttpPipe;
import de.rccookie.http.server.util.NoConnectionException;
import de.rccookie.http.util.BodyWriter;
import de.rccookie.json.JsonObject;
import de.rccookie.util.Console;
import de.rccookie.util.IntWrapper;
import de.rccookie.util.Pipe;

public class WebsocketTest implements HttpRequestListener {

    public static void main(String[] args) throws IOException {

        WebsocketTest test = new WebsocketTest();

        HttpServer server = new HttpServer();
        server.addHandler(test);
        server.listen(80);

        IntWrapper count = new IntWrapper();

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                Console.map("Currently connected", test.broadcast.connection().request().client());
//                test.broadcast.writeText("Hello "+(count.value++)+"!\n");
                test.broadcast.writeJson(new JsonObject("hello", count.value++));
            } catch(NoConnectionException e) {
                Console.log("No one connected");
            }
        }, 0, 1, TimeUnit.SECONDS);

//        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
//        server.setExecutor(Executors.newCachedThreadPool());
//        server.createContext("/infinite", exchange -> {
//            Console.log("Incoming request");
//            exchange.sendResponseHeaders(200, 0);
//            Console.log("Headers sent back");
//            while(true) try {
//                Thread.sleep(1000);
//                Console.log("Writing data");
//                exchange.getResponseBody().write(("1234567890".repeat(10000)+"\n---\n").getBytes());
//                exchange.getResponseBody().flush();
//                Console.log("Data written");
//            } catch(InterruptedException e) {
//                throw Utils.rethrow(e);
//            }
//        });
//        server.start();
    }


    @Route("/")
    public Body root() {
        return Body.of("Hello World!");
    }

    @Route("/websocket")
    @Response(code = ResponseCode.SWITCHING_PROTOCOLS)
    public void websocket() {
        Console.log(request().header());
        request().respond(ResponseCode.SWITCHING_PROTOCOLS)
                .setHeaderField("Connection", "Upgrade")
                .setHeaderField("Upgrade", "websocket");
    }

    @Route("/infinite")
    @Response(contentType = "application/json")
    public InputStream infinite() throws IOException {
        Console.log("Request received");
        Pipe pipe = new Pipe();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            Console.log("Sending data");
            try {
                pipe.write(("1234567890".repeat(10000)+"\n---\n").getBytes());
                pipe.flush();
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
            Console.log("Data sent");
        }, 1, 1, TimeUnit.SECONDS);
        return pipe;
    }

    @Route("/newInfinite")
    @Response(code = ResponseCode.OK, contentType = "text/plain")
    public BodyWriter newInfinite() {
        Console.log("Request received");
        return out -> {
            while(true) {
                Thread.sleep(1000);
                Console.log("Writing data");
//                Console.Config.out.setOut(out);
                try {
                    out.write(("1234567890" + "\n---\n").getBytes());
                    out.flush();
//                    Console.log("Hello World!");
                } finally {
//                    Console.Config.out.setOut(System.out);
                }
                Console.log("Data written");
            }
        };
    }


    private final HttpPipe broadcast = new HttpPipe();


    @Route("/notifications")
    @Response(contentType = "application/json")
    public BodyWriter notifications() {
        return broadcast.register();
    }
}

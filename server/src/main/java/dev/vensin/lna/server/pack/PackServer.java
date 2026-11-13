package dev.vensin.lna.server.pack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PackServer {

    private final ResourcePack pack;
    private HttpServer server;
    private ExecutorService executor;

    public PackServer(ResourcePack pack) {
        this.pack = pack;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
        executor = Executors.newFixedThreadPool(2, Thread.ofPlatform().daemon().name("pack-server-", 0).factory());
        server.setExecutor(executor);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    private void handle(HttpExchange exchange) {
        try (exchange) {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            if (!"/pack.zip".equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, pack.data().length);
            exchange.getResponseBody().write(pack.data());
        } catch (IOException clientWentAway) {
            // the client retries on its own
        }
    }
}

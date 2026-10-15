package io.github.vxnsin.lna.plugin.pack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Minimal HTTP server that serves the resource pack at {@code /pack.zip}. */
public final class PackServer {

    private final ResourcePack pack;
    private final Logger logger;
    private HttpServer server;
    private ExecutorService executor;

    public PackServer(ResourcePack pack, Logger logger) {
        this.pack = pack;
        this.logger = logger;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
        executor = Executors.newFixedThreadPool(2, Thread.ofPlatform().daemon().name("lna-pack-server-", 0).factory());
        server.setExecutor(executor);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            if (!"/pack.zip".equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] data = pack.data();
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
        } catch (IOException e) {
            logger.log(Level.FINE, "Pack download aborted by client", e);
        }
    }
}

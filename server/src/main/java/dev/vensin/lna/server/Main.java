package dev.vensin.lna.server;

import dev.vensin.lna.server.console.Terminal;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        // Before any class that logs or prints is loaded, see Terminal.
        Terminal.prepare();
        Server.run();
    }
}

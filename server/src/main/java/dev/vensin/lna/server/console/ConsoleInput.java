package dev.vensin.lna.server.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lines typed into the server console run as commands, with or without a leading slash. */
public final class ConsoleInput {

    private static final Logger LOG = LoggerFactory.getLogger(ConsoleInput.class);

    private ConsoleInput() {}

    public static void start() {
        Thread.ofPlatform().daemon().name("console-input").start(ConsoleInput::read);
    }

    private static void read() {
        Charset charset = System.console() != null ? System.console().charset() : Charset.defaultCharset();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String command = line.strip();
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }
                if (!command.isEmpty()) {
                    run(command);
                }
            }
        } catch (IOException closed) {
            // no console attached: the server keeps running without input
        }
    }

    private static void run(String command) {
        CommandManager commands = MinecraftServer.getCommandManager();
        CommandResult result = commands.execute(commands.getConsoleSender(), command);
        switch (result.getType()) {
            case UNKNOWN -> LOG.warn("Unknown command: {} (try help)", command.split(" ")[0]);
            case INVALID_SYNTAX -> LOG.warn("Wrong usage: {} (try help)", command);
            default -> { }
        }
    }
}

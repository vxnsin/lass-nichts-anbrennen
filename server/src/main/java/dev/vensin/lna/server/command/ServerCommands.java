package dev.vensin.lna.server.command;

import java.io.IOException;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerCommands {

    private static final Logger LOG = LoggerFactory.getLogger(ServerCommands.class);
    private static final List<String> HELP = List.of(
            "<gold>help</gold> <gray>this list</gray>",
            "<gold>list</gold> <gray>who is online</gray>",
            "<gold>op <name></gold> <gray>give a player admin rights</gray>",
            "<gold>deop <name></gold> <gray>take them away again</gray>",
            "<gold>give <item></gold> <gray>get a custom item (in game)</gray>",
            "<gold>refill</gold> <gray>fill the extinguisher in your hand (in game)</gray>",
            "<gold>fire start <class> [strength]</gold> <gray>light a test fire where you look (in game)</gray>",
            "<gold>fire clear</gold> <gray>remove all fires</gray>",
            "<gold>mission [start <id> | stop]</gold> <gray>list, start or end missions (in game, every player)</gray>",
            "<gold>party [invite <name> | accept | leave]</gold> <gray>play missions together (in game, every player)</gray>",
            "<gold>stop</gold> <gray>shut the server down</gray>");

    private ServerCommands() {}

    public static void register(CommandManager commands, Operators operators) {
        Command help = admin("help", operators);
        help.setDefaultExecutor((sender, context) -> HELP.forEach(line -> reply(sender, line)));

        Command list = admin("list", operators);
        list.setDefaultExecutor((sender, context) -> {
            List<String> names = MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                    .map(Player::getUsername).sorted().toList();
            reply(sender, names.isEmpty() ? "<gray>Nobody is online.</gray>"
                    : "<gray>" + names.size() + " online:</gray> <white>" + String.join(", ", names) + "</white>");
        });

        ArgumentWord name = ArgumentType.Word("name");
        Command op = admin("op", operators);
        op.setDefaultExecutor((sender, context) -> reply(sender, "<gray>Usage: op <name></gray>"));
        op.addSyntax((sender, context) -> change(sender, operators, context.get(name), true), name);

        Command deop = admin("deop", operators);
        deop.setDefaultExecutor((sender, context) -> reply(sender, "<gray>Usage: deop <name></gray>"));
        deop.addSyntax((sender, context) -> change(sender, operators, context.get(name), false), name);

        Command stop = admin("stop", operators);
        stop.setDefaultExecutor((sender, context) -> {
            LOG.info("Shutting down");
            MinecraftServer.stopCleanly();
            System.exit(0);
        });

        commands.register(help, list, op, deop, stop);
    }

    public static void reply(CommandSender sender, String miniMessage) {
        Component message = MiniMessage.miniMessage().deserialize(miniMessage);
        if (sender instanceof Player player) {
            player.sendMessage(message);
        } else {
            LOG.info(ANSIComponentSerializer.ansi().serialize(message));
        }
    }

    static Command admin(String name, Operators operators) {
        Command command = new Command(name);
        command.setCondition((sender, input) -> operators.allows(sender));
        return command;
    }

    private static void change(CommandSender sender, Operators operators, String name, boolean grant) {
        try {
            boolean changed = grant ? operators.add(name) : operators.remove(name);
            if (!changed) {
                reply(sender, "<gray>" + name + (grant ? " is already an operator." : " is not an operator.") + "</gray>");
                return;
            }
            reply(sender, "<green>" + name + (grant ? " is now an operator." : " is no longer an operator.") + "</green>");
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(name);
            if (player != null) {
                player.refreshCommands();
            }
        } catch (IOException e) {
            reply(sender, "<red>Could not save ops.txt: " + e.getMessage() + "</red>");
        }
    }
}

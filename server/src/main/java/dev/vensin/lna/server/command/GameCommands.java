package dev.vensin.lna.server.command;

import dev.vensin.lna.server.extinguisher.Extinguishers;
import dev.vensin.lna.server.fire.Cell;
import dev.vensin.lna.server.fire.FireClass;
import dev.vensin.lna.server.fire.FireService;
import dev.vensin.lna.server.item.CustomItem;
import dev.vensin.lna.server.item.Items;
import java.util.Optional;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.command.builder.arguments.number.ArgumentDouble;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;

/** Operator tools for trying things out in game. */
public final class GameCommands {

    private static final int REACH = 8;

    private GameCommands() {}

    public static void register(CommandManager commands, Operators operators, Items items, FireService fires,
                                Extinguishers extinguishers) {
        ArgumentWord itemId = ArgumentType.Word("item");
        itemId.setSuggestionCallback((sender, context, suggestion) ->
                items.all().forEach(item -> suggestion.addEntry(new SuggestionEntry(item.id()))));
        Command give = ServerCommands.admin("give", operators);
        give.setDefaultExecutor((sender, context) -> ServerCommands.reply(sender, "<gray>Usage: give <item></gray>"));
        give.addSyntax((sender, context) -> player(sender).ifPresent(player -> {
            Optional<CustomItem> item = items.get(context.get(itemId));
            if (item.isEmpty()) {
                ServerCommands.reply(sender, "<red>Unknown item: " + context.get(itemId) + "</red>");
                return;
            }
            player.getInventory().addItemStack(items.create(item.get()));
        }), itemId);

        Command refill = ServerCommands.admin("refill", operators);
        refill.setDefaultExecutor((sender, context) -> player(sender).ifPresent(player -> ServerCommands.reply(sender,
                extinguishers.refill(player) ? "<green>Refilled.</green>" : "<gray>Hold an extinguisher first.</gray>")));

        ArgumentWord fireClass = ArgumentType.Word("class");
        ArgumentDouble strength = ArgumentType.Double("strength");
        Command start = new Command("start");
        start.addSyntax((sender, context) -> ignite(sender, fires, context.get(fireClass), 30), fireClass);
        start.addSyntax((sender, context) -> ignite(sender, fires, context.get(fireClass), context.get(strength)),
                fireClass, strength);
        Command clear = new Command("clear");
        clear.setDefaultExecutor((sender, context) -> {
            int count = fires.fires().size();
            fires.clear();
            ServerCommands.reply(sender, "<green>Removed " + count + " fires.</green>");
        });
        Command fire = ServerCommands.admin("fire", operators);
        fire.setDefaultExecutor((sender, context) ->
                ServerCommands.reply(sender, "<gray>Usage: fire start <a|b|c|d|f|electric> [strength], fire clear</gray>"));
        fire.addSubcommands(start, clear);

        commands.register(give, refill, fire);
    }

    private static void ignite(CommandSender sender, FireService fires, String className, double strength) {
        player(sender).ifPresent(player -> {
            Optional<FireClass> fireClass = FireClass.parse(className);
            Point target = player.getTargetBlockPosition(REACH);
            if (fireClass.isEmpty() || target == null) {
                ServerCommands.reply(sender, fireClass.isEmpty() ? "<red>Unknown fire class: " + className + "</red>"
                        : "<gray>Look at a block within " + REACH + " blocks.</gray>");
                return;
            }
            fires.ignite(new Cell(player.getInstance(), target.blockX(), target.blockY() + 1, target.blockZ()),
                    fireClass.get(), strength);
            ServerCommands.reply(sender, "<green>Fire of class " + fireClass.get() + " started.</green>");
        });
    }

    private static Optional<Player> player(CommandSender sender) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        ServerCommands.reply(sender, "<gray>This one only works in game.</gray>");
        return Optional.empty();
    }
}

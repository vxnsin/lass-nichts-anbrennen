package io.github.vxnsin.lna.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.vxnsin.lna.plugin.LnaPlugin;
import io.github.vxnsin.lna.plugin.item.CustomItem;
import io.github.vxnsin.lna.plugin.pack.PackDistributor;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Registers {@code /lna} (tools) and {@code /credits} via Paper's Brigadier command API. */
public final class LnaCommands {

    private static final SimpleCommandExceptionType PLAYERS_ONLY =
            new SimpleCommandExceptionType(new LiteralMessage("Dieser Befehl ist nur für Spieler*innen."));
    private static final Predicate<CommandSourceStack> ADMIN = source -> source.getSender().hasPermission("lna.admin");

    private LnaCommands() {}

    public static void register(LnaPlugin plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(root(plugin), "Werkzeuge für Lass nichts anbrennen");
            commands.register(Commands.literal("credits")
                    .executes(context -> {
                        plugin.credits().send(context.getSource().getSender());
                        return Command.SINGLE_SUCCESS;
                    })
                    .build(), "Zeigt alle Quellen und Lizenzen");
        });
    }

    private static LiteralCommandNode<CommandSourceStack> root(LnaPlugin plugin) {
        return Commands.literal("lna")
                .then(Commands.literal("give")
                        .requires(ADMIN)
                        .then(Commands.argument("item", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    plugin.items().all().stream()
                                            .map(CustomItem::id)
                                            .filter(id -> id.startsWith(builder.getRemainingLowerCase()))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> give(plugin, context, List.of(player(context))))
                                .then(Commands.argument("targets", ArgumentTypes.players())
                                        .executes(context -> give(plugin, context, context
                                                .getArgument("targets", PlayerSelectorArgumentResolver.class)
                                                .resolve(context.getSource()))))))
                .then(Commands.literal("refill")
                        .requires(ADMIN)
                        .executes(context -> {
                            Player player = player(context);
                            boolean refilled = plugin.extinguishers().refill(player);
                            plugin.message(player, refilled
                                    ? "<green>Feuerlöscher aufgefüllt.</green>"
                                    : "<red>Halte einen Feuerlöscher in der Hand.</red>");
                            return refilled ? Command.SINGLE_SUCCESS : 0;
                        }))
                .then(Commands.literal("items")
                        .requires(ADMIN)
                        .executes(context -> {
                            var sender = context.getSource().getSender();
                            plugin.message(sender, "<gold>Eigene Items:</gold>");
                            for (CustomItem item : plugin.items().all()) {
                                plugin.message(sender, "<gray>•</gray> <white><id></white> <dark_gray>(<base>)</dark_gray>",
                                        Placeholder.unparsed("id", item.id()),
                                        Placeholder.unparsed("base", item.base().getKey().asString()));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("reloadpack")
                        .requires(ADMIN)
                        .executes(context -> {
                            var sender = context.getSource().getSender();
                            try {
                                plugin.pack().reload();
                            } catch (IOException e) {
                                plugin.getLogger().log(Level.SEVERE, "Could not reload pack.zip", e);
                                plugin.message(sender, "<red>pack.zip konnte nicht gelesen werden, siehe Konsole.</red>");
                                return 0;
                            }
                            plugin.packDistributor().sendToAll();
                            plugin.message(sender, "<green>Pack neu geladen und an alle gesendet.</green> <gray>SHA-1 <sha></gray>",
                                    Placeholder.unparsed("sha", plugin.pack().sha1().substring(0, 10)));
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("pack")
                        .executes(context -> {
                            PackDistributor distributor = plugin.packDistributor();
                            distributor.send(player(context));
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    private static int give(LnaPlugin plugin, CommandContext<CommandSourceStack> context,
                            Collection<Player> targets) {
        String id = StringArgumentType.getString(context, "item");
        Optional<CustomItem> item = plugin.items().get(id);
        var sender = context.getSource().getSender();
        if (item.isEmpty()) {
            plugin.message(sender, "<red>Unbekanntes Item: <id></red>", Placeholder.unparsed("id", id));
            return 0;
        }
        for (Player target : targets) {
            ItemStack stack = plugin.items().create(item.get());
            target.getInventory().addItem(stack).values()
                    .forEach(leftover -> target.getWorld().dropItem(target.getLocation(), leftover));
        }
        plugin.message(sender, "<green><id> an <count> Spieler*in(nen) gegeben.</green>",
                Placeholder.unparsed("id", id), Placeholder.unparsed("count", String.valueOf(targets.size())));
        return targets.size();
    }

    private static Player player(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (context.getSource().getExecutor() instanceof Player player) {
            return player;
        }
        throw PLAYERS_ONLY.create();
    }
}

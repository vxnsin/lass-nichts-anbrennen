package io.github.vxnsin.lna.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.vxnsin.lna.plugin.LnaPlugin;
import io.github.vxnsin.lna.plugin.fire.Fire;
import io.github.vxnsin.lna.plugin.fire.FireClass;
import io.github.vxnsin.lna.plugin.furniture.FurnitureListener;
import io.github.vxnsin.lna.plugin.item.CustomItem;
import io.github.vxnsin.lna.plugin.map.MapBuilder;
import io.github.vxnsin.lna.plugin.map.MapDefinition;
import io.github.vxnsin.lna.plugin.map.PlacedMaps.PlacedMap;
import io.github.vxnsin.lna.plugin.pack.PackDistributor;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
                .then(fire(plugin))
                .then(map(plugin))
                .then(Commands.literal("pack")
                        .executes(context -> {
                            PackDistributor distributor = plugin.packDistributor();
                            distributor.send(player(context));
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    /** /lna fire start &lt;class&gt; [intensity] | list | clear: test fires at the block the player looks at. */
    private static LiteralArgumentBuilder<CommandSourceStack> fire(LnaPlugin plugin) {
        return Commands.literal("fire")
                .requires(ADMIN)
                .then(Commands.literal("start")
                        .then(Commands.argument("class", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (FireClass fireClass : FireClass.values()) {
                                        String name = fireClass.label().toLowerCase(Locale.ROOT);
                                        if (name.startsWith(builder.getRemainingLowerCase())) {
                                            builder.suggest(name);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> startFire(plugin, context, 30))
                                .then(Commands.argument("intensity", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> startFire(plugin, context,
                                                IntegerArgumentType.getInteger(context, "intensity"))))))
                .then(Commands.literal("list")
                        .executes(context -> {
                            var sender = context.getSource().getSender();
                            var fires = plugin.fires().fires();
                            plugin.message(sender, "<gold>Aktive Brände: <count></gold>",
                                    Placeholder.unparsed("count", String.valueOf(fires.size())));
                            for (Fire fire : fires) {
                                Block origin = fire.origin();
                                plugin.message(sender, "<gray>•</gray> <white><class></white> <gray>Stärke <intensity>, "
                                                + "<state>, bei <x> <y> <z></gray>",
                                        Placeholder.unparsed("class", fire.fireClass().display()),
                                        Placeholder.unparsed("intensity", String.valueOf(Math.round(fire.intensity()))),
                                        Placeholder.unparsed("state", fire.state() == Fire.State.BURNING ? "brennt" : "glimmt"),
                                        Placeholder.unparsed("x", String.valueOf(origin.getX())),
                                        Placeholder.unparsed("y", String.valueOf(origin.getY())),
                                        Placeholder.unparsed("z", String.valueOf(origin.getZ())));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("clear")
                        .executes(context -> {
                            int count = plugin.fires().fires().size();
                            plugin.fires().clear();
                            plugin.message(context.getSource().getSender(), "<green><count> Brände entfernt.</green>",
                                    Placeholder.unparsed("count", String.valueOf(count)));
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    /** /lna map list | build &lt;map&gt; | tp &lt;map&gt;: build maps from maps/*.yml at the player's position. */
    private static LiteralArgumentBuilder<CommandSourceStack> map(LnaPlugin plugin) {
        return Commands.literal("map")
                .requires(source -> source.getSender().hasPermission(FurnitureListener.BUILD_PERMISSION))
                .then(Commands.literal("list")
                        .executes(context -> {
                            var sender = context.getSource().getSender();
                            plugin.message(sender, "<gold>Maps:</gold>");
                            plugin.maps().values().forEach(map -> plugin.message(sender,
                                    "<gray>•</gray> <white><id></white> <gray><name> (<size>)</gray><built>",
                                    Placeholder.unparsed("id", map.id()),
                                    Placeholder.unparsed("name", map.name()),
                                    Placeholder.unparsed("size", map.width() + "×" + map.depth()),
                                    Placeholder.parsed("built", plugin.placedMaps().get(map.id()).isPresent()
                                            ? " <green>gebaut</green>" : "")));
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("build")
                        .then(Commands.argument("map", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    plugin.maps().keySet().stream()
                                            .filter(id -> id.startsWith(builder.getRemainingLowerCase()))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> buildMap(plugin, context))))
                .then(Commands.literal("tp")
                        .then(Commands.argument("map", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    plugin.placedMaps().all().stream().map(PlacedMap::id)
                                            .filter(id -> id.startsWith(builder.getRemainingLowerCase()))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = player(context);
                                    String id = StringArgumentType.getString(context, "map");
                                    Optional<Location> spawn = plugin.placedMaps().get(id).flatMap(PlacedMap::spawn);
                                    if (spawn.isEmpty()) {
                                        plugin.message(player, "<red>Die Map <id> ist noch nicht gebaut.</red>",
                                                Placeholder.unparsed("id", id));
                                        return 0;
                                    }
                                    player.teleport(spawn.get());
                                    return Command.SINGLE_SUCCESS;
                                })));
    }

    private static int buildMap(LnaPlugin plugin, CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        Player player = player(context);
        String id = StringArgumentType.getString(context, "map");
        MapDefinition map = plugin.maps().get(id);
        if (map == null) {
            plugin.message(player, "<red>Unbekannte Map: <id></red>", Placeholder.unparsed("id", id));
            return 0;
        }
        MapBuilder.Result result = plugin.mapBuilder().build(map, player.getLocation());
        try {
            plugin.placedMaps().put(result.placed());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save placed-maps.yml", e);
        }
        result.placed().spawn().ifPresent(player::teleport);
        plugin.message(player, "<green>Map gebaut:</green> <white><name></white> <gray>(<size>)</gray>",
                Placeholder.unparsed("name", map.name()),
                Placeholder.unparsed("size", map.width() + "×" + map.depth()));
        result.warnings().forEach(warning -> plugin.message(player, "<gold>Warnung:</gold> <gray><text></gray>",
                Placeholder.unparsed("text", warning)));
        return Command.SINGLE_SUCCESS;
    }

    private static int startFire(LnaPlugin plugin, CommandContext<CommandSourceStack> context, int intensity)
            throws CommandSyntaxException {
        Player player = player(context);
        String raw = StringArgumentType.getString(context, "class");
        Optional<FireClass> fireClass = FireClass.parse(raw);
        if (fireClass.isEmpty()) {
            plugin.message(player, "<red>Unbekannte Brandklasse: <raw></red> <gray>(a, b, c, d, f, elektro)</gray>",
                    Placeholder.unparsed("raw", raw));
            return 0;
        }
        Block target = player.getTargetBlockExact(8);
        if (target == null) {
            plugin.message(player, "<red>Schau auf einen Block (höchstens 8 Blöcke entfernt).</red>");
            return 0;
        }
        Block origin = target.getType().isSolid() ? target.getRelative(BlockFace.UP) : target;
        plugin.fires().ignite(origin, fireClass.get(), intensity);
        plugin.message(player, "<gold>Brand gestartet:</gold> <white><class></white> <gray>Stärke <intensity></gray>",
                Placeholder.unparsed("class", fireClass.get().display()),
                Placeholder.unparsed("intensity", String.valueOf(intensity)));
        return Command.SINGLE_SUCCESS;
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

package io.github.vxnsin.lna.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.vxnsin.lna.plugin.LnaPlugin;
import io.github.vxnsin.lna.plugin.mission.MissionInfo;
import io.github.vxnsin.lna.plugin.mission.PartyService.Party;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/** Commands for all players: /mission (list, start, stop) and /gruppe (group management). */
final class PlayerCommands {

    private PlayerCommands() {}

    static void register(Commands commands, LnaPlugin plugin) {
        commands.register(mission(plugin), "Missionen der Kampagne", List.of("missionen"));
        commands.register(group(plugin), "Gruppe für Missionen", List.of("party"));
    }

    private static LiteralCommandNode<CommandSourceStack> mission(LnaPlugin plugin) {
        return Commands.literal("mission")
                .executes(context -> listMissions(plugin, LnaCommands.player(context)))
                .then(Commands.literal("list").executes(context -> listMissions(plugin, LnaCommands.player(context))))
                .then(Commands.literal("start")
                        .then(Commands.argument("mission", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    plugin.missions().missions().values().stream()
                                            .filter(MissionInfo::available).map(MissionInfo::id)
                                            .filter(id -> id.startsWith(builder.getRemainingLowerCase()))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player player = LnaCommands.player(context);
                                    Optional<String> error = plugin.missions()
                                            .startMission(player, StringArgumentType.getString(context, "mission"));
                                    error.ifPresent(text -> plugin.message(player, "<red><text></red>",
                                            Placeholder.unparsed("text", text)));
                                    return error.isPresent() ? 0 : Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("antwort")
                        .then(Commands.argument("nummer", IntegerArgumentType.integer(1, 9))
                                .executes(context -> {
                                    Player player = LnaCommands.player(context);
                                    boolean ok = plugin.missions().answer(player,
                                            IntegerArgumentType.getInteger(context, "nummer"));
                                    return ok ? Command.SINGLE_SUCCESS : 0;
                                })))
                .then(Commands.literal("stop")
                        .executes(context -> {
                            Player player = LnaCommands.player(context);
                            if (!plugin.missions().stop(player)) {
                                plugin.message(player, "<gray>Du bist in keiner Mission.</gray>");
                                return 0;
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    static int listMissions(LnaPlugin plugin, Player player) {
        List<String> stamps = plugin.missions().passStamps(player);
        plugin.message(player, "<gold><bold>Kampagne „Lass nichts anbrennen“</bold></gold> <gray>(Stempel: <count>)</gray>",
                Placeholder.unparsed("count", String.valueOf(stamps.size())));
        for (MissionInfo info : plugin.missions().missions().values()) {
            String state = !info.available() ? "<dark_gray>bald verfügbar</dark_gray>"
                    : "<green><click:run_command:'/mission start " + info.id() + "'>[Starten]</click></green>";
            plugin.message(player, "<white><label></white> <stamp><state>",
                    Placeholder.unparsed("label", info.label()),
                    Placeholder.parsed("stamp", stamps.contains(info.id()) ? "<green>✔</green> " : ""),
                    Placeholder.parsed("state", state));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static LiteralCommandNode<CommandSourceStack> group(LnaPlugin plugin) {
        return Commands.literal("gruppe")
                .executes(context -> groupInfo(plugin, LnaCommands.player(context)))
                .then(Commands.literal("einladen")
                        .then(Commands.argument("spieler", ArgumentTypes.player())
                                .executes(context -> invite(plugin, context))))
                .then(Commands.literal("annehmen")
                        .executes(context -> {
                            Player player = LnaCommands.player(context);
                            Optional<Party> party = plugin.parties().accept(player);
                            if (party.isEmpty()) {
                                plugin.message(player, "<red>Du hast keine gültige Einladung.</red>");
                                return 0;
                            }
                            party.get().onlineMembers().forEach(member -> plugin.message(member,
                                    "<green><name> ist der Gruppe beigetreten.</green>",
                                    Placeholder.unparsed("name", player.getName())));
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("verlassen")
                        .executes(context -> {
                            Player player = LnaCommands.player(context);
                            List<Player> before = plugin.parties().onlineMembers(player);
                            plugin.parties().leave(player);
                            before.forEach(member -> plugin.message(member, "<gray><name> hat die Gruppe verlassen.</gray>",
                                    Placeholder.unparsed("name", player.getName())));
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    private static int invite(LnaPlugin plugin, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player leader = LnaCommands.player(context);
        Player target = context.getArgument("spieler", PlayerSelectorArgumentResolver.class)
                .resolve(context.getSource()).getFirst();
        Optional<String> error = plugin.parties().invite(leader, target);
        if (error.isPresent()) {
            plugin.message(leader, "<red><text></red>", Placeholder.unparsed("text", error.get()));
            return 0;
        }
        plugin.message(leader, "<green>Einladung an <name> gesendet.</green>", Placeholder.unparsed("name", target.getName()));
        plugin.message(target, "<gold><name></gold> <white>lädt dich in eine Gruppe ein.</white> "
                        + "<green><click:run_command:'/gruppe annehmen'>[Annehmen]</click></green> <gray>(1 Minute gültig)</gray>",
                Placeholder.unparsed("name", leader.getName()));
        return Command.SINGLE_SUCCESS;
    }

    static int groupInfo(LnaPlugin plugin, Player player) {
        Optional<Party> party = plugin.parties().partyOf(player);
        if (party.isEmpty()) {
            plugin.message(player, "<gray>Du bist in keiner Gruppe.</gray> <white>Lade jemanden ein:</white> "
                    + "<yellow><click:suggest_command:'/gruppe einladen '>/gruppe einladen <Name></click></yellow>");
            return Command.SINGLE_SUCCESS;
        }
        plugin.message(player, "<gold>Deine Gruppe:</gold>");
        for (Player member : party.get().onlineMembers()) {
            boolean leader = member.getUniqueId().equals(party.get().leader());
            plugin.message(player, "<gray>•</gray> <white><name></white><role>",
                    Placeholder.unparsed("name", member.getName()),
                    Placeholder.parsed("role", leader ? " <gold>(Leitung)</gold>" : ""));
        }
        plugin.message(player, "<red><click:run_command:'/gruppe verlassen'>[Gruppe verlassen]</click></red>");
        return Command.SINGLE_SUCCESS;
    }
}

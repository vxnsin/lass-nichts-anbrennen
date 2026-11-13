package dev.vensin.lna.server.command;

import dev.vensin.lna.server.lang.Messages;
import dev.vensin.lna.server.mission.MissionInfo;
import dev.vensin.lna.server.mission.Missions;
import dev.vensin.lna.server.mission.Parties;
import dev.vensin.lna.server.mission.Parties.Party;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.command.builder.arguments.number.ArgumentNumber;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

/** Commands every player may use: missions and parties. */
public final class PlayerCommands {

    private PlayerCommands() {}

    public static void register(CommandManager commands, Messages messages, Missions missions, Parties parties) {
        commands.register(mission(messages, missions), party(messages, parties));
    }

    private static Command mission(Messages messages, Missions missions) {
        CommandExecutor showList = inGame(player -> listMissions(messages, missions, player));
        Command list = new Command("list");
        list.setDefaultExecutor(showList);

        ArgumentWord missionId = ArgumentType.Word("mission");
        missionId.setSuggestionCallback((sender, context, suggestion) -> missions.missions().values().stream()
                .filter(MissionInfo::available)
                .forEach(info -> suggestion.addEntry(new SuggestionEntry(info.id()))));
        Command start = new Command("start");
        start.setDefaultExecutor(showList);
        start.addSyntax(inGame((player, context) ->
                missions.startMission(player, context.get(missionId)).ifPresent(player::sendMessage)), missionId);

        ArgumentNumber<Integer> number = ArgumentType.Integer("number").between(1, 9);
        Command answer = new Command("antwort", "answer");
        answer.addSyntax(inGame((player, context) -> missions.answer(player, context.get(number))), number);

        Command stop = new Command("stop");
        stop.setDefaultExecutor(inGame(player -> {
            if (!missions.stop(player)) {
                player.sendMessage(messages.text(player, "mission.not-in-mission"));
            }
        }));

        Command mission = new Command("mission", "missionen", "missions");
        mission.setDefaultExecutor(showList);
        mission.addSubcommands(list, start, answer, stop);
        return mission;
    }

    private static void listMissions(Messages messages, Missions missions, Player player) {
        List<String> stamps = missions.passes().of(player.getUuid());
        player.sendMessage(messages.text(player, "mission.list.header",
                Placeholder.unparsed("count", String.valueOf(stamps.size()))));
        for (MissionInfo info : missions.missions().values()) {
            Component state = info.available()
                    ? messages.text(player, "mission.list.start",
                            Placeholder.styling("start", ClickEvent.runCommand("/mission start " + info.id())))
                    : messages.text(player, "mission.list.soon");
            player.sendMessage(messages.text(player, "mission.list.entry", Missions.label(info),
                    Placeholder.component("stamp", stamps.contains(info.id())
                            ? messages.text(player, "mission.list.stamp") : Component.empty()),
                    Placeholder.component("state", state)));
        }
    }

    private static Command party(Messages messages, Parties parties) {
        ArgumentWord name = ArgumentType.Word("player");
        name.setSuggestionCallback((sender, context, suggestion) ->
                MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                        .filter(online -> online != sender)
                        .forEach(online -> suggestion.addEntry(new SuggestionEntry(online.getUsername()))));
        Command invite = new Command("einladen", "invite");
        invite.setDefaultExecutor(inGame(player -> player.sendMessage(messages.text(player, "party.usage-invite"))));
        invite.addSyntax(inGame((player, context) -> invite(messages, parties, player, context.get(name))), name);

        Command accept = new Command("annehmen", "accept");
        accept.setDefaultExecutor(inGame(player -> {
            Optional<Party> party = parties.accept(player);
            if (party.isEmpty()) {
                player.sendMessage(messages.text(player, "party.no-invite"));
                return;
            }
            party.get().onlineMembers().forEach(member -> member.sendMessage(messages.text(member, "party.joined",
                    Placeholder.unparsed("name", player.getUsername()))));
        }));

        Command leave = new Command("verlassen", "leave");
        leave.setDefaultExecutor(inGame(player -> {
            if (parties.partyOf(player).isEmpty()) {
                player.sendMessage(messages.text(player, "party.none"));
                return;
            }
            List<Player> before = parties.onlineMembers(player);
            parties.leave(player);
            before.forEach(member -> member.sendMessage(messages.text(member, "party.left",
                    Placeholder.unparsed("name", player.getUsername()))));
        }));

        Command party = new Command("gruppe", "party");
        party.setDefaultExecutor(inGame(player -> partyInfo(messages, parties, player)));
        party.addSubcommands(invite, accept, leave);
        return party;
    }

    private static void invite(Messages messages, Parties parties, Player leader, String targetName) {
        Player target = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(targetName);
        if (target == null) {
            leader.sendMessage(messages.text(leader, "party.error.unknown-player",
                    Placeholder.unparsed("name", targetName)));
            return;
        }
        Optional<String> error = parties.invite(leader, target);
        if (error.isPresent()) {
            leader.sendMessage(messages.text(leader, error.get(), Placeholder.unparsed("name", target.getUsername())));
            return;
        }
        leader.sendMessage(messages.text(leader, "party.invite-sent", Placeholder.unparsed("name", target.getUsername())));
        target.sendMessage(messages.text(target, "party.invited", Placeholder.unparsed("name", leader.getUsername())));
    }

    private static void partyInfo(Messages messages, Parties parties, Player player) {
        Optional<Party> party = parties.partyOf(player);
        if (party.isEmpty()) {
            player.sendMessage(messages.text(player, "party.none"));
            return;
        }
        player.sendMessage(messages.text(player, "party.header"));
        for (Player member : party.get().onlineMembers()) {
            boolean leader = member.getUuid().equals(party.get().leader());
            player.sendMessage(messages.text(player, "party.member", Placeholder.unparsed("name", member.getUsername()),
                    Placeholder.component("role", leader ? messages.text(player, "party.leader") : Component.empty())));
        }
        player.sendMessage(messages.text(player, "party.leave-button"));
    }

    private interface PlayerExecutor {
        void run(Player player, CommandContext context);
    }

    private static CommandExecutor inGame(Consumer<Player> action) {
        return inGame((player, context) -> action.accept(player));
    }

    private static CommandExecutor inGame(PlayerExecutor action) {
        return (sender, context) -> {
            if (sender instanceof Player player) {
                action.run(player, context);
            } else {
                ServerCommands.reply(sender, "<gray>This one only works in game.</gray>");
            }
        };
    }
}

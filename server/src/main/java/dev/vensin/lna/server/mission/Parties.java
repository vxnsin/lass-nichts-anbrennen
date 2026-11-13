package dev.vensin.lna.server.mission;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

public final class Parties {

    private static final long INVITE_MILLIS = 60_000;

    public static final class Party {
        private UUID leader;
        private final Set<UUID> members = new LinkedHashSet<>();

        private Party(UUID leader) {
            this.leader = leader;
            members.add(leader);
        }

        public UUID leader() {
            return leader;
        }

        public List<Player> onlineMembers() {
            return members.stream().map(MinecraftServer.getConnectionManager()::getOnlinePlayerByUuid)
                    .filter(Objects::nonNull).toList();
        }
    }

    private record Invite(Party party, long expires) {}

    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();

    public Optional<Party> partyOf(Player player) {
        return Optional.ofNullable(parties.get(player.getUuid()));
    }

    public boolean canStartMissions(Player player) {
        return partyOf(player).map(party -> party.leader().equals(player.getUuid())).orElse(true);
    }

    public List<Player> onlineMembers(Player player) {
        return partyOf(player).map(Party::onlineMembers).orElse(List.of(player));
    }

    /** Returns the message key of the error or empty on success. */
    public Optional<String> invite(Player leader, Player target) {
        if (leader.getUuid().equals(target.getUuid())) {
            return Optional.of("party.error.self");
        }
        if (parties.containsKey(target.getUuid())) {
            return Optional.of("party.error.in-party");
        }
        Party party = parties.computeIfAbsent(leader.getUuid(), Party::new);
        if (!party.leader.equals(leader.getUuid())) {
            return Optional.of("party.error.not-leader");
        }
        invites.put(target.getUuid(), new Invite(party, System.currentTimeMillis() + INVITE_MILLIS));
        return Optional.empty();
    }

    public Optional<Party> accept(Player player) {
        Invite invite = invites.remove(player.getUuid());
        if (invite == null || invite.expires() < System.currentTimeMillis() || parties.containsKey(player.getUuid())
                || !invite.party().members.contains(invite.party().leader)) {
            return Optional.empty();
        }
        invite.party().members.add(player.getUuid());
        parties.put(player.getUuid(), invite.party());
        return Optional.of(invite.party());
    }

    public void leave(Player player) {
        Party party = parties.remove(player.getUuid());
        invites.remove(player.getUuid());
        if (party == null) {
            return;
        }
        party.members.remove(player.getUuid());
        if (party.members.size() <= 1) {
            party.members.forEach(parties::remove);
            party.members.clear();
        } else if (party.leader.equals(player.getUuid())) {
            party.leader = party.members.iterator().next();
        }
    }
}

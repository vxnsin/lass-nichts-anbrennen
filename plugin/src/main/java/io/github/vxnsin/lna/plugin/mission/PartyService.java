package io.github.vxnsin.lna.plugin.mission;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Groups of players that play missions together. Invites expire after one minute. */
public final class PartyService {

    private static final long INVITE_MILLIS = 60_000;

    /** A group; the leader starts missions for everyone. */
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
            return members.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();
        }
    }

    private record Invite(Party party, long expires) {}

    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();

    public Optional<Party> partyOf(Player player) {
        return Optional.ofNullable(parties.get(player.getUniqueId()));
    }

    public boolean canStartMissions(Player player) {
        return partyOf(player).map(party -> party.leader().equals(player.getUniqueId())).orElse(true);
    }

    /** All online members of the player's group, or only the player when not in a group. */
    public List<Player> onlineMembers(Player player) {
        return partyOf(player).map(Party::onlineMembers).orElse(List.of(player));
    }

    /** Returns an error message or empty on success. */
    public Optional<String> invite(Player leader, Player target) {
        if (leader.equals(target)) {
            return Optional.of("Du kannst dich nicht selbst einladen.");
        }
        if (parties.containsKey(target.getUniqueId())) {
            return Optional.of(target.getName() + " ist schon in einer Gruppe.");
        }
        Party party = parties.computeIfAbsent(leader.getUniqueId(), id -> new Party(id));
        if (!party.leader.equals(leader.getUniqueId())) {
            return Optional.of("Nur die Gruppenleitung kann einladen.");
        }
        invites.put(target.getUniqueId(), new Invite(party, System.currentTimeMillis() + INVITE_MILLIS));
        return Optional.empty();
    }

    /** Accepts the pending invite. Returns the joined group or empty if there is no valid invite. */
    public Optional<Party> accept(Player player) {
        Invite invite = invites.remove(player.getUniqueId());
        if (invite == null || invite.expires() < System.currentTimeMillis() || parties.containsKey(player.getUniqueId())) {
            return Optional.empty();
        }
        invite.party().members.add(player.getUniqueId());
        parties.put(player.getUniqueId(), invite.party());
        return Optional.of(invite.party());
    }

    /** Leaves the group; the next member becomes leader, a group of one is dissolved. */
    public void leave(Player player) {
        Party party = parties.remove(player.getUniqueId());
        invites.remove(player.getUniqueId());
        if (party == null) {
            return;
        }
        party.members.remove(player.getUniqueId());
        if (party.members.size() <= 1) {
            party.members.forEach(parties::remove);
            party.members.clear();
        } else if (party.leader.equals(player.getUniqueId())) {
            party.leader = party.members.iterator().next();
        }
    }
}

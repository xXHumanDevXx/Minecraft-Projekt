package de.falixteleport;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Parties: Gruppen von Spielern mit Einladung. Nur im Speicher, sie gehen beim Neustart verloren. */
public class PartyManager {

    private static final class Party {
        final UUID leader;
        final Set<UUID> members = new LinkedHashSet<>();

        Party(UUID leader) {
            this.leader = leader;
            members.add(leader);
        }
    }

    private final FalixTeleport plugin;
    private final Map<UUID, Party> parties = new HashMap<>();  // Mitglied -> Party
    private final Map<UUID, UUID> invites = new HashMap<>();   // Eingeladener -> Leiter

    public PartyManager(FalixTeleport plugin) {
        this.plugin = plugin;
    }

    public void invite(Player leader, Player target) {
        if (leader.getUniqueId().equals(target.getUniqueId())) {
            leader.sendMessage(plugin.msg("party-self"));
            return;
        }
        Party party = parties.get(leader.getUniqueId());
        if (party == null) {
            party = new Party(leader.getUniqueId());
            parties.put(leader.getUniqueId(), party);
        } else if (!party.leader.equals(leader.getUniqueId())) {
            leader.sendMessage(plugin.msg("party-not-leader"));
            return;
        }
        if (parties.containsKey(target.getUniqueId())) {
            leader.sendMessage(plugin.msg("party-already", "{player}", target.getName()));
            return;
        }
        invites.put(target.getUniqueId(), leader.getUniqueId());
        target.sendMessage(plugin.msg("party-invited", "{player}", leader.getName()));
        leader.sendMessage(plugin.msg("party-invite-sent", "{player}", target.getName()));
    }

    public void accept(Player p) {
        UUID leaderId = invites.remove(p.getUniqueId());
        if (leaderId == null) {
            p.sendMessage(plugin.msg("party-no-invite"));
            return;
        }
        if (parties.containsKey(p.getUniqueId())) {
            p.sendMessage(plugin.msg("party-already-in"));
            return;
        }
        Party party = parties.get(leaderId);
        if (party == null) {
            p.sendMessage(plugin.msg("party-gone"));
            return;
        }
        party.members.add(p.getUniqueId());
        parties.put(p.getUniqueId(), party);
        notifyMembers(party, "party-joined", "{player}", p.getName());
    }

    public void leave(Player p) {
        Party party = parties.get(p.getUniqueId());
        if (party == null) {
            p.sendMessage(plugin.msg("party-not-in"));
            return;
        }
        if (party.leader.equals(p.getUniqueId())) {
            dissolve(party);
            return;
        }
        party.members.remove(p.getUniqueId());
        parties.remove(p.getUniqueId());
        p.sendMessage(plugin.msg("party-you-left"));
        notifyMembers(party, "party-left", "{player}", p.getName());
    }

    public void kick(Player leader, String name) {
        Party party = parties.get(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage(plugin.msg("party-not-in"));
            return;
        }
        if (!party.leader.equals(leader.getUniqueId())) {
            leader.sendMessage(plugin.msg("party-not-leader"));
            return;
        }
        UUID target = null;
        for (UUID id : party.members) {
            if (name(id).equalsIgnoreCase(name)) {
                target = id;
                break;
            }
        }
        if (target == null || target.equals(party.leader)) {
            leader.sendMessage(plugin.msg("player-not-found", "{player}", name));
            return;
        }
        party.members.remove(target);
        parties.remove(target);
        Player kicked = Bukkit.getPlayer(target);
        if (kicked != null) kicked.sendMessage(plugin.msg("party-you-kicked"));
        notifyMembers(party, "party-kicked", "{player}", name(target));
    }

    public void disband(Player leader) {
        Party party = parties.get(leader.getUniqueId());
        if (party == null) {
            leader.sendMessage(plugin.msg("party-not-in"));
            return;
        }
        if (!party.leader.equals(leader.getUniqueId())) {
            leader.sendMessage(plugin.msg("party-not-leader"));
            return;
        }
        dissolve(party);
    }

    public void list(Player p) {
        Party party = parties.get(p.getUniqueId());
        if (party == null) {
            p.sendMessage(plugin.msg("party-none"));
            return;
        }
        p.sendMessage(plugin.msg("party-list-header",
                "{count}", String.valueOf(party.members.size()),
                "{leader}", name(party.leader)));
        for (UUID id : party.members) {
            p.sendMessage(plugin.msg("party-list-entry", "{player}", name(id)));
        }
    }

    private void dissolve(Party party) {
        notifyMembers(party, "party-disbanded");
        for (UUID id : party.members) parties.remove(id);
        party.members.clear();
    }

    private void notifyMembers(Party party, String key, String... pairs) {
        for (UUID id : party.members) {
            Player m = Bukkit.getPlayer(id);
            if (m != null) m.sendMessage(plugin.msg(key, pairs));
        }
    }

    private String name(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "?";
    }
}

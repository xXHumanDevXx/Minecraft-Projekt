package de.falixteleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Teleport-Anfragen (/tpa, /tphere), Wartezeit mit Abbruch bei Bewegung, Warps und Homes. */
public class TeleportManager {

    /** mover wird zu dest teleportiert, sobald target zustimmt. */
    private record Request(UUID requester, UUID mover, UUID dest, long time) { }

    private record Pending(BukkitTask task, Location start) { }

    private final FalixTeleport plugin;
    private final Map<UUID, Request> requests = new HashMap<>(); // Ziel -> Anfrage
    private final Map<UUID, Pending> pending = new HashMap<>();  // Spieler mit laufendem Timer

    public TeleportManager(FalixTeleport plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  Anfragen
    // ------------------------------------------------------------------

    public void request(Player from, Player to, boolean here) {
        UUID mover = here ? to.getUniqueId() : from.getUniqueId();
        UUID dest = here ? from.getUniqueId() : to.getUniqueId();
        requests.put(to.getUniqueId(), new Request(from.getUniqueId(), mover, dest, System.currentTimeMillis()));

        if (here) {
            to.sendMessage(plugin.msg("tphere-received", "{player}", from.getName()));
            from.sendMessage(plugin.msg("tphere-sent", "{player}", to.getName()));
        } else {
            to.sendMessage(plugin.msg("tpa-received", "{player}", from.getName()));
            from.sendMessage(plugin.msg("tpa-sent", "{player}", to.getName()));
        }
        to.sendMessage(plugin.msg("request-hint"));
    }

    public void accept(Player target) {
        Request r = requests.remove(target.getUniqueId());
        if (r == null) {
            target.sendMessage(plugin.msg("request-none"));
            return;
        }
        long timeout = plugin.getConfig().getLong("settings.request-timeout", 60) * 1000L;
        if (System.currentTimeMillis() - r.time() > timeout) {
            target.sendMessage(plugin.msg("request-expired"));
            return;
        }
        Player requester = Bukkit.getPlayer(r.requester());
        Player mover = Bukkit.getPlayer(r.mover());
        Player dest = Bukkit.getPlayer(r.dest());
        if (mover == null || dest == null) {
            target.sendMessage(plugin.msg("request-expired"));
            return;
        }
        if (requester != null) {
            requester.sendMessage(plugin.msg("request-accepted", "{player}", target.getName()));
        }
        teleportDelayed(mover, dest.getLocation(), "teleported-to", "{player}", dest.getName());
    }

    public void deny(Player target) {
        Request r = requests.remove(target.getUniqueId());
        if (r == null) {
            target.sendMessage(plugin.msg("request-none"));
            return;
        }
        Player requester = Bukkit.getPlayer(r.requester());
        if (requester != null) {
            requester.sendMessage(plugin.msg("request-denied", "{player}", target.getName()));
        }
        target.sendMessage(plugin.msg("request-denied-self"));
    }

    // ------------------------------------------------------------------
    //  Warps und Homes
    // ------------------------------------------------------------------

    public void warp(Player p, String name) {
        Location l = plugin.store().getWarp(name);
        if (l == null) {
            p.sendMessage(plugin.msg("warp-not-found", "{name}", name));
            return;
        }
        teleportDelayed(p, l, "teleported-to-warp", "{name}", name);
    }

    public void home(Player p, String name) {
        Location l = plugin.store().getHome(p.getUniqueId(), name);
        if (l == null) {
            p.sendMessage(plugin.msg("home-not-found", "{name}", name));
            return;
        }
        teleportDelayed(p, l, "teleported-home", "{name}", name);
    }

    // ------------------------------------------------------------------
    //  Wartezeit
    // ------------------------------------------------------------------

    public void teleportDelayed(Player p, Location dest, String key, String... pairs) {
        cancel(p.getUniqueId());
        int delay = plugin.getConfig().getInt("settings.teleport-delay", 3);
        if (delay <= 0) {
            p.teleport(dest);
            p.sendMessage(plugin.msg(key, pairs));
            return;
        }

        UUID id = p.getUniqueId();
        p.sendMessage(plugin.msg("teleport-delay", "{seconds}", String.valueOf(delay)));
        Location start = p.getLocation().clone();

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pending.remove(id);
            Player online = Bukkit.getPlayer(id);
            if (online == null) return;
            online.teleport(dest);
            online.sendMessage(plugin.msg(key, pairs));
        }, delay * 20L);

        pending.put(id, new Pending(task, start));
    }

    /** Wird bei jeder Bewegung aufgerufen und bricht den Timer ab, sobald der Block wechselt. */
    public void checkMove(Player p, Location to) {
        Pending pd = pending.get(p.getUniqueId());
        if (pd == null) return;
        Location s = pd.start();
        if (s.getWorld() == to.getWorld() && s.getBlockX() == to.getBlockX()
                && s.getBlockY() == to.getBlockY() && s.getBlockZ() == to.getBlockZ()) return;
        cancel(p.getUniqueId());
        p.sendMessage(plugin.msg("teleport-cancelled"));
    }

    public void cancel(UUID id) {
        Pending pd = pending.remove(id);
        if (pd != null) pd.task().cancel();
    }

    /** Beim Verlassen des Servers aufräumen. */
    public void forget(UUID id) {
        cancel(id);
        requests.remove(id);
        requests.entrySet().removeIf(en -> en.getValue().requester().equals(id));
    }
}

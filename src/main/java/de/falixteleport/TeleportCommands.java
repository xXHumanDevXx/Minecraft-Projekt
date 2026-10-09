package de.falixteleport;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Alle Befehle des Plugins. */
public class TeleportCommands implements CommandExecutor, TabCompleter {

    private final FalixTeleport plugin;

    public TeleportCommands(FalixTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!(s instanceof Player p)) {
            s.sendMessage(plugin.msg("player-only"));
            return true;
        }
        switch (c.getName().toLowerCase(Locale.ROOT)) {
            case "tpa" -> tpa(p, a, false);
            case "tphere" -> {
                if (!p.hasPermission("falixteleport.admin")) noPerm(p);
                else tpa(p, a, true);
            }
            case "tpaccept" -> plugin.teleports().accept(p);
            case "tpdeny" -> plugin.teleports().deny(p);
            case "party" -> party(p, a);
            case "warp" -> warp(p, a);
            case "setwarp" -> setWarp(p, a);
            case "delwarp" -> delWarp(p, a);
            case "sethome" -> setHome(p, a);
            case "home" -> home(p, a);
            case "delhome" -> delHome(p, a);
            default -> { }
        }
        return true;
    }

    private void noPerm(Player p) {
        p.sendMessage(plugin.msg("no-permission"));
    }

    private void usage(Player p, String text) {
        p.sendMessage(plugin.msg("usage", "{usage}", text));
    }

    private void tpa(Player p, String[] a, boolean here) {
        if (a.length < 1) {
            usage(p, here ? "/tphere <Spieler>" : "/tpa <Spieler>");
            return;
        }
        Player t = Bukkit.getPlayerExact(a[0]);
        if (t == null) {
            p.sendMessage(plugin.msg("player-offline", "{player}", a[0]));
            return;
        }
        if (t.equals(p)) {
            p.sendMessage(plugin.msg("self-target"));
            return;
        }
        plugin.teleports().request(p, t, here);
    }

    private void party(Player p, String[] a) {
        String sub = a.length > 0 ? a[0].toLowerCase(Locale.ROOT) : "list";
        PartyManager pm = plugin.party();
        switch (sub) {
            case "invite" -> {
                if (a.length < 2) {
                    usage(p, "/party invite <Spieler>");
                    return;
                }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) {
                    p.sendMessage(plugin.msg("player-offline", "{player}", a[1]));
                    return;
                }
                pm.invite(p, t);
            }
            case "accept" -> pm.accept(p);
            case "leave" -> pm.leave(p);
            case "kick" -> {
                if (a.length < 2) {
                    usage(p, "/party kick <Spieler>");
                    return;
                }
                pm.kick(p, a[1]);
            }
            case "disband" -> pm.disband(p);
            case "list" -> pm.list(p);
            default -> usage(p, "/party <invite | accept | leave | kick | disband | list>");
        }
    }

    private void warp(Player p, String[] a) {
        if (a.length == 0) {
            TeleportMenu.openWarps(plugin, p);
            return;
        }
        plugin.teleports().warp(p, a[0]);
    }

    private void setWarp(Player p, String[] a) {
        if (!p.hasPermission("falixteleport.admin")) {
            noPerm(p);
            return;
        }
        if (a.length < 1) {
            usage(p, "/setwarp <Name>");
            return;
        }
        plugin.store().setWarp(a[0], p.getLocation());
        p.sendMessage(plugin.msg("warp-set", "{name}", a[0].toLowerCase(Locale.ROOT)));
    }

    private void delWarp(Player p, String[] a) {
        if (!p.hasPermission("falixteleport.admin")) {
            noPerm(p);
            return;
        }
        if (a.length < 1) {
            usage(p, "/delwarp <Name>");
            return;
        }
        if (plugin.store().delWarp(a[0])) {
            p.sendMessage(plugin.msg("warp-deleted", "{name}", a[0].toLowerCase(Locale.ROOT)));
        } else {
            p.sendMessage(plugin.msg("warp-not-found", "{name}", a[0]));
        }
    }

    private void setHome(Player p, String[] a) {
        String name = a.length > 0 ? a[0] : "home";
        int max = plugin.getConfig().getInt("settings.max-homes", 3);
        boolean exists = plugin.store().getHome(p.getUniqueId(), name) != null;
        boolean unlimited = p.hasPermission("falixteleport.admin");
        if (!exists && !unlimited && plugin.store().homes(p.getUniqueId()).size() >= max) {
            p.sendMessage(plugin.msg("home-limit", "{max}", String.valueOf(max)));
            return;
        }
        plugin.store().setHome(p.getUniqueId(), name, p.getLocation());
        p.sendMessage(plugin.msg("home-set", "{name}", name.toLowerCase(Locale.ROOT)));
    }

    private void home(Player p, String[] a) {
        if (a.length == 0) {
            TeleportMenu.openHomes(plugin, p);
            return;
        }
        plugin.teleports().home(p, a[0]);
    }

    private void delHome(Player p, String[] a) {
        if (a.length < 1) {
            usage(p, "/delhome <Name>");
            return;
        }
        if (plugin.store().delHome(p.getUniqueId(), a[0])) {
            p.sendMessage(plugin.msg("home-deleted", "{name}", a[0].toLowerCase(Locale.ROOT)));
        } else {
            p.sendMessage(plugin.msg("home-not-found", "{name}", a[0]));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        if (!(s instanceof Player p) || a.length == 0) return List.of();
        String cmd = c.getName().toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();

        if (a.length == 1) {
            switch (cmd) {
                case "party" -> options.addAll(List.of("invite", "accept", "leave", "kick", "disband", "list"));
                case "tpa", "tphere" -> onlineNames(options);
                case "warp", "delwarp" -> options.addAll(plugin.store().warps().keySet());
                case "home", "delhome" -> options.addAll(plugin.store().homes(p.getUniqueId()).keySet());
                default -> { }
            }
        } else if (a.length == 2 && cmd.equals("party")
                && (a[0].equalsIgnoreCase("invite") || a[0].equalsIgnoreCase("kick"))) {
            onlineNames(options);
        }
        return filter(options, a[a.length - 1]);
    }

    private void onlineNames(List<String> out) {
        Bukkit.getOnlinePlayers().forEach(x -> out.add(x.getName()));
    }

    private List<String> filter(List<String> options, String start) {
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(start.toLowerCase(Locale.ROOT))) out.add(o);
        }
        return out;
    }
}

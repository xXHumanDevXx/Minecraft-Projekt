package de.falixteleport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** Hauptklasse: lädt Speicher und Managers und registriert Befehle und Listener. */
public class FalixTeleport extends JavaPlugin {

    private LocationStore store;
    private TeleportManager teleports;
    private PartyManager party;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new LocationStore(this);
        store.load();
        teleports = new TeleportManager(this);
        party = new PartyManager(this);

        Bukkit.getPluginManager().registerEvents(new TeleportListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);

        TeleportCommands executor = new TeleportCommands(this);
        for (String name : List.of("tpa", "tpaccept", "tpdeny", "tphere", "party",
                "warp", "setwarp", "delwarp", "sethome", "home", "delhome")) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(executor);
                cmd.setTabCompleter(executor);
            }
        }
        getLogger().info("FalixTeleport aktiviert.");
    }

    @Override
    public void onDisable() {
        if (store != null) store.save();
    }

    public LocationStore store() {
        return store;
    }

    public TeleportManager teleports() {
        return teleports;
    }

    public PartyManager party() {
        return party;
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Nachricht aus messages.<key>, Platzhalter als Paare: "{player}", name, ... */
    public String msg(String key, String... pairs) {
        String raw = getConfig().getString("messages." + key, "&cFehlende Nachricht: " + key);
        raw = raw.replace("{prefix}", getConfig().getString("messages.prefix", ""));
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            raw = raw.replace(pairs[i], pairs[i + 1]);
        }
        return color(raw);
    }
}

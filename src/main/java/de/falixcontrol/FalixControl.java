package de.falixcontrol;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Hauptklasse: lädt die Config und registriert Listener und Befehl. */
public class FalixControl extends JavaPlugin {

    private AccessManager access;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        access = new AccessManager(this);
        access.reload();

        Bukkit.getPluginManager().registerEvents(new CommandFilterListener(this), this);

        ControlCommand executor = new ControlCommand(this);
        PluginCommand cmd = getCommand("falixcontrol");
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }
        getLogger().info("FalixControl aktiviert - Befehlssichtbarkeit ist aktiv.");
    }

    public AccessManager access() {
        return access;
    }

    /** Config neu laden und die Befehlsliste aller Online-Spieler aktualisieren. */
    public void reloadAll() {
        reloadConfig();
        access.reload();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.updateCommands();
        }
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

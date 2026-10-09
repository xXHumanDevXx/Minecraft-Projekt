package de.falixprotect;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** Hauptklasse: lädt die Config und registriert Listener und Befehl. */
public class FalixProtect extends JavaPlugin {

    private ProtectionManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new ProtectionManager(this);
        manager.load();

        Bukkit.getPluginManager().registerEvents(new ProtectListener(this), this);

        ProtectCommand executor = new ProtectCommand(this);
        PluginCommand cmd = getCommand("protection");
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }
        getLogger().info("FalixProtect aktiviert - Schutzblöcke sind aktiv.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.save();
    }

    public ProtectionManager manager() {
        return manager;
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

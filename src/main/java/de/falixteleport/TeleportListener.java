package de.falixteleport;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Bricht Teleport-Timer bei Bewegung ab und räumt beim Verlassen auf. */
public class TeleportListener implements Listener {

    private final FalixTeleport plugin;

    public TeleportListener(FalixTeleport plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        plugin.teleports().checkMove(e.getPlayer(), e.getTo());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.teleports().forget(e.getPlayer().getUniqueId());
    }
}

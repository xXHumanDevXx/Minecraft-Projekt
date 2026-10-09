package de.falixprotect;

import de.falixprotect.ProtectionManager.Protector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/** Setzt Schutzblöcke durch: Abbau und Explosionen. */
public class ProtectListener implements Listener {

    private final FalixProtect plugin;

    public ProtectListener(FalixProtect plugin) {
        this.plugin = plugin;
    }

    private ProtectionManager manager() {
        return plugin.manager();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!manager().isProtectorItem(e.getItemInHand())) return;

        manager().add(e.getBlockPlaced(), e.getPlayer());
        int radius = plugin.getConfig().getInt("settings.radius-chunks", 1);
        e.getPlayer().sendMessage(plugin.msg("placed", "{radius}", String.valueOf(radius)));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();

        // Den Schutzblock selbst abbauen: nur mit Bypass, dann wird der Bereich aufgehoben
        Protector self = manager().at(b);
        if (self != null) {
            if (p.hasPermission("falixprotect.bypass")) {
                manager().remove(self);
                p.sendMessage(plugin.msg("removed"));
            } else {
                e.setCancelled(true);
                p.sendMessage(plugin.msg("blocked"));
            }
            return;
        }

        if (manager().blocking(b, p) != null) {
            e.setCancelled(true);
            p.sendMessage(plugin.msg("blocked"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (!plugin.getConfig().getBoolean("settings.protect-explosions", true)) return;
        e.blockList().removeIf(b -> manager().blocking(b, null) != null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (!plugin.getConfig().getBoolean("settings.protect-explosions", true)) return;
        e.blockList().removeIf(b -> manager().blocking(b, null) != null);
    }
}

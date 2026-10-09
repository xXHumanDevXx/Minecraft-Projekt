package de.falixteleport;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Klicks im Warp- und Home-Menü. */
public class MenuListener implements Listener {

    private final FalixTeleport plugin;

    public MenuListener(FalixTeleport plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof TeleportMenu.Holder holder)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getInventory()) return;

        String name = holder.entries().get(e.getSlot());
        if (name == null) return;

        if (holder.kind() == TeleportMenu.Kind.HOME && e.isRightClick()) {
            plugin.store().delHome(p.getUniqueId(), name);
            p.sendMessage(plugin.msg("home-deleted", "{name}", name));
            TeleportMenu.openHomes(plugin, p);
            return;
        }
        if (!e.isLeftClick()) return;

        p.closeInventory();
        if (holder.kind() == TeleportMenu.Kind.WARP) {
            plugin.teleports().warp(p, name);
        } else {
            plugin.teleports().home(p, name);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof TeleportMenu.Holder) {
            e.setCancelled(true);
        }
    }
}

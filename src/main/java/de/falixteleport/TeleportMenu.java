package de.falixteleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Eigenes Inventar-Menü für /warp und /home. */
public final class TeleportMenu {

    public enum Kind { WARP, HOME }

    /** Kennzeichnet unser Menü, damit Klicks eindeutig zugeordnet werden können. */
    public static final class Holder implements InventoryHolder {
        private final Kind kind;
        private final Map<Integer, String> entries = new HashMap<>();
        private Inventory inventory;

        Holder(Kind kind) {
            this.kind = kind;
        }

        public Kind kind() {
            return kind;
        }

        public Map<Integer, String> entries() {
            return entries;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private TeleportMenu() { }

    public static void openWarps(FalixTeleport plugin, Player p) {
        Map<String, Location> warps = plugin.store().warps();
        if (warps.isEmpty()) {
            p.sendMessage(plugin.msg("warp-none"));
            return;
        }
        build(plugin, p, Kind.WARP, warps);
    }

    public static void openHomes(FalixTeleport plugin, Player p) {
        Map<String, Location> homes = plugin.store().homes(p.getUniqueId());
        if (homes.isEmpty()) {
            p.sendMessage(plugin.msg("home-none"));
            return;
        }
        build(plugin, p, Kind.HOME, homes);
    }

    private static void build(FalixTeleport plugin, Player p, Kind kind, Map<String, Location> entries) {
        Holder holder = new Holder(kind);
        String title = plugin.color(plugin.getConfig().getString(
                kind == Kind.WARP ? "ui.warp-title" : "ui.home-title", "&8Menü"));
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.inventory = inv;

        // Ganzes Menü mit Rahmen füllen, Einträge werden danach darübergesetzt
        ItemStack filler = named(material(plugin, "ui.filler", Material.GRAY_STAINED_GLASS_PANE), " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        Material icon = kind == Kind.WARP
                ? material(plugin, "ui.warp-icon", Material.ENDER_PEARL)
                : material(plugin, "ui.home-icon", Material.RED_BED);

        int slot = 0;
        for (Map.Entry<String, Location> en : entries.entrySet()) {
            if (slot >= 45) break; // untere Reihe bleibt Rahmen
            Location l = en.getValue();

            List<String> lore = new ArrayList<>();
            lore.add(plugin.color("&8Welt: &7" + l.getWorld().getName()));
            lore.add(plugin.color("&8Ort: &7" + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ()));
            lore.add("");
            lore.add(plugin.color("&aLinksklick: &7teleportieren"));
            if (kind == Kind.HOME) lore.add(plugin.color("&cRechtsklick: &7löschen"));

            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(plugin.color("&b" + en.getKey()));
            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot, item);
            holder.entries.put(slot, en.getKey());
            slot++;
        }
        p.openInventory(inv);
    }

    private static ItemStack named(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        it.setItemMeta(meta);
        return it;
    }

    private static Material material(FalixTeleport plugin, String path, Material def) {
        Material m = Material.matchMaterial(plugin.getConfig().getString(path, ""));
        return m == null ? def : m;
    }
}

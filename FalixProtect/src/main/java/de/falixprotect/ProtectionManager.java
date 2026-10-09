package de.falixprotect;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** Verwaltet die Schutzblöcke und speichert sie in plugins/FalixProtect/data.yml */
public class ProtectionManager {

    public record Protector(String world, int x, int y, int z, UUID owner, String ownerName) {

        public boolean sameBlock(Block b) {
            return b.getX() == x && b.getY() == y && b.getZ() == z
                    && b.getWorld().getName().equals(world);
        }
    }

    private final FalixProtect plugin;
    private final NamespacedKey key;
    private final File file;
    private final List<Protector> protectors = new CopyOnWriteArrayList<>();

    public ProtectionManager(FalixProtect plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "protector");
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    // ------------------------------------------------------------------
    //  Schutzblock-Item
    // ------------------------------------------------------------------

    public ItemStack createItem() {
        var cfg = plugin.getConfig();
        Material mat = Material.matchMaterial(cfg.getString("settings.item-material", "BEACON"));
        if (mat == null) mat = Material.BEACON;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.color(cfg.getString("item.name", "&6Schutzblock")));

        List<String> lore = new ArrayList<>();
        for (String line : cfg.getStringList("item.lore")) lore.add(plugin.color(line));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "protector");
        item.setItemMeta(meta);
        return item;
    }

    public boolean isProtectorItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    // ------------------------------------------------------------------
    //  Schutzblöcke verwalten
    // ------------------------------------------------------------------

    public void add(Block b, Player owner) {
        protectors.add(new Protector(b.getWorld().getName(), b.getX(), b.getY(), b.getZ(),
                owner.getUniqueId(), owner.getName()));
        save();
    }

    public Protector at(Block b) {
        for (Protector p : protectors) {
            if (p.sameBlock(b)) return p;
        }
        return null;
    }

    public void remove(Protector p) {
        protectors.remove(p);
        save();
    }

    public List<Protector> all() {
        return List.copyOf(protectors);
    }

    /**
     * Gibt den Schutzblock zurück, der diesen Block für den Spieler sperrt, sonst null.
     * Mit p == null (Explosionen) gilt kein Bypass.
     */
    public Protector blocking(Block b, Player p) {
        if (p != null && p.hasPermission("falixprotect.bypass")) return null;

        int r = plugin.getConfig().getInt("settings.radius-chunks", 1);
        boolean ownerBypass = plugin.getConfig().getBoolean("settings.owner-bypass", false);
        int cx = b.getX() >> 4;
        int cz = b.getZ() >> 4;
        String world = b.getWorld().getName();

        for (Protector pr : protectors) {
            if (!pr.world().equals(world)) continue;
            if (Math.abs((pr.x() >> 4) - cx) > r) continue;
            if (Math.abs((pr.z() >> 4) - cz) > r) continue;
            if (p != null && ownerBypass && p.getUniqueId().equals(pr.owner())) continue;
            return pr;
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  Laden / Speichern
    // ------------------------------------------------------------------

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        for (String line : data.getStringList("protectors")) {
            String[] parts = line.split(";");
            if (parts.length < 6) continue;
            try {
                protectors.add(new Protector(parts[0],
                        Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                        UUID.fromString(parts[4]), parts[5]));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public synchronized void save() {
        List<String> out = new ArrayList<>();
        for (Protector p : protectors) {
            out.add(p.world() + ";" + p.x() + ";" + p.y() + ";" + p.z() + ";"
                    + p.owner() + ";" + p.ownerName());
        }
        YamlConfiguration data = new YamlConfiguration();
        data.set("protectors", out);
        try {
            plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("data.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}

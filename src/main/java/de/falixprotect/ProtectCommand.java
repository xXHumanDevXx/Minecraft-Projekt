package de.falixprotect;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /protection            -> Schutzblock erhalten
 *  /protection list       -> alle Schutzblöcke anzeigen (Admin)
 *  /protection reload     -> Config neu laden (Admin) */
public class ProtectCommand implements CommandExecutor, TabCompleter {

    private final FalixProtect plugin;

    public ProtectCommand(FalixProtect plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            giveItem(s);
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                if (!s.hasPermission("falixprotect.admin")) return noPerm(s);
                list(s);
            }
            case "reload" -> {
                if (!s.hasPermission("falixprotect.admin")) return noPerm(s);
                plugin.reloadConfig();
                s.sendMessage(plugin.msg("reload-done"));
            }
            default -> s.sendMessage(plugin.msg("usage", "{usage}", "/protection [list | reload]"));
        }
        return true;
    }

    private void giveItem(CommandSender s) {
        if (!(s instanceof Player p)) {
            s.sendMessage(plugin.msg("usage", "{usage}", "/protection [list | reload]"));
            return;
        }
        ItemStack item = plugin.manager().createItem();
        Map<Integer, ItemStack> left = p.getInventory().addItem(item);
        if (left.isEmpty()) {
            p.sendMessage(plugin.msg("received"));
        } else {
            left.values().forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
            p.sendMessage(plugin.msg("inventory-full"));
        }
    }

    private void list(CommandSender s) {
        List<ProtectionManager.Protector> all = plugin.manager().all();
        if (all.isEmpty()) {
            s.sendMessage(plugin.msg("list-none"));
            return;
        }
        s.sendMessage(plugin.msg("list-header", "{count}", String.valueOf(all.size())));
        for (ProtectionManager.Protector p : all) {
            s.sendMessage(plugin.msg("list-entry",
                    "{world}", p.world(),
                    "{x}", String.valueOf(p.x()),
                    "{y}", String.valueOf(p.y()),
                    "{z}", String.valueOf(p.z()),
                    "{owner}", p.ownerName()));
        }
    }

    private boolean noPerm(CommandSender s) {
        s.sendMessage(plugin.color("&cDazu hast du keine Berechtigung."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        if (a.length != 1) return List.of();
        List<String> out = new ArrayList<>();
        for (String o : List.of("list", "reload")) {
            if (o.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(o);
        }
        return out;
    }
}

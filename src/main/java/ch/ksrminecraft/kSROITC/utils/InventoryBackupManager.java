package ch.ksrminecraft.kSROITC.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryBackupManager {

    private static final Map<UUID, ItemStack[]> inventories = new HashMap<>();

    public static void saveInventory(Player p) {
        inventories.put(p.getUniqueId(), p.getInventory().getContents().clone());
    }

    public static void restoreInventory(Player p) {
        UUID id = p.getUniqueId();
        ItemStack[] items = inventories.remove(id);
        if (items == null) return; // Nur wiederherstellen, wenn gespeichert war
        p.getInventory().setContents(items);
    }

    public static void clear(Player p) {
        inventories.remove(p.getUniqueId());
    }
}

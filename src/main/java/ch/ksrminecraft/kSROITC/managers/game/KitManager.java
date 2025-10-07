package ch.ksrminecraft.kSROITC.managers.game;

import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class KitManager {

    private final Set<UUID> noKitNextRespawn = new HashSet<>();

    public void giveKit(Player p, boolean giveSword) {
        p.getInventory().clear();

        // 🏹 Bogen – 1 Pfeil, kein Infinity
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.setDisplayName("§7Präzisionsbogen");
        bowMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        bow.setItemMeta(bowMeta);
        p.getInventory().addItem(bow);
        p.getInventory().addItem(new ItemStack(Material.ARROW, 1));

        // ⚔️ Steinschwert mit reduziertem Schaden
        if (giveSword) {
            ItemStack sword = new ItemStack(Material.STONE_SWORD);
            ItemMeta meta = sword.getItemMeta();
            meta.setDisplayName("§7Steinschwert");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            sword.setItemMeta(meta);
            p.getInventory().addItem(sword);
        }

        p.getInventory().setArmorContents(null);
        p.updateInventory();
    }

    public void giveKitWithoutArrow(Player p, boolean giveSword) {
        p.getInventory().clear();

        // Bogen ohne Pfeil
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.setDisplayName("§7Präzisionsbogen");
        bowMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        bow.setItemMeta(bowMeta);
        p.getInventory().addItem(bow);

        // Schwert (optional)
        if (giveSword) {
            ItemStack sword = new ItemStack(Material.STONE_SWORD);
            ItemMeta meta = sword.getItemMeta();
            meta.setDisplayName("§7Steinschwert");
            AttributeModifier dmgModifier = new AttributeModifier(
                    new NamespacedKey("ksroitc", "reduced_damage"),
                    -3.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
            );
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, dmgModifier);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            sword.setItemMeta(meta);
            p.getInventory().addItem(sword);
        }

        p.updateInventory();
        Dbg.d(KitManager.class, "giveKitWithoutArrow -> " + p.getName());
    }


    public void giveOneArrow(Player p) {
        p.getInventory().addItem(new ItemStack(Material.ARROW, 1));
        Dbg.d(KitManager.class, "giveOneArrow -> " + p.getName());
    }

    /** Merkt sich, dass ein Spieler beim nächsten Respawn kein Kit erhalten soll. */
    public void markNoKitNextRespawn(Player p) {
        if (p != null) noKitNextRespawn.add(p.getUniqueId());
    }

    /** Prüft und entfernt das „kein-Kit-Flag“. */
    public boolean shouldSkipKit(Player p) {
        return p != null && noKitNextRespawn.remove(p.getUniqueId());
    }
}

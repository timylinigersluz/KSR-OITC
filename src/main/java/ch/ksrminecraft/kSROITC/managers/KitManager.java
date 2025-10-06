package ch.ksrminecraft.kSROITC.managers;

import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class KitManager {

    public void giveKit(Player p, boolean withSword) {
        Dbg.d(KitManager.class, "giveKit -> " + p.getName() + " withSword=" + withSword);
        PlayerInventory inv = p.getInventory();
        inv.clear();
        p.setGameMode(GameMode.SURVIVAL);

        ItemStack bow = new ItemStack(Material.BOW, 1);
        ItemMeta bm = bow.getItemMeta();
        bm.setUnbreakable(true);
        bow.setItemMeta(bm);
        inv.setItem(0, bow);

        if (withSword) {
            ItemStack sword = new ItemStack(Material.STONE_SWORD, 1);
            ItemMeta sm = sword.getItemMeta();
            sm.setUnbreakable(true);
            sword.setItemMeta(sm);
            inv.setItem(1, sword);
        }

        giveOneArrow(p);
    }

    public void giveOneArrow(Player p) {
        p.getInventory().addItem(new ItemStack(Material.ARROW, 1));
        Dbg.d(KitManager.class, "giveOneArrow -> " + p.getName());
    }
}

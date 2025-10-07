package ch.ksrminecraft.kSROITC.managers.match;

import ch.ksrminecraft.kSROITC.KSROITC;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.List;

public class CelebrationManager {

    private final KSROITC plugin;

    public CelebrationManager(KSROITC plugin) {
        this.plugin = plugin;
    }

    /**
     * Startet eine kurze Siegesfeier für alle Gewinner.
     */
    public void celebrateWinners(List<Player> winners) {
        if (winners == null || winners.isEmpty()) return;

        for (Player p : winners) {
            if (p == null || !p.isOnline()) continue;

            p.sendTitle(ChatColor.GREEN + "🏆 Sieg!", ChatColor.YELLOW + "Du hast gewonnen!", 10, 60, 10);
            p.sendMessage("§a[OITC] Glückwunsch, du hast gewonnen!");
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            launchFireworks(p);
        }
    }

    /**
     * Lässt mehrere Feuerwerke beim Spieler aufsteigen.
     */
    private void launchFireworks(Player player) {
        for (int i = 0; i < 3; i++) {
            int delay = i * 20; // alle 20 Ticks (1 Sekunde)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location loc = player.getLocation().add(0, 1, 0);
                Firework firework = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);

                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.LIME, Color.AQUA, Color.YELLOW)
                        .withFade(Color.WHITE)
                        .trail(true)
                        .flicker(true)
                        .build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
            }, delay);
        }
    }

    /**
     * Sound für Verlierer (optionaler Einsatz).
     */
    public void playLoserSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        } catch (Throwable ignored) {}
    }
}

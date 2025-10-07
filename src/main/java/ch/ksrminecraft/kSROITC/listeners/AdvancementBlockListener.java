package ch.ksrminecraft.kSROITC.listeners;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.HashSet;

/**
 * Blockiert alle Minecraft-Advancements global:
 * - Keine Chatmeldung (Paper: event.message(null))
 * - Bereits vergebene Kriterien werden wieder entzogen
 */
public class AdvancementBlockListener implements Listener {

    private final KSROITC plugin;

    public AdvancementBlockListener(KSROITC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();

        // Erfolgsmeldung vollständig unterdrücken (Paper 1.21+)
        event.message(null);

        // Bereits vergebene Kriterien wieder entziehen, damit es nicht "erledigt" bleibt
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        for (String c : new HashSet<>(progress.getAwardedCriteria())) {
            progress.revokeCriteria(c);
        }

        // Debug
        Dbg.d(getClass(), "Advancement blockiert für " + player.getName()
                + " → " + advancement.getKey());
    }
}

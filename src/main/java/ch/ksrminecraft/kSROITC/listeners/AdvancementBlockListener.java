package ch.ksrminecraft.kSROITC.listeners;

import ch.ksrminecraft.kSROITC.KSROITC;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Unterdrückt ALLE Advancements (nicht nur Rezepte).
 * --------------------------------------------------
 * Kein Spieler erhält Fortschritte oder Popup-Meldungen.
 * Keine Logeinträge, kein Chatspam – läuft vollständig im Hintergrund.
 *
 * Wird von Paper/Bukkit-Events ausgelöst, sobald ein Advancement vergeben würde.
 * Der Listener entfernt es sofort wieder.
 */
public class AdvancementBlockListener implements Listener {

    private final KSROITC plugin;

    public AdvancementBlockListener(KSROITC plugin) {
        this.plugin = plugin;
    }

    private final Set<String> suppressed = new HashSet<>();

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        Advancement adv = event.getAdvancement();
        String key = adv.getKey().toString();
        if (!suppressed.add(key)) return; // nur einmal pro Key innerhalb der Laufzeit

        Player p = event.getPlayer();
        p.getAdvancementProgress(adv).getAwardedCriteria().forEach(c -> {
            p.getAdvancementProgress(adv).revokeCriteria(c);
        });
    }
}

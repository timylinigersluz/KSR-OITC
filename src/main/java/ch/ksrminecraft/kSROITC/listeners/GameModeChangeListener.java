package ch.ksrminecraft.kSROITC.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

/**
 * Unterdrückt die Standardmeldung von Minecraft,
 * wenn sich der Spielmodus eines Spielers ändert.
 */
public class GameModeChangeListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        // Standardmeldung ("Dein Spielmodus wurde auf ... gesetzt") unterdrücken
        e.setCancelled(false); // Änderung selbst erlauben
        e.getPlayer().sendMessage(""); // leere Zeile → überschreibt Standardmeldung
    }
}

package ch.ksrminecraft.kSROITC.listeners;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Optional;

public class CommandBlockListener implements Listener {

    private final KSROITC plugin;

    public CommandBlockListener(KSROITC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String msg = e.getMessage().toLowerCase();

        // 🛡Admins mit oitc.bypass dürfen immer
        if (p.hasPermission("oitc.bypass")) return;

        // Prüfen, ob Spieler in einem laufenden OITC-Spiel ist
        Optional<GameSession> sopt = plugin.getGameManager().getSessionManager().byPlayer(p);
        boolean inGame = sopt.isPresent() && sopt.get().getState() == GameState.RUNNING;

        if (!inGame) return;

        // Blockiere alle Varianten von /kill während des Spiels
        if (msg.startsWith("/kill") || msg.startsWith("/minecraft:kill") || msg.startsWith("/suicide")) {
            e.setCancelled(true);

            // Sound-Feedback
            try {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            } catch (Throwable ignored) {}

            // Hinweis
            p.sendMessage("§7[OITC] §cTötungsbefehle sind während des Spiels deaktiviert. " +
                    "Nutze §e/oitc leave§c, um das Spiel regulär zu verlassen.");

            // Debug-Ausgabe
            Dbg.d(CommandBlockListener.class,
                    "blocked kill command by " + p.getName() + " (" + msg + ") in arena "
                            + sopt.map(s -> s.getArena().getName()).orElse("?"));
            return;
        }
    }
}

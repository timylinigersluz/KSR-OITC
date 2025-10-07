package ch.ksrminecraft.kSROITC.managers.game;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.integration.RankPointsHook;
import ch.ksrminecraft.kSROITC.managers.arena.TeleportManager;
import ch.ksrminecraft.kSROITC.managers.match.MatchManager;
import ch.ksrminecraft.kSROITC.managers.system.CountdownManager;
import ch.ksrminecraft.kSROITC.models.*;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Verantwortlich für das Verlassen einer Arena.
 * - Rückgabe von Inventar
 * - Abbruch von Countdown oder Match bei zu wenigen Spielern
 * - Entfernen von Spectator-Zustand
 */
public class PlayerLeaveHandler {

    private final KSROITC plugin;
    private final TeleportManager tp;
    private final RankPointsHook rankpoints;
    private final SessionManager sessions;
    private final CountdownManager countdowns;
    private final MatchManager match;
    private final SpectatorManager spectators;

    public PlayerLeaveHandler(KSROITC plugin,
                              TeleportManager tp,
                              RankPointsHook rankpoints,
                              SessionManager sessions,
                              CountdownManager countdowns,
                              MatchManager match,
                              SpectatorManager spectators) {
        this.plugin = plugin;
        this.tp = tp;
        this.rankpoints = rankpoints;
        this.sessions = sessions;
        this.countdowns = countdowns;
        this.match = match;
        this.spectators = spectators;
    }

    // ============================================================
    // SPIELER VERLÄSST ARENA
    // ============================================================
    public void handleLeave(Player p) {
        Optional<GameSession> opt = sessions.byPlayer(p);
        if (opt.isEmpty()) {
            p.sendMessage("§cDu bist in keiner aktiv laufenden Arena.");
            return;
        }

        GameSession s = opt.get();
        Arena a = s.getArena();
        boolean wasSpectator = spectators.isSpectator(p);

        // --- Spieler aus Session entfernen ---
        sessions.removePlayer(p, s);
        spectators.setSpectator(p, false); // sicher entfernen

        // --- Teleport in Hauptlobby ---
        try {
            tp.toMainLobby(p);
        } catch (Exception ex) {
            plugin.getLogger().warning("[OITC] Teleport zur MainLobby fehlgeschlagen für " + p.getName() + ": " + ex.getMessage());
        }

        // --- Inventar wiederherstellen ---
        try {
            ch.ksrminecraft.kSROITC.utils.InventoryBackupManager.restoreInventory(p);
        } catch (Exception ex) {
            plugin.getLogger().warning("[OITC] Inventar konnte für " + p.getName() + " nicht wiederhergestellt werden: " + ex.getMessage());
        }

        // --- GameMode-Sicherheit ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && p.getGameMode() == GameMode.SPECTATOR) {
                p.setGameMode(GameMode.SURVIVAL);
                Dbg.d(PlayerLeaveHandler.class, "Gamemode korrigiert (SPECTATOR → SURVIVAL) für " + p.getName());
            }
        }, 2L);

        // --- Nachricht für aktive Spieler ---
        if (!wasSpectator) {
            p.sendMessage("§7[OITC] §cDu hast die Arena §e" + a.getName() + " §cverlassen.");
        } else {
            p.sendMessage("§7[OITC] Du hast das Zuschauen in §e" + a.getName() + " §7beendet.");
        }

        plugin.getSignManager().updateAllSigns();

        // --- Countdown abbrechen, wenn zu wenige Spieler ---
        if (s.getState() == GameState.COUNTDOWN && sessions.getActiveCount(s) < a.getMinPlayers()) {
            countdowns.cancel(s, "zu wenige Spieler");
            for (Player pl : sessions.getActivePlayers(s)) {
                pl.sendMessage("§cZu wenige Spieler – Countdown abgebrochen.");
            }
        }

        // --- Laufendes Spiel abbrechen, wenn zu wenige ---
        if (s.getState() == GameState.RUNNING && sessions.getActiveCount(s) < a.getMinPlayers()) {
            plugin.getLogger().info("[OITC] Spiel '" + a.getName() + "' wird wegen zu weniger Spieler beendet.");

            for (Player pl : sessions.getActivePlayers(s)) {
                pl.sendMessage("§7[OITC] Runde beendet, da zu wenige Spieler übrig sind.");
            }

            if (rankpoints != null && rankpoints.isEnabled()) {
                try {
                    rankpoints.commitSessionPoints();
                } catch (Exception ex) {
                    plugin.getLogger().warning("[OITC] Fehler bei Punkte-Speicherung: " + ex.getMessage());
                }
            }

            match.stop(s, false);
            sessions.clearMappings(s);
            Dbg.d(PlayerLeaveHandler.class, "Match gestoppt, da zu wenige Spieler.");
            return;
        }

        // --- Session aufräumen, wenn komplett leer ---
        if (sessions.getActiveCount(s) == 0 && s.getPlayers().isEmpty()) {
            countdowns.cancel(s, "keine Spieler mehr");
            match.stop(s, false);
            sessions.clearMappings(s);
            Dbg.d(PlayerLeaveHandler.class, "Arena " + a.getName() + " geleert und gestoppt (keine Spieler mehr).");
        }

        Dbg.d(PlayerLeaveHandler.class, p.getName() + " hat Arena " + a.getName() + " verlassen (Spectator=" + wasSpectator + ")");
    }
}

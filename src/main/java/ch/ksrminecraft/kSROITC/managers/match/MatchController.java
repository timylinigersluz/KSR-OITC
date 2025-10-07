package ch.ksrminecraft.kSROITC.managers.match;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.game.SessionManager;
import ch.ksrminecraft.kSROITC.managers.system.CountdownManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import ch.ksrminecraft.kSROITC.utils.Dbg;

/**
 * Verwaltet das Starten und Stoppen von Matches (GameSessions).
 * Kapselt Logik aus dem GameManager zur besseren Übersicht.
 */
public class MatchController {

    private final KSROITC plugin;
    private final SessionManager sessions;
    private final MatchManager match;
    private final CountdownManager countdowns;

    public MatchController(KSROITC plugin,
                           SessionManager sessions,
                           MatchManager match,
                           CountdownManager countdowns) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.match = match;
        this.countdowns = countdowns;
    }

    // ============================================================
    // START
    // ============================================================
    public void startMatch(String arenaName) {
        Dbg.d(MatchController.class, "startMatch(" + arenaName + ")");
        Arena a = plugin.getGameManager().getArenaManager().get(arenaName);
        if (a == null) return;

        GameSession s = sessions.ensure(a);
        if (s.getState() == GameState.COUNTDOWN) {
            countdowns.cancel(s, "start()");
        }

        match.start(s);
        plugin.getSignManager().updateAllSigns();
    }

    // ============================================================
    // STOP
    // ============================================================
    public void stopMatch(String arenaName) {
        Dbg.d(MatchController.class, "stopMatch(" + arenaName + ")");
        sessions.byArena(arenaName).ifPresent(s -> {
            countdowns.cancel(s, "stop()");
            match.stop(s, false);
            sessions.clearMappings(s);
        });

        plugin.getSignManager().updateAllSigns();
    }
}

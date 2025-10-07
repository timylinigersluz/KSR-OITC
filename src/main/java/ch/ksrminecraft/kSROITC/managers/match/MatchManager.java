package ch.ksrminecraft.kSROITC.managers.match;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.integration.RankPointsHook;
import ch.ksrminecraft.kSROITC.managers.arena.TeleportManager;
import ch.ksrminecraft.kSROITC.managers.game.KitManager;
import ch.ksrminecraft.kSROITC.managers.game.ScoreboardManager;
import ch.ksrminecraft.kSROITC.managers.game.SessionManager;
import ch.ksrminecraft.kSROITC.models.*;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Hauptklasse zur Steuerung von Spielrunden.
 * Koordiniert Start/Stop und delegiert Tick-Logik und Spielende
 * an spezialisierte Untermanager.
 */
public class MatchManager {

    private final KSROITC plugin;
    private final TeleportManager tp;
    private final KitManager kits;
    private final SessionManager sessions;
    private final ScoreboardManager scoreboards;
    private final RankPointsHook rankpoints;

    private final MatchRuntimeManager runtime;
    private final MatchEndManager endManager;

    public MatchManager(KSROITC plugin,
                        TeleportManager tp,
                        KitManager kits,
                        SessionManager sessions,
                        ScoreboardManager scoreboards,
                        RankPointsHook rankpoints) {
        this.plugin = plugin;
        this.tp = tp;
        this.kits = kits;
        this.sessions = sessions;
        this.scoreboards = scoreboards;
        this.rankpoints = rankpoints;

        this.runtime = new MatchRuntimeManager(plugin, sessions, scoreboards);
        this.endManager = new MatchEndManager(plugin, sessions, scoreboards, rankpoints);
    }

    // ============================================================
    // MATCH START
    // ============================================================
    public void start(GameSession s) {
        Arena a = s.getArena();
        World w = Bukkit.getWorld(a.getWorldName());
        if (w == null || a.getSpawns().isEmpty()) {
            Dbg.d(MatchManager.class, "start: invalid world/spawns");
            return;
        }
        if (s.getState() == GameState.RUNNING) return;
        if (sessions.getActiveCount(s) < a.getMinPlayers()) return;

        s.setState(GameState.RUNNING);
        long endTs = a.getMaxSeconds() > 0
                ? System.currentTimeMillis() + a.getMaxSeconds() * 1000L
                : -1;
        s.setEndTimestamp(endTs);
        s.setCountdownEndTimestamp(-1L);

        int teleported = 0;
        for (Player p : sessions.getActivePlayers(s)) {
            tp.toRandomSpawn(p, a);
            if (!kits.shouldSkipKit(p)) {
                kits.giveKit(p, a.isGiveSword());
            }
            scoreboards.apply(p, s);
            teleported++;
        }

        plugin.getSignManager().updateAllSigns();
        Dbg.d(MatchManager.class, "start: players=" + teleported + ", endTs=" + endTs);

        runtime.startTimer(s);
    }

    // ============================================================
    // MATCH STOP
    // ============================================================
    public void stop(GameSession s, boolean showMsg) {
        runtime.stopTimer(s);
        endManager.resetArena(s, showMsg);
        plugin.getSignManager().updateAllSigns();
    }

    // ============================================================
    // DELEGATIONEN
    // ============================================================
    public void endWithWinners(GameSession s, String reason) {
        endManager.handleMatchEnd(s, reason);
    }

    public MatchRuntimeManager getRuntime() {
        return runtime;
    }

    public MatchEndManager getEndManager() {
        return endManager;
    }
}

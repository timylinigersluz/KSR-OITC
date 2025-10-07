package ch.ksrminecraft.kSROITC.managers.match;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.game.ScoreboardManager;
import ch.ksrminecraft.kSROITC.managers.game.SessionManager;
import ch.ksrminecraft.kSROITC.models.*;
import ch.ksrminecraft.kSROITC.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

/**
 * Verantwortlich für Tick-Logik (Bossbar, Restzeit, automatische Beendigung).
 */
class MatchRuntimeManager {

    private final KSROITC plugin;
    private final SessionManager sessions;
    private final ScoreboardManager scoreboards;
    private final Map<String, BossBar> matchBars = new HashMap<>();

    public MatchRuntimeManager(KSROITC plugin, SessionManager sessions, ScoreboardManager scoreboards) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.scoreboards = scoreboards;
    }

    public void startTimer(GameSession s) {
        long endTs = s.getEndTimestamp();
        if (endTs <= 0) return;

        Arena a = s.getArena();
        long total = a.getMaxSeconds();
        BossBar bar = Bukkit.createBossBar("⌛ Restzeit", BarColor.GREEN, BarStyle.SEGMENTED_20);
        matchBars.put(a.getName().toLowerCase(Locale.ROOT), bar);

        for (Player p : sessions.getActivePlayers(s)) bar.addPlayer(p);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(s), 20L, 20L);
        s.setTaskId(task.getTaskId());
    }

    public void stopTimer(GameSession s) {
        if (s.getTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(s.getTaskId());
            s.setTaskId(-1);
        }
        BossBar bar = matchBars.remove(s.getArena().getName().toLowerCase(Locale.ROOT));
        if (bar != null) bar.removeAll();
    }

    private void tick(GameSession s) {
        if (s.getState() != GameState.RUNNING) return;

        long left = (s.getEndTimestamp() - System.currentTimeMillis()) / 1000L;
        if (s.getEndTimestamp() > 0 && left <= 0) {
            plugin.getGameManager().getMatchManager().endWithWinners(s, "time");
            return;
        }

        scoreboards.updateAll(s);

        Arena a = s.getArena();
        BossBar bar = matchBars.get(a.getName().toLowerCase(Locale.ROOT));
        if (bar != null && s.getEndTimestamp() > 0) {
            bar.setTitle("⌛ Restzeit: " + TimeUtil.formatCompact(left));
            double prog = (a.getMaxSeconds() <= 0) ? 1.0 : Math.max(0.0, Math.min(1.0, left / (double) a.getMaxSeconds()));
            bar.setProgress(prog);

            if (prog > 0.5) bar.setColor(BarColor.GREEN);
            else if (prog > 0.2) bar.setColor(BarColor.YELLOW);
            else bar.setColor(BarColor.RED);
        }
    }
}

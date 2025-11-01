package ch.ksrminecraft.kSROITC.managers.system;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.*;

public class CountdownManager {

    private final KSROITC plugin;
    private final Map<String, BossBar> bars = new HashMap<>();
    private final Map<String, Integer> runningTasks = new HashMap<>();

    public CountdownManager(KSROITC plugin) { this.plugin = plugin; }

    /**
     * Startet einen Countdown (Lobby oder Match)
     */
    public void start(GameSession s, int seconds, Runnable onFinish) {
        // Bereits laufender Countdown? -> abbrechen
        cancel(s, "neuer Countdown gestartet");

        if (s.getState() != GameState.IDLE && s.getState() != GameState.COUNTDOWN) return;
        s.setState(GameState.COUNTDOWN);

        BossBar bar = Bukkit.createBossBar("Start in " + seconds + "s", BarColor.BLUE, BarStyle.SEGMENTED_20);
        String key = s.getArena().getName().toLowerCase(Locale.ROOT);
        bars.put(key, bar);

        s.getPlayers().forEach(u -> {
            Player p = Bukkit.getPlayer(u);
            if (p != null) bar.addPlayer(p);
        });

        bar.setProgress(1.0);
        s.setCountdownEndTimestamp(System.currentTimeMillis() + seconds * 1000L);
        plugin.getSignManager().updateAllSigns();

        final int total = seconds;
        final int[] left = {seconds};

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (s.getState() != GameState.COUNTDOWN) { cancel(s, "state change"); return; }
            if (s.getPlayers().size() < s.getArena().getMinPlayers()) { cancel(s, "zu wenige Spieler"); return; }

            left[0]--;
            bar.setTitle("Start in " + left[0] + "s");
            bar.setProgress(Math.max(0.0, Math.min(1.0, left[0] / (double) total)));

            if (left[0] <= 0) {
                cancel(s, "done");
                onFinish.run();
            }
        }, 20L, 20L);

        runningTasks.put(key, taskId);
        s.setTaskId(taskId);
        Dbg.d(CountdownManager.class, "countdown start arena=" + s.getArena().getName() + " seconds=" + seconds + " task=" + taskId);
    }

    /**
     * Bricht den Countdown ab und entfernt die BossBar.
     */
    public void cancel(GameSession s, String reason) {
        String key = s.getArena().getName().toLowerCase(Locale.ROOT);

        // Task stoppen
        Integer taskId = runningTasks.remove(key);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        if (s.getTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(s.getTaskId());
            s.setTaskId(-1);
        }

        // BossBar entfernen
        BossBar bar = bars.remove(key);
        if (bar != null) bar.removeAll();

        // Status zurücksetzen
        s.setCountdownEndTimestamp(-1L);
        if (s.getState() == GameState.COUNTDOWN) s.setState(GameState.IDLE);

        plugin.getSignManager().updateAllSigns();
        Dbg.d(CountdownManager.class, "countdown cancel arena=" + s.getArena().getName() + " reason=" + reason);
    }

    /**
     * Wird aufgerufen, wenn ein Spieler die Arena verlässt.
     * Falls dadurch zu wenige Spieler da sind → Countdown abbrechen.
     */
    public void handlePlayerLeave(GameSession s) {
        if (s.getState() == GameState.COUNTDOWN && s.getPlayers().size() < s.getArena().getMinPlayers()) {
            cancel(s, "player left");
        }
    }

    /**
     * Wird aufgerufen, wenn das Match beendet oder zurückgesetzt wird.
     * Entfernt alle eventuell verbliebenen BossBars/Tasks.
     */
    public void cleanup(GameSession s) {
        cancel(s, "cleanup/reset");
    }

    public void cleanupAll() {
        for (String k : new HashSet<>(bars.keySet())) {
            BossBar bar = bars.remove(k);
            if (bar != null) bar.removeAll();
        }
        for (Integer id : runningTasks.values()) {
            Bukkit.getScheduler().cancelTask(id);
        }
        runningTasks.clear();
        Dbg.d(CountdownManager.class, "cleanupAll: alle Countdowns entfernt");
    }

    public BossBar getActiveBar(String arenaName) {
        return bars.get(arenaName.toLowerCase(Locale.ROOT));
    }
}

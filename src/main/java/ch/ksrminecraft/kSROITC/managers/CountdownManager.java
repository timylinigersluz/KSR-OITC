package ch.ksrminecraft.kSROITC.managers;

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

    public CountdownManager(KSROITC plugin) { this.plugin = plugin; }

    public void start(GameSession s, int seconds, Runnable onFinish) {
        if (s.getState() != GameState.IDLE) return;
        s.setState(GameState.COUNTDOWN);

        BossBar bar = Bukkit.createBossBar("Start in " + seconds + "s", BarColor.BLUE, BarStyle.SEGMENTED_20);
        bars.put(s.getArena().getName().toLowerCase(Locale.ROOT), bar);
        s.getPlayers().forEach(u -> { Player p = Bukkit.getPlayer(u); if (p != null) bar.addPlayer(p); });
        bar.setProgress(1.0);

        s.setCountdownEndTimestamp(System.currentTimeMillis() + seconds * 1000L);

        plugin.getSignManager().updateAllSigns(); // sofortige Aktualisierung

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

        s.setTaskId(taskId);
        Dbg.d(CountdownManager.class, "countdown start arena=" + s.getArena().getName() + " seconds=" + seconds + " task=" + taskId);
    }

    public void cancel(GameSession s, String reason) {
        BossBar bar = bars.remove(s.getArena().getName().toLowerCase(Locale.ROOT));
        if (bar != null) bar.removeAll();
        if (s.getTaskId() != -1) { Bukkit.getScheduler().cancelTask(s.getTaskId()); s.setTaskId(-1); }
        s.setCountdownEndTimestamp(-1L);
        if (s.getState() == GameState.COUNTDOWN) s.setState(GameState.IDLE);

        plugin.getSignManager().updateAllSigns(); // Schilder nach Abbruch aktualisieren
        Dbg.d(CountdownManager.class, "countdown cancel arena=" + s.getArena().getName() + " reason=" + reason);
    }
}

package ch.ksrminecraft.kSROITC.managers;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.integration.RankPointsHook;
import ch.ksrminecraft.kSROITC.models.*;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.InventoryBackupManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class MatchManager {

    private final KSROITC plugin;
    private final TeleportManager tp;
    private final KitManager kits;
    private final SessionManager sessions;
    private final ScoreboardManager scoreboards;
    private final RankPointsHook rankpoints;
    private final Map<String, BossBar> matchBars = new HashMap<>();

    public MatchManager(KSROITC plugin, TeleportManager tp, KitManager kits, SessionManager sessions,
                        ScoreboardManager scoreboards, RankPointsHook rankpoints) {
        this.plugin = plugin;
        this.tp = tp;
        this.kits = kits;
        this.sessions = sessions;
        this.scoreboards = scoreboards;
        this.rankpoints = rankpoints;
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
        if (s.getPlayers().size() < a.getMinPlayers()) return;

        s.setState(GameState.RUNNING);
        long endTs = a.getMaxSeconds() > 0
                ? System.currentTimeMillis() + a.getMaxSeconds() * 1000L
                : -1;
        s.setEndTimestamp(endTs);
        s.setCountdownEndTimestamp(-1L);

        int teleported = 0;
        for (UUID u : s.getPlayers()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                tp.toRandomSpawn(p, a);
                kits.giveKit(p, a.isGiveSword());
                scoreboards.apply(p, s);

                // Titel und Sound
                p.showTitle(Title.title(
                        Component.text("OITC").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                        Component.text("Ziel: " + a.getMaxKills() + " Kills").color(NamedTextColor.GRAY)
                ));
                try {
                    p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                } catch (Throwable ignored) {}
                teleported++;
            }
        }

        plugin.getSignManager().updateAllSigns();
        broadcast(s, "§a[OITC] §7Gestartet in §e" + a.getName() + "§7. Ziel: §e" + a.getMaxKills() + " Kills.");
        Dbg.d(MatchManager.class, "start: tp+kit=" + teleported + " endTs=" + endTs);

        // Bossbar initialisieren
        if (endTs > 0) {
            long total = a.getMaxSeconds();
            long left = Math.max(0, (endTs - System.currentTimeMillis()) / 1000L);
            BossBar bar = Bukkit.createBossBar("⌛ Restzeit: " + left + "s", BarColor.GREEN, BarStyle.SEGMENTED_20);
            matchBars.put(a.getName().toLowerCase(Locale.ROOT), bar);
            for (UUID u : s.getPlayers()) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) bar.addPlayer(p);
            }
            double prog = (total <= 0) ? 1.0 : Math.max(0.0, Math.min(1.0, left / (double) total));
            bar.setProgress(prog);
        }

        if (endTs > 0) {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(s), 20L, 20L);
            s.setTaskId(task.getTaskId());
        }
    }

    // ============================================================
    // MATCH STOP
    // ============================================================
    public void stop(GameSession s, boolean showMsg) {
        if (s.getTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(s.getTaskId());
            s.setTaskId(-1);
        }

        // Bossbar entfernen
        BossBar bar = matchBars.remove(s.getArena().getName().toLowerCase(Locale.ROOT));
        if (bar != null) bar.removeAll();

        // Scoreboards leeren
        scoreboards.clearAll(s);

        // Alle Spieler zur Mainlobby + Inventar wiederherstellen
        int returned = 0;
        for (UUID u : new HashSet<>(s.getPlayers())) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                tp.toMainLobby(p);
                ch.ksrminecraft.kSROITC.utils.InventoryBackupManager.restoreInventory(p);
                if (showMsg) p.sendMessage("§7Zurück zur Mainlobby.");
                returned++;
            }
        }

        // Session-Reset
        sessions.clearMappings(s);
        s.getPlayers().clear();
        s.getKills().clear();
        s.setState(GameState.IDLE);

        plugin.getSignManager().updateAllSigns();
        Dbg.d(MatchManager.class, "stop: returned=" + returned + " arena=" + s.getArena().getName());
    }

    // ============================================================
    // MATCH TICK
    // ============================================================
    private void tick(GameSession s) {
        if (s.getState() != GameState.RUNNING) return;

        if (s.getPlayers().isEmpty()) {
            Dbg.d(MatchManager.class, "tick: empty session -> stop");
            stop(s, true);
            return;
        }

        long left = (s.getEndTimestamp() - System.currentTimeMillis()) / 1000L;
        if (s.getEndTimestamp() > 0 && left <= 0) {
            endWithWinners(s, "time");
            return;
        }

        scoreboards.updateAll(s);
        for (UUID u : s.getPlayers()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && s.getEndTimestamp() > 0)
                p.sendActionBar(Component.text("⏱ " + left + "s"));
        }

        Arena a = s.getArena();
        BossBar bar = matchBars.get(a.getName().toLowerCase(Locale.ROOT));
        if (bar != null && s.getEndTimestamp() > 0) {
            bar.setTitle("⌛ Restzeit: " + left + "s");
            double prog = (a.getMaxSeconds() <= 0)
                    ? 1.0
                    : Math.max(0.0, Math.min(1.0, left / (double) a.getMaxSeconds()));
            bar.setProgress(prog);
        }
    }

    // ============================================================
    // MATCH ENDE MIT GEWINNERN
    // ============================================================
    public void endWithWinners(GameSession s, String reason) {
        if (s == null || s.getArena() == null) return;

        // Gewinner bestimmen
        int top = 0;
        List<Player> winners = new ArrayList<>();
        for (UUID u : s.getPlayers()) {
            int k = s.getKills().getOrDefault(u, 0);
            if (k > top) {
                top = k;
                winners.clear();
            }
            if (k == top) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) winners.add(p);
            }
        }

        // Gewinneranzeige
        if (winners.isEmpty()) {
            broadcast(s, "§e[OITC] §7Runde beendet. Kein Gewinner.");
        } else if (winners.size() == 1) {
            broadcast(s, "§6" + winners.get(0).getName() + " §7gewinnt mit §e" + top + " §7Kills!");
        } else {
            String names = String.join("§7, §6", winners.stream().map(Player::getName).toList());
            broadcast(s, "§eUnentschieden §7zwischen §6" + names + " §7(§e" + top + "§7 Kills).");
        }

        // Rangliste
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(s.getKills().entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        broadcast(s, "§7§m-----------------------------");
        broadcast(s, "§e§lEndstand:");
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : sorted) {
            Player pl = Bukkit.getPlayer(entry.getKey());
            if (pl != null) {
                broadcast(s, "§7" + rank + ". §e" + pl.getName() + " §7- §b" + entry.getValue() + " Kills");
                rank++;
            }
        }
        broadcast(s, "§7§m-----------------------------");

        // Punktevergabe
        if (rankpoints != null && rankpoints.isEnabled()) {
            try {
                for (Map.Entry<UUID, Integer> entry : s.getKills().entrySet()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p == null) continue;
                    int kills = entry.getValue();
                    for (int i = 0; i < kills; i++) rankpoints.recordKill(p);
                }
                int participants = s.getPlayers().size();
                for (Player winner : winners) rankpoints.recordWin(winner, participants);
                rankpoints.commitSessionPoints();
            } catch (Exception e) {
                plugin.getLogger().warning("[OITC] Fehler bei Punktevergabe: " + e.getMessage());
            }
        }

        s.setState(GameState.ENDING);
        plugin.getSignManager().updateAllSigns();
        stop(s, true);
        Dbg.d(MatchManager.class, "endWithWinners: reason=" + reason + ", arena=" + s.getArena().getName());
    }

    // ============================================================
    // BROADCAST
    // ============================================================
    public void broadcast(GameSession s, String msg) {
        for (UUID u : s.getPlayers()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage(msg);
        }
    }
}

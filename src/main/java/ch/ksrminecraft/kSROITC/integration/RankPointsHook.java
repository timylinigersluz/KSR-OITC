package ch.ksrminecraft.kSROITC.integration;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.RankPointsAPI.PointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Verwaltet die Anbindung an die RankPointsAPI.
 * Punkte werden während des Spiels zwischengespeichert
 * und erst am Spielende persistiert.
 */
public class RankPointsHook {

    private final KSROITC plugin;
    private final boolean enabled;
    private final int perKill;
    private final int winCap;
    private final boolean excludeStaff;

    private PointsAPI api;

    // Summe der Rundenpunkte (Kills + Winbonus) pro Spieler
    private final Map<UUID, Integer> sessionPoints = new HashMap<>();

    // Kills pro Spieler (nur für Anzeige / Debug)
    private final Map<UUID, Integer> killCounts = new HashMap<>();

    // Winbonus pro Spieler (nur für Anzeige / Debug)
    private final Map<UUID, Integer> winBonus = new HashMap<>();

    public RankPointsHook(KSROITC plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("rankpoints.enabled", true);
        this.perKill = plugin.getConfig().getInt("rankpoints.per_kill", 1);
        this.winCap = plugin.getConfig().getInt("rankpoints.win_bonus_cap", 5);
        this.excludeStaff = plugin.getConfig().getBoolean("rankpoints.exclude_staff", true);

        Dbg.d(RankPointsHook.class, "init: enabled=" + enabled + " perKill=" + perKill + " winCap=" + winCap);

        if (!enabled) {
            Bukkit.getLogger().info("[KSROITC] RankPointsAPI deaktiviert.");
            return;
        }

        try {
            String host = plugin.getConfig().getString("mysql.host");
            int port = plugin.getConfig().getInt("mysql.port", 3306);
            String database = plugin.getConfig().getString("mysql.database");
            String user = plugin.getConfig().getString("mysql.user");
            String password = plugin.getConfig().getString("mysql.password");
            boolean debug = plugin.getConfig().getBoolean("debug", false);

            String jdbc = "jdbc:mysql://" + host + ":" + port + "/" + database;
            this.api = new PointsAPI(jdbc, user, password, plugin.getLogger(), debug, excludeStaff);
            Bukkit.getLogger().info("[KSROITC] RankPointsAPI erfolgreich initialisiert.");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[KSROITC] Fehler bei RankPointsAPI-Initialisierung: " + e.getMessage());
        }
    }

    // ============================================================
    // 🧠 Kill & Win Speicherung
    // ============================================================

    /**
     * Punkte & Kill zählen
     */
    public void recordKill(Player p) {
        if (!enabled || p == null) return;

        sessionPoints.merge(p.getUniqueId(), perKill, Integer::sum);
        killCounts.merge(p.getUniqueId(), 1, Integer::sum);

        Dbg.d(RankPointsHook.class, "recordKill: " + p.getName() + " +" + perKill);
    }

    /**
     * Sieg-Bonuspunkte (Modell 1):
     * bonus = min(participants, win_bonus_cap)
     */
    public void recordWin(Player p, int participants) {
        if (!enabled || p == null) return;

        int bonus = Math.min(Math.max(0, participants), winCap);

        sessionPoints.merge(p.getUniqueId(), bonus, Integer::sum);
        winBonus.merge(p.getUniqueId(), bonus, Integer::sum);

        Dbg.d(RankPointsHook.class, "recordWin: " + p.getName() + " +" + bonus + " (participants=" + participants + ")");
    }

    // ============================================================
    // 💾 Punkte am Spielende speichern
    // ============================================================

    public void commitSessionPoints() {
        if (!enabled || api == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int saved = 0;
            int totalPointsSaved = 0;

            for (Map.Entry<UUID, Integer> entry : sessionPoints.entrySet()) {
                UUID id = entry.getKey();
                int roundPts = entry.getValue();

                int kills = killCounts.getOrDefault(id, 0);
                int killPts = kills * perKill;
                int winPts = winBonus.getOrDefault(id, 0);

                try {
                    api.addPoints(id, roundPts);
                    int newTotal = api.getPoints(id);

                    Player p = Bukkit.getPlayer(id);
                    if (p != null && p.isOnline()) {
                        // ✅ Klare, korrekte Aufschlüsselung
                        if (winPts > 0) {
                            p.sendMessage("§7Runde: §e" + kills + "§7 Kills (§a+" + killPts + "§7) "
                                    + "§7+ Siegbonus (§a+" + winPts + "§7) §7= §a" + roundPts + "§7 Punkte.");
                        } else {
                            p.sendMessage("§7Runde: §e" + kills + "§7 Kills (§a+" + killPts + "§7) "
                                    + "§7= §a" + roundPts + "§7 Punkte.");
                        }
                        p.sendMessage("§7Deine neuen Rangpunkte: §b" + newTotal + "§7.");
                    }

                    Dbg.d(RankPointsHook.class, "commit: " + id + " +" + roundPts
                            + " (kills=" + kills + ", killPts=" + killPts + ", winPts=" + winPts + ") → total=" + newTotal);

                    saved++;
                    totalPointsSaved += roundPts;
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[KSROITC] Fehler beim Schreiben von Punkten für " + id + ": " + e.getMessage());
                }
            }

            sessionPoints.clear();
            killCounts.clear();
            winBonus.clear();

            Dbg.d(RankPointsHook.class, "commitSessionPoints: saved=" + saved + ", totalPoints=" + totalPointsSaved);
        });
    }

    // ============================================================
    // 🧹 Cleanup
    // ============================================================

    public void clearSession() {
        sessionPoints.clear();
        killCounts.clear();
        winBonus.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getWinCap() {
        return winCap;
    }

    public int getPerKill() {
        return perKill;
    }

    public void removePlayerFromSession(Player p) {
        if (p != null) {
            UUID id = p.getUniqueId();
            sessionPoints.remove(id);
            killCounts.remove(id);
            winBonus.remove(id);
            Dbg.d(RankPointsHook.class, "removePlayerFromSession: " + p.getName());
        }
    }
}

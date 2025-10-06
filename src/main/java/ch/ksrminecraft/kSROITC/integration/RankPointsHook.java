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
    private final Map<UUID, Integer> sessionPoints = new HashMap<>();

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

    /**
     * Nur intern: Punkte für einen Kill zwischenspeichern.
     */
    public void recordKill(Player p) {
        if (!enabled || p == null) return;
        sessionPoints.merge(p.getUniqueId(), perKill, Integer::sum);
        Dbg.d(RankPointsHook.class, "recordKill: " + p.getName() + " +" + perKill);
    }

    /**
     * Nur intern: Punkte für einen Sieg zwischenspeichern.
     */
    public void recordWin(Player p, int participants) {
        if (!enabled || p == null) return;
        int bonus = Math.min(participants, winCap);
        sessionPoints.merge(p.getUniqueId(), bonus, Integer::sum);
        Dbg.d(RankPointsHook.class, "recordWin: " + p.getName() + " +" + bonus);
    }

    /**
     * Schreibt alle gesammelten Punkte der Session in die Datenbank.
     * Wird am Spielende aufgerufen.
     */
    public void commitSessionPoints() {
        if (!enabled || api == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int saved = 0;

            for (Map.Entry<UUID, Integer> entry : sessionPoints.entrySet()) {
                UUID id = entry.getKey();
                int pts = entry.getValue();

                try {
                    api.addPoints(id, pts); // Staff wird intern gefiltert
                    int total = api.getPoints(id);

                    Player p = Bukkit.getPlayer(id);
                    if (p != null)
                        p.sendMessage("§a+" + pts + " Punkte §7(insgesamt §b" + total + "§7)");

                    Dbg.d(RankPointsHook.class, "commit: " + id + " +" + pts + " → total=" + total);
                    saved++;
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[KSROITC] Fehler beim Schreiben von Punkten für " + id + ": " + e.getMessage());
                }
            }

            sessionPoints.clear();
            Dbg.d(RankPointsHook.class, "commitSessionPoints: saved=" + saved + " Einträge geschrieben.");
        });
    }

    public void clearSession() {
        sessionPoints.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void awardKill(Player p) {
        if (!enabled || api == null || p == null) return;
        int points = plugin.getConfig().getInt("points.kill", 5); // z. B. 5 Punkte pro Kill
        try {
            api.addPoints(p.getUniqueId(), points);
            int total = api.getPoints(p.getUniqueId());
            p.sendMessage("§a+" + points + " Punkte §7(insgesamt §b" + total + "§7)");
            plugin.getLogger().info("[OITC] +" + points + " Punkte an " + p.getName() + " vergeben.");
        } catch (Exception e) {
            plugin.getLogger().warning("[OITC] Fehler bei awardKill(" + p.getName() + "): " + e.getMessage());
        }
    }
}

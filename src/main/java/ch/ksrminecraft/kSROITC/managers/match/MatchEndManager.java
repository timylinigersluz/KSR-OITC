package ch.ksrminecraft.kSROITC.managers.match;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.integration.RankPointsHook;
import ch.ksrminecraft.kSROITC.managers.arena.TeleportManager;
import ch.ksrminecraft.kSROITC.managers.game.ScoreboardManager;
import ch.ksrminecraft.kSROITC.managers.game.SessionManager;
import ch.ksrminecraft.kSROITC.models.*;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Verantwortlich für das Spielende:
 * - Gewinnerermittlung
 * - Punktevergabe
 * - Fireworks & Rangliste
 * - Arena-Cleanup (teleportiert & reset)
 * - Countdown-Cleanup nach Match-Ende
 */
public class MatchEndManager {

    private final KSROITC plugin;
    private final SessionManager sessions;
    private final ScoreboardManager scoreboards;
    private final RankPointsHook rankpoints;

    public MatchEndManager(KSROITC plugin, SessionManager sessions, ScoreboardManager scoreboards, RankPointsHook rankpoints) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.scoreboards = scoreboards;
        this.rankpoints = rankpoints;
    }

    public void handleMatchEnd(GameSession s, String reason) {
        if (s == null || s.getArena() == null) return;

        // --- Gewinner bestimmen ---
        int top = 0;
        List<Player> winners = new ArrayList<>();
        for (UUID u : s.getPlayers()) {
            if (sessions.isSpectator(Bukkit.getPlayer(u))) continue;
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

        // --- Ergebnisnachricht ---
        if (winners.isEmpty()) {
            broadcast(s, "§e[OITC] §7Runde beendet. Kein Gewinner.");
        } else if (winners.size() == 1) {
            broadcast(s, "§6" + winners.get(0).getName() + " §7gewinnt mit §e" + top + " §7Kills!");
        } else {
            String names = String.join("§7, §6", winners.stream().map(Player::getName).toList());
            broadcast(s, "§eUnentschieden §7zwischen §6" + names + " §7(§e" + top + "§7 Kills).");
        }

        // --- Punktevergabe ---
        if (rankpoints != null && rankpoints.isEnabled()) {
            try {
                for (Map.Entry<UUID, Integer> entry : s.getKills().entrySet()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p == null || sessions.isSpectator(p)) continue;
                    int kills = entry.getValue();
                    for (int i = 0; i < kills; i++) rankpoints.recordKill(p);
                }
                int participants = sessions.getActiveCount(s);
                for (Player winner : winners) rankpoints.recordWin(winner, participants);
                rankpoints.commitSessionPoints();
            } catch (Exception e) {
                plugin.getLogger().warning("[OITC] Fehler bei Punktevergabe: " + e.getMessage());
            }
        }

        // --- Celebration ---
        if (!winners.isEmpty()) {
            new CelebrationManager(plugin).celebrateWinners(winners);
        }

        // --- Spielstatus aktualisieren ---
        s.setState(GameState.ENDING);
        plugin.getSignManager().updateAllSigns();

        // --- Cleanup (inkl. Countdown-Cleanup) ---
        plugin.getGameManager().getMatchManager().stop(s, true);
        plugin.getGameManager().getCountdowns().cleanup(s);

        Dbg.d(MatchEndManager.class, "handleMatchEnd: reason=" + reason + ", arena=" + s.getArena().getName());
    }

    /**
     * Zentrale Methode zum Zurücksetzen einer Arena und Teleportieren aller Spieler.
     */
    public int resetArena(GameSession s, boolean showMsg) {
        TeleportManager tp = plugin.getTeleportManager();
        var specs = plugin.getGameManager().getSpectatorManager();

        int moved = 0;
        for (UUID id : new HashSet<>(s.getPlayers())) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            specs.setSpectator(p, false);
            p.getInventory().clear();
            p.setFireTicks(0);
            p.setHealth(20);
            p.setFoodLevel(20);
            p.setSaturation(5);
            tp.toMainLobby(p);

            try {
                ch.ksrminecraft.kSROITC.utils.InventoryBackupManager.restoreInventory(p);
            } catch (Exception ignored) {}

            p.sendMessage("§aSpiel vorbei – du bist zurück in der Mainlobby.");
            moved++;
        }

        scoreboards.clearAll(s);
        sessions.clearMappings(s);
        s.getPlayers().clear();
        s.getKills().clear();
        s.setState(GameState.IDLE);

        // --- Nur Admins sehen die Reset-Info ---
        if (showMsg) {
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("oitc.admin")) {
                    admin.sendMessage("§a[OITC] §7Arena §e" + s.getArena().getName()
                            + " §7wurde zurückgesetzt. (" + moved + " Spieler teleportiert)");
                }
            }
        }

        Dbg.d(MatchEndManager.class, "resetArena: arena=" + s.getArena().getName() + " teleported=" + moved);
        return moved;
    }

    private void broadcast(GameSession s, String msg) {
        for (Player p : sessions.getActivePlayers(s)) {
            p.sendMessage(msg);
        }
    }
}

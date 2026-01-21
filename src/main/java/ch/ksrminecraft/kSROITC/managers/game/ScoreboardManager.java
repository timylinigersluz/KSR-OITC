package ch.ksrminecraft.kSROITC.managers.game;

import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Zeigt die Live-Rangliste der AKTIVEN Spieler im Spiel an.
 *
 * WICHTIG:
 * - Spectators dürfen das Scoreboard sehen,
 * - aber sie selbst dürfen NICHT in der Rangliste erscheinen.
 *
 * Anzeige:
 * §6§lKSR-OITC
 * §7Arena: §f[arena]
 * Top 1–5 farbig
 * + eigene Zeile (nur wenn man aktiver Spieler ist) unter Top 5
 * Footer: §fksrminecraft.ch
 */
public class ScoreboardManager {

    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final SessionManager sessions;

    public ScoreboardManager(SessionManager sessions) {
        this.sessions = sessions;
    }

    public void apply(Player viewer, GameSession s) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("oitc", Criteria.DUMMY, Component.text("§6§lKSR-OITC"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        UUID self = viewer.getUniqueId();
        int line = 15;

        // --- Leere Zeile nach Titel ---
        obj.getScore(" ").setScore(line--);

        // --- Arena-Zeile ---
        obj.getScore("§7Arena: §f" + s.getArena().getName()).setScore(line--);

        // --- Leere Zeile ---
        obj.getScore("  ").setScore(line--);

        // ============================================================
        // ✅ Rangliste NUR aus aktiven Spielern (Spectators rausfiltern)
        // ============================================================
        List<Player> active = sessions.getActivePlayers(s);

        Map<UUID, Integer> activeKills = new HashMap<>();
        for (Player p : active) {
            activeKills.put(p.getUniqueId(), s.getKills().getOrDefault(p.getUniqueId(), 0));
        }

        List<Map.Entry<UUID, Integer>> sorted = activeKills.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .toList();

        int rank = 1;
        boolean selfShown = false;

        for (Map.Entry<UUID, Integer> e : sorted) {
            if (rank > 5) break;

            Player pl = Bukkit.getPlayer(e.getKey());
            if (pl == null) continue;

            String color = switch (rank) {
                case 1 -> "§a"; // Grün
                case 2 -> "§e"; // Gelb
                default -> "§c"; // Rot
            };

            boolean isSelf = pl.getUniqueId().equals(self);
            String name = isSelf ? "§l" + pl.getName() : pl.getName();

            String lineText = color + rank + ". " + name + " §7- §f" + e.getValue();
            obj.getScore(lineText).setScore(line--);

            if (isSelf) selfShown = true;
            rank++;
        }

        // ============================================================
        // 👤 Eigene Zeile NUR, wenn man aktiver Spieler ist (nicht Spectator)
        // ============================================================
        boolean selfIsActive = activeKills.containsKey(self);

        if (selfIsActive && !selfShown) {
            int selfRank = 1;
            for (Map.Entry<UUID, Integer> e : sorted) {
                if (e.getKey().equals(self)) break;
                selfRank++;
            }
            int kills = activeKills.getOrDefault(self, 0);
            String selfLine = "§7" + selfRank + ". §f§l" + viewer.getName() + " §7- §f" + kills;
            obj.getScore(selfLine).setScore(line--);
        }

        // --- Leerzeile vor Footer ---
        obj.getScore("   ").setScore(line--);

        // Footer
        obj.getScore("§fksrminecraft.ch").setScore(line--);

        // Anwenden
        viewer.setScoreboard(sb);
        boards.put(viewer.getUniqueId(), sb);
    }

    /**
     * Aktualisiert alle Scoreboards einer Session.
     * (Auch Spectators bekommen das Scoreboard aktualisiert, sehen aber nur aktive Spieler.)
     */
    public void updateAll(GameSession s) {
        for (UUID u : s.getPlayers()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline()) {
                apply(p, s);
            }
        }
    }

    /**
     * Entfernt Scoreboard eines Spielers.
     */
    public void clear(Player p) {
        boards.remove(p.getUniqueId());
        p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        Dbg.d(getClass(), "clear -> " + p.getName());
    }

    /**
     * Entfernt alle Scoreboards einer Session.
     */
    public void clearAll(GameSession s) {
        for (UUID u : new HashSet<>(s.getPlayers())) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) clear(p);
        }
    }
}

package ch.ksrminecraft.kSROITC.managers.game;

import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Zeigt die Live-Rangliste aller Spieler im Spiel an.
 * -----------------------------------------------
 * §6§lKSR-OITC
 *
 * §7Arena: §f[arena]
 *
 * Platz 1–5 farbig:
 *   §a1. Spieler – 8 Kills
 *   §e2. Spieler – 6 Kills
 *   §c3. Spieler – 4 Kills
 *   ...
 * + eigene Zeile unter Platz 6 (grau), falls man außerhalb der Top5 ist.
 * Eigener Name ist immer fett.
 * Maximal 7 Zeilen insgesamt.
 *
 * Footer: §fksrminecraft.ch
 */
public class ScoreboardManager {

    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public void apply(Player p, GameSession s) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("oitc", Criteria.DUMMY, Component.text("§6§lKSR-OITC"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        UUID self = p.getUniqueId();
        int line = 15;

        // --- Leere Zeile nach Titel ---
        obj.getScore(" ").setScore(line--);

        // --- Arena-Zeile ---
        obj.getScore("§7Arena: §f" + s.getArena().getName()).setScore(line--);

        // --- Leere Zeile ---
        obj.getScore("  ").setScore(line--);

        // ------------------------------------------------------------
        // 🏆 Sortiere Spieler nach Kills (absteigend)
        List<Map.Entry<UUID, Integer>> sorted = s.getKills().entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .toList();

        // ------------------------------------------------------------
        // 🔢 Rangliste anzeigen (max 5 + eigener Rang)
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

            String lineText = color + rank + ". " + pl.getName() + " §7- §f" + e.getValue();
            obj.getScore(lineText).setScore(line--);

            if (pl.getUniqueId().equals(self)) selfShown = true;
            rank++;
        }

        // 👤 Eigenen Rang immer anzeigen (wenn nicht in Top 5)
        if (!selfShown) {
            int selfRank = 1;
            for (Map.Entry<UUID, Integer> e : sorted) {
                if (e.getKey().equals(self)) break;
                selfRank++;
            }
            int kills = s.getKills().getOrDefault(self, 0);
            String selfLine = "§7" + selfRank + ". §f" + p.getName() + " §7- §f" + kills;
            obj.getScore(selfLine).setScore(line--);
        }

        // ------------------------------------------------------------
        // 🆕 Leerzeile vor Footer
        obj.getScore("   ").setScore(line--);

        // Footer
        obj.getScore("§fksrminecraft.ch").setScore(line--);

        // Anwenden
        p.setScoreboard(sb);
        boards.put(p.getUniqueId(), sb);
        Dbg.d(getClass(), "apply -> " + p.getName());
    }

    /**
     * Aktualisiert alle Scoreboards einer Session.
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

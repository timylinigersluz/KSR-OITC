package ch.ksrminecraft.kSROITC.managers;

import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardManager {

    // pro Spieler ein eigenes Board
    private final Map<UUID, org.bukkit.scoreboard.Scoreboard> boards = new HashMap<>();

    // 16 eindeutige Einträge (Scoreboard-Zeilen-Keys)
    private static final String[] ENTRY = {
            "§0","§1","§2","§3","§4","§5","§6","§7",
            "§8","§9","§a","§b","§c","§d","§e","§f"
    };

    public void apply(Player p, GameSession s) {
        org.bukkit.scoreboard.Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("oitc", Criteria.DUMMY, Component.text("§6§lOITC"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Reihenfolge: höhere Score oben
        int line = 7;

        // Arena
        line(sb, obj, line--, "ksr_arena", "§7Arena: §e", s.getArena().getName(), ENTRY[0]);

        // Spacer
        obj.getScore(ENTRY[1]).setScore(line--);

        // Kills (dynamisch)
        int kills = s.getKills().getOrDefault(p.getUniqueId(), 0);
        line(sb, obj, line--, "ksr_kills", "§fKills: §a", String.valueOf(kills), ENTRY[2]);

        // Ziel
        line(sb, obj, line--, "ksr_target", "§fZiel: §e", String.valueOf(s.getArena().getMaxKills()), ENTRY[3]);

        // Zeit
        String time = timeLeftReadable(s);
        line(sb, obj, line--, "ksr_time", "§fZeit: §b", time, ENTRY[4]);

        // Footer
        obj.getScore("§8ksr-oitc").setScore(line--);

        p.setScoreboard(sb);
        boards.put(p.getUniqueId(), sb);
        Dbg.d(ScoreboardManager.class, "apply -> " + p.getName());
    }

    public void updateAll(GameSession s) {
        String time = timeLeftReadable(s);

        for (UUID u : s.getPlayers()) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;

            org.bukkit.scoreboard.Scoreboard sb = boards.get(u);
            if (sb == null) { apply(p, s); sb = boards.get(u); }
            if (sb == null) continue;

            Team tk = team(sb, "ksr_kills");
            tk.suffix(Component.text(String.valueOf(s.getKills().getOrDefault(u, 0))));

            Team tt = team(sb, "ksr_target");
            tt.suffix(Component.text(String.valueOf(s.getArena().getMaxKills())));

            Team ti = team(sb, "ksr_time");
            ti.suffix(Component.text(time));
        }
    }

    public void clear(Player p) {
        boards.remove(p.getUniqueId());
        p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        Dbg.d(ScoreboardManager.class, "clear -> " + p.getName());
    }

    public void clearAll(GameSession s) {
        for (UUID u : new HashSet<>(s.getPlayers())) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) clear(p);
        }
    }

    // ----- helpers -----
    private void line(org.bukkit.scoreboard.Scoreboard sb, Objective obj, int score, String teamName,
                      String prefix, String suffix, String entry) {
        Team t = team(sb, teamName);
        if (!t.hasEntry(entry)) t.addEntry(entry);
        t.prefix(Component.text(prefix));
        t.suffix(Component.text(suffix));
        obj.getScore(entry).setScore(score);
    }

    private Team team(org.bukkit.scoreboard.Scoreboard sb, String name) {
        Team t = sb.getTeam(name);
        if (t == null) t = sb.registerNewTeam(name);
        return t;
    }

    private String timeLeftReadable(GameSession s) {
        if (s.getEndTimestamp() <= 0) return "∞";
        long sec = Math.max(0, (s.getEndTimestamp() - System.currentTimeMillis()) / 1000L);
        long m = sec / 60, sRem = sec % 60;
        return (m > 0 ? m + "m " : "") + sRem + "s";
    }
}

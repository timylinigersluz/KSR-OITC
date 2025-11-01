package ch.ksrminecraft.kSROITC.managers.game;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.integration.RankPointsHook;
import ch.ksrminecraft.kSROITC.managers.arena.ArenaManager;
import ch.ksrminecraft.kSROITC.managers.arena.TeleportManager;
import ch.ksrminecraft.kSROITC.managers.match.MatchManager;
import ch.ksrminecraft.kSROITC.managers.system.CountdownManager;
import ch.ksrminecraft.kSROITC.models.*;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

/**
 * Verantwortlich für den Arena-Beitritt (Join).
 * Prüft Arena-Bedingungen, setzt Spielerstatus,
 * startet ggf. Countdown und behandelt Zuschauer korrekt.
 */
public class PlayerJoinHandler {

    private final KSROITC plugin;
    private final ArenaManager arenas;
    private final TeleportManager tp;
    private final RankPointsHook rankpoints;
    private final SessionManager sessions;
    private final CountdownManager countdowns;
    private final ScoreboardManager scoreboards;
    private final MatchManager match;
    private final SpectatorManager spectators;

    public PlayerJoinHandler(KSROITC plugin,
                             ArenaManager arenas,
                             TeleportManager tp,
                             RankPointsHook rankpoints,
                             SessionManager sessions,
                             CountdownManager countdowns,
                             ScoreboardManager scoreboards,
                             MatchManager match,
                             SpectatorManager spectators) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.tp = tp;
        this.rankpoints = rankpoints;
        this.sessions = sessions;
        this.countdowns = countdowns;
        this.scoreboards = scoreboards;
        this.match = match;
        this.spectators = spectators;
    }

    // ============================================================
    // SPIELER JOIN
    // ============================================================
    public boolean handleJoin(Player p, String arenaName) {
        Dbg.d(PlayerJoinHandler.class, "handleJoin(" + p.getName() + ", arena=" + arenaName + ")");

        // --- Arena prüfen ---
        Arena a = arenas.get(arenaName);
        if (a == null) {
            p.sendMessage("§cArena nicht gefunden.");
            return false;
        }

        World w = Bukkit.getWorld(a.getWorldName());
        if (w == null) {
            p.sendMessage("§cWelt '" + a.getWorldName() + "' ist nicht geladen.");
            return false;
        }

        if (a.getLobby() == null) {
            p.sendMessage("§cKeine Lobby gesetzt (§e/oitc setlobby§c).");
            return false;
        }

        if (a.getSpawns().size() < 2) {
            p.sendMessage("§cMindestens 2 Spawns erforderlich (§e/oitc addspawn§c).");
            return false;
        }

        // --- Spielerstatus prüfen ---
        if (sessions.byPlayer(p).isPresent()) {
            p.sendMessage("§cDu bist bereits in einer Arena. Nutze §e/oitc leave§c.");
            return false;
        }

        // --- Session erstellen oder laden ---
        GameSession s = sessions.ensure(a);
        boolean isRunning = s.getState() == GameState.RUNNING;

        if (isRunning && !a.isAllowJoinInProgress()) {
            p.sendMessage("§cDiese Arena läuft bereits.");
            return false;
        }

        if (!isRunning && sessions.getActiveCount(s) >= a.getMaxPlayers()) {
            p.sendMessage("§cArena ist voll.");
            return false;
        }

        // --- Spielerstatus zurücksetzen & speichern ---
        ch.ksrminecraft.kSROITC.utils.InventoryBackupManager.saveInventory(p);
        preparePlayerForJoin(p);

        // --- Registrierung in Session ---
        boolean spectator = isRunning;
        sessions.addPlayer(p, s, spectator);

        // --- Teleport zuerst ---
        tp.toLobby(p, a);

        // --- Danach Modus / Anzeige setzen ---
        if (spectator) {
            spectators.setSpectator(p, true);

            // 🧩 Spectator: Scoreboard & laufende BossBar anzeigen
            scoreboards.apply(p, s);
            BossBar bar = countdowns.getActiveBar(a.getName());
            if (bar != null) {
                bar.addPlayer(p);
                Dbg.d(PlayerJoinHandler.class, "Spectator zur laufenden BossBar hinzugefügt: " + a.getName());
            }

            // Sicherheitscheck nach 2 Ticks – andere Plugins überschreiben manchmal GameMode
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    p.setGameMode(org.bukkit.GameMode.SPECTATOR);
                    Dbg.d(PlayerJoinHandler.class, "Spectator-Mode erneut gesetzt für " + p.getName());
                }
            }, 2L);

            p.sendMessage("§7[OITC] Du bist §eals Zuschauer §7dem Spiel §e" + a.getName() + " §7beigetreten.");
            plugin.getSignManager().updateAllSigns();
            Dbg.d(PlayerJoinHandler.class, "handleJoin: " + p.getName() + " ist Spectator (mit Scoreboard & BossBar)");
            return true;
        }

        // --- Normale Spieler ---
        spectators.setSpectator(p, false);
        scoreboards.apply(p, s);
        plugin.getSignManager().updateAllSigns();

        // --- Countdown-Entscheidung ---
        int current = sessions.getActiveCount(s);
        int min = a.getMinPlayers();
        int missing = Math.max(0, min - current);

        if (missing > 0) {
            broadcastMissingPlayers(a, missing);
        } else {
            // Nur im Lobbyzustand starten; CountdownManager.start() räumt einen evtl. alten Task selbst auf
            if (s.getState() == GameState.IDLE || s.getState() == GameState.COUNTDOWN) {
                int seconds = plugin.getConfigManager().getCountdownSeconds();
                countdowns.start(s, seconds, () -> match.start(s));
                Bukkit.broadcastMessage("§a[OITC] §7Genug Spieler in §e" + a.getName() + "§7! Countdown startet ...");
                Dbg.d(PlayerJoinHandler.class, "Countdown gestartet: arena=" + a.getName() + " seconds=" + seconds);
            }
        }

        Dbg.d(PlayerJoinHandler.class, "handleJoin: " + p.getName() + " ist aktiver Spieler");
        return true;
    }

    // ============================================================
    // HILFSMETHODEN
    // ============================================================
    private void preparePlayerForJoin(Player p) {
        p.getInventory().clear();
        p.setFoodLevel(20);
        p.setSaturation(5);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setExp(0);
        p.setLevel(0);

        var attr = org.bukkit.attribute.Attribute.MAX_HEALTH;
        if (p.getAttribute(attr) != null) {
            p.getAttribute(attr).setBaseValue(20.0);
            p.setHealth(p.getAttribute(attr).getBaseValue());
        }
    }

    private void broadcastMissingPlayers(Arena a, int missing) {
        String msg = "§7[OITC] Es " + (missing == 1 ? "fehlt" : "fehlen") +
                " noch §e" + missing + " §7Spieler bis zum Start von §e" + a.getName() + "§7.";
        for (Player pl : Bukkit.getOnlinePlayers()) {
            boolean inGame = sessions.byPlayer(pl)
                    .map(GameSession::getState)
                    .map(state -> state == GameState.RUNNING)
                    .orElse(false);
            if (!inGame) pl.sendMessage(msg);
        }
    }

    public GameSession getSession(Player p) {
        return sessions.byPlayer(p).orElse(null);
    }
}

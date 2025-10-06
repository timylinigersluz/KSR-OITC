package ch.ksrminecraft.kSROITC.managers;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.integration.RankPointsHook;
import ch.ksrminecraft.kSROITC.models.*;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class GameManager {

    private final KSROITC plugin;
    private final ArenaManager arenas;
    private final TeleportManager tp;
    private final RankPointsHook rankpoints;

    private final SessionManager sessions;
    private final KitManager kits;
    private final CountdownManager countdowns;
    private final MatchManager match;
    private final CombatManager combat;
    private final ScoreboardManager scoreboards;

    public GameManager(KSROITC plugin, ArenaManager arenas, TeleportManager tp, RankPointsHook rankpoints) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.tp = tp;
        this.rankpoints = rankpoints;

        this.sessions = new SessionManager();
        this.kits = new KitManager();
        this.countdowns = new CountdownManager(plugin);
        this.scoreboards = new ScoreboardManager();
        this.match = new MatchManager(plugin, tp, kits, sessions, scoreboards, rankpoints);
        this.combat = new CombatManager(sessions, kits, match, scoreboards, rankpoints);
    }

    // -------- Persistenz --------
    public void saveToStorage() { sessions.saveToStorage(); }
    public void loadFromStorage(ArenaManager arenaManager) { sessions.loadFromStorage(arenaManager); }
    public SessionManager getSessionManager() { return sessions; }

    // -------- Spiellogik --------

    public boolean join(Player p, String arenaName) {
        Dbg.d(GameManager.class, "join(" + p.getName() + ", arena=" + arenaName + ")");
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
            p.sendMessage("§cKeine Lobby gesetzt (/oitc setlobby).");
            return false;
        }
        if (a.getSpawns().size() < 2) {
            p.sendMessage("§cMindestens 2 Spawns erforderlich (/oitc addspawn).");
            return false;
        }

        // Prüfen, ob Spieler schon in einer Session ist
        if (sessions.byPlayer(p).isPresent()) {
            p.sendMessage("§cDu bist bereits in einer Arena. Nutze /oitc leave.");
            return false;
        }

        // Session holen oder erstellen
        GameSession s = sessions.ensure(a);
        if (s.getState() == GameState.RUNNING && !a.isAllowJoinInProgress()) {
            p.sendMessage("§cDiese Arena läuft bereits.");
            return false;
        }
        if (s.getPlayers().size() >= a.getMaxPlayers()) {
            p.sendMessage("§cArena ist voll.");
            return false;
        }

        // 🧳 Inventar sichern und leeren
        ch.ksrminecraft.kSROITC.utils.InventoryBackupManager.saveInventory(p);
        if (p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
            p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0);
        }
        p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue());

        p.getInventory().clear();
        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.setGameMode(org.bukkit.GameMode.ADVENTURE);

        // Spieler hinzufügen + Teleport
        sessions.addPlayer(p, s);
        tp.toLobby(p, a);
        p.sendMessage("§a[OITC] §7Beigetreten: §e" + a.getName() + " §7(" + s.getPlayers().size() + "/" + a.getMaxPlayers() + ")");

        // Scoreboard optional
        scoreboards.apply(p, s);

        // Schilder aktualisieren
        plugin.getSignManager().updateAllSigns();

        // Auto-Countdown bei genügend Spielern
        if (s.getState() == GameState.IDLE && s.getPlayers().size() >= a.getMinPlayers()) {
            countdowns.start(s, plugin.getConfigManager().getCountdownSeconds(), () -> start(a.getName()));
        }

        return true;
    }

    public void leave(Player p) {
        Optional<GameSession> opt = sessions.byPlayer(p);
        if (opt.isEmpty()) {
            p.sendMessage("§cDu bist in keiner Arena.");
            return;
        }

        GameSession s = opt.get();
        Arena a = s.getArena();

        // Spieler austragen + Teleport
        sessions.removePlayer(p, s);
        tp.toMainLobby(p);
        p.sendMessage("§7[OITC] §cDu hast die Arena §e" + a.getName() + " §cverlassen.");

        // 🧳 Inventar wiederherstellen
        ch.ksrminecraft.kSROITC.utils.InventoryBackupManager.restoreInventory(p);

        // Schilder sofort aktualisieren
        plugin.getSignManager().updateAllSigns();

        // Countdown abbrechen, wenn zu wenige Spieler
        if (s.getState() == GameState.COUNTDOWN && s.getPlayers().size() < a.getMinPlayers()) {
            countdowns.cancel(s, "zu wenige Spieler");
            for (UUID u : s.getPlayers()) {
                Player pl = Bukkit.getPlayer(u);
                if (pl != null) {
                    pl.sendMessage("§cZu wenige Spieler – Countdown abgebrochen.");
                }
            }
        }

        // Wenn letzte Person geht → Match stoppen
        if (s.getPlayers().isEmpty()) {
            countdowns.cancel(s, "keine Spieler mehr");
            match.stop(s, false);
            sessions.clearMappings(s);
        }
    }


    public void start(String arenaName) {
        Dbg.d(GameManager.class, "start(" + arenaName + ")");
        Arena a = arenas.get(arenaName);
        if (a == null) return;

        GameSession s = sessions.ensure(a);
        if (s.getState() == GameState.COUNTDOWN) countdowns.cancel(s, "start()");
        match.start(s);

        plugin.getSignManager().updateAllSigns();
    }

    public void stop(String arenaName) {
        Dbg.d(GameManager.class, "stop(" + arenaName + ")");
        sessions.byArena(arenaName).ifPresent(s -> {
            countdowns.cancel(s, "stop()");
            match.stop(s, false);
            sessions.clearMappings(s);
        });

        plugin.getSignManager().updateAllSigns();
    }

    // --- Delegations ---
    public boolean shouldAllowCombat(Player a, Player b) { return combat.shouldAllowCombat(a, b); }
    public void recordHitArrow(Player victim, boolean byArrow) { combat.recordHit(victim, byArrow); }
    public void handleDeath(Player victim) { combat.handleDeath(victim); }
    public KitManager getKits() { return kits; }
}

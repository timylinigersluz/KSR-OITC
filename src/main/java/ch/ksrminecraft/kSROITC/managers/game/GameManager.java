package ch.ksrminecraft.kSROITC.managers.game;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.integration.RankPointsHook;
import ch.ksrminecraft.kSROITC.managers.arena.ArenaManager;
import ch.ksrminecraft.kSROITC.managers.arena.TeleportManager;
import ch.ksrminecraft.kSROITC.managers.match.MatchController;
import ch.ksrminecraft.kSROITC.managers.match.MatchManager;
import ch.ksrminecraft.kSROITC.managers.system.CountdownManager;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.entity.Player;

/**
 * Zentraler Koordinator des KSR-OITC-Systems.
 * Verteilt Aufgaben an spezialisierte Manager:
 *  - PlayerJoinHandler & PlayerLeaveHandler
 *  - MatchController: Start / Stopp von Matches
 *  - CombatManager: Kills, Tode & Kampfregeln
 *  - SessionManager: Arena- und Spielerzuweisungen
 *  - KitManager: Ausrüstung
 *  - SpectatorManager: Zuschauerverwaltung
 */
public class GameManager {

    private final KSROITC plugin;
    private final ArenaManager arenas;
    private final TeleportManager tp;
    private final RankPointsHook rankpoints;

    // === Basissysteme ===
    private final SpectatorManager spectators;
    private final SessionManager sessions;
    private final KitManager kits;
    private final CountdownManager countdowns;
    private final ScoreboardManager scoreboards;

    // === Spiel-Subsysteme ===
    private final MatchManager match;
    private final CombatManager combat;

    // === High-Level Controller ===
    private final PlayerJoinHandler joinHandler;
    private final PlayerLeaveHandler leaveHandler;
    private final MatchController matchController;

    // ============================================================
    // KONSTRUKTOR
    // ============================================================
    public GameManager(KSROITC plugin, ArenaManager arenas, TeleportManager tp, RankPointsHook rankpoints) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.tp = tp;
        this.rankpoints = rankpoints;

        // Kernkomponenten initialisieren
        this.spectators = new SpectatorManager();
        this.sessions = new SessionManager(spectators);
        this.kits = new KitManager();
        this.countdowns = new CountdownManager(plugin);
        this.scoreboards = new ScoreboardManager();

        // Match & Kampfmanager
        this.match = new MatchManager(plugin, tp, kits, sessions, scoreboards, rankpoints);
        this.combat = new CombatManager(sessions, kits, match, scoreboards, rankpoints, spectators);

        // High-Level Controller
        this.joinHandler = new PlayerJoinHandler(plugin, arenas, tp, rankpoints, sessions, countdowns, scoreboards, match, spectators);
        this.leaveHandler = new PlayerLeaveHandler(plugin, tp, rankpoints, sessions, countdowns, match, spectators);
        this.matchController = new MatchController(plugin, sessions, match, countdowns);

        Dbg.d(GameManager.class, "Initialisiert mit modularen Subsystemen.");
    }

    // ============================================================
    // PERSISTENZ
    // ============================================================
    public void saveToStorage() {
        sessions.saveToStorage();
    }

    public void loadFromStorage(ArenaManager arenaManager) {
        sessions.loadFromStorage(arenaManager);
    }

    // ============================================================
    // SPIELER-STEUERUNG
    // ============================================================

    /**
     * Spieler tritt einer Arena bei.
     * Delegiert an PlayerJoinHandler.
     */
    public boolean join(Player p, String arenaName) {
        return joinHandler.handleJoin(p, arenaName);
    }

    /**
     * Spieler verlässt eine Arena.
     * Delegiert an PlayerLeaveHandler.
     */
    public void leave(Player p) {
        leaveHandler.handleLeave(p);
    }

    // ============================================================
    // MATCH-STEUERUNG
    // ============================================================

    public void start(String arenaName) {
        matchController.startMatch(arenaName);
    }

    public void stop(String arenaName) {
        matchController.stopMatch(arenaName);
    }

    // ============================================================
    // DELEGATIONEN & GETTER
    // ============================================================

    public ArenaManager getArenaManager() { return arenas; }
    public KSROITC getPlugin() { return plugin; }
    public KitManager getKits() { return kits; }
    public ScoreboardManager getScoreboards() { return scoreboards; }
    public CountdownManager getCountdowns() { return countdowns; }
    public MatchManager getMatchManager() { return match; }
    public SessionManager getSessionManager() { return sessions; }
    public SpectatorManager getSpectatorManager() { return spectators; }
    public CombatManager getCombat() { return combat; }
    public MatchController getMatchController() { return matchController; }
    public PlayerJoinHandler getJoinHandler() { return joinHandler; }
    public PlayerLeaveHandler getLeaveHandler() { return leaveHandler; }

    // ============================================================
    // KAMPF-DELEGATIONEN
    // ============================================================

    public boolean shouldAllowCombat(Player a, Player b) {
        return combat.shouldAllowCombat(a, b);
    }

    public void recordHitArrow(Player victim, boolean byArrow) {
        combat.recordHit(victim, byArrow);
    }

    public void handleDeath(Player victim) {
        combat.handleDeath(victim);
    }

    // ============================================================
    // DEBUG
    // ============================================================
    public void debugSessionInfo() {
        for (GameSession s : sessions.allSessions()) {
            Dbg.d(GameManager.class, "Arena=" + s.getArena().getName() +
                    " | State=" + s.getState() +
                    " | Spieler=" + s.getPlayers().size() +
                    " | Spectators=" + countSpectatorsInSession(s));
        }
    }

    private long countSpectatorsInSession(GameSession s) {
        return s.getPlayers().stream()
                .map(org.bukkit.Bukkit::getPlayer)
                .filter(p -> p != null && spectators.isSpectator(p))
                .count();
    }
}

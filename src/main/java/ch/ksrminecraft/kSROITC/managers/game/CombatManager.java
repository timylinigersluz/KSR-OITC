package ch.ksrminecraft.kSROITC.managers.game;

import ch.ksrminecraft.kSROITC.integration.RankPointsHook;
import ch.ksrminecraft.kSROITC.managers.match.MatchManager;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Verantwortlich für alle Kampfereignisse:
 * - Treffer, Kills, Tod durch Umgebung
 * - Killzählung & Siegbedingungen
 * - Blockierung von Zuschauer-Interaktionen
 */
public class CombatManager {

    private final SessionManager sessions;
    private final KitManager kits;
    private final MatchManager match;
    private final ScoreboardManager scoreboards;
    private final RankPointsHook rpHook;
    private final SpectatorManager spectators;

    private final Map<UUID, Boolean> lastHitByArrow = new HashMap<>();
    private final Set<UUID> noKitOnNextRespawn = new HashSet<>();

    public CombatManager(SessionManager sessions,
                         KitManager kits,
                         MatchManager match,
                         ScoreboardManager scoreboards,
                         RankPointsHook rpHook,
                         SpectatorManager spectators) {
        this.sessions = sessions;
        this.kits = kits;
        this.match = match;
        this.scoreboards = scoreboards;
        this.rpHook = rpHook;
        this.spectators = spectators;
    }

    // ============================================================
    // KAMPF-LOGIK
    // ============================================================

    public boolean shouldAllowCombat(Player attacker, Player victim) {
        if (spectators.shouldBlockAttack(attacker, victim)) return false;
        return sessions.sameRunningSession(attacker, victim);
    }

    public void recordHit(Player victim, boolean byArrow) {
        lastHitByArrow.put(victim.getUniqueId(), byArrow);
    }

    public void handleDeath(Player victim) {
        Optional<GameSession> sv = sessions.byPlayer(victim);
        if (sv.isEmpty() || sv.get().getState() != GameState.RUNNING) return;
        GameSession s = sv.get();

        if (sessions.isSpectator(victim)) {
            Dbg.d(CombatManager.class, "handleDeath: spectator=" + victim.getName() + " ignoriert");
            return;
        }

        Player killer = victim.getKiller();

        // === Fall 1: normaler Kill ===
        if (killer != null) {
            if (sessions.isSpectator(killer)) return;

            Optional<GameSession> sk = sessions.byPlayer(killer);
            if (sk.isPresent() && sk.get() == s) {
                int k = s.incrementKills(killer.getUniqueId());
                rpHook.recordKill(killer);

                // Egal womit der Kill erzielt wurde → Pfeil geben
                kits.giveOneArrow(killer);
                Dbg.d(CombatManager.class, "handleDeath: killer=" + killer.getName() +
                        " kills=" + k + " → Pfeil vergeben (Waffe=" +
                        killer.getInventory().getItemInMainHand().getType() + ")");

                // Scoreboard aktualisieren
                scoreboards.updateAll(s);

                // Siegbedingung prüfen
                if (k >= s.getArena().getMaxKills()) {
                    match.endWithWinners(s, "max_kills");
                }
            }
            return;
        }

        // === Fall 2: Tod ohne Killer (Void, Fall, etc.) ===
        lastHitByArrow.remove(victim.getUniqueId());

        boolean hasArrow = victim.getInventory().contains(Material.ARROW);

        if (!hasArrow) {
            Dbg.d(CombatManager.class, "handleDeath: " + victim.getName() +
                    " fiel ins Void ohne Pfeil → Kit ohne Pfeil");
            noKitOnNextRespawn.add(victim.getUniqueId());
            kits.giveKitWithoutArrow(victim, s.getArena().isGiveSword());
            return;
        }

        // Normaler Umwelttod → komplettes Kit
        kits.giveKit(victim, s.getArena().isGiveSword());
        Dbg.d(CombatManager.class, "handleDeath: respawn kit gegeben für " + victim.getName());
    }

    // ============================================================
    // KIT-LOGIK BEIM RESPAWN
    // ============================================================

    public boolean shouldGiveKitOnRespawn(Player p) {
        boolean allowed = !noKitOnNextRespawn.remove(p.getUniqueId());
        Dbg.d(CombatManager.class, "shouldGiveKitOnRespawn(" + p.getName() + ") -> " + allowed);
        return allowed;
    }
}

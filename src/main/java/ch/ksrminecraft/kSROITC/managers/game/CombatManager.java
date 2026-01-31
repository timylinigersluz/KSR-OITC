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
 *
 * Option A:
 * - Kill-Punkte werden NUR hier (während RUNNING) gezählt.
 * - MatchEndManager vergibt am Ende NUR Win-Bonus + commit.
 *
 * WICHTIG (Option 1):
 * - max_kills <= 0 bedeutet: Kill-Limit deaktiviert (kein Abbruch nach Kills).
 *
 * WICHTIG (Attribution-Fix):
 * - Wir verlassen uns NICHT mehr primär auf victim.getKiller().
 * - Stattdessen "reservieren" wir beim Treffer den Killer (First-hit-wins) für kurze Zeit.
 * - Damit werden gleichzeitige Treffer / getKiller()==null / Void nach Hit deutlich robuster.
 */
public class CombatManager {

    private final SessionManager sessions;
    private final KitManager kits;
    private final MatchManager match;
    private final ScoreboardManager scoreboards;
    private final RankPointsHook rpHook;
    private final SpectatorManager spectators;

    private static final long DEATH_DEDUP_MS = 750L;
    private final Map<UUID, Long> lastDeathHandledAt = new HashMap<>();

    private final Map<UUID, Boolean> lastHitByArrow = new HashMap<>();
    private final Set<UUID> noKitOnNextRespawn = new HashSet<>();

    // ============================================================
    // KILL-ATTRIBUTION (FIRST-HIT-WINS)
    // ============================================================

    /**
     * Wenn mehrere Treffer "gleichzeitig" passieren (One-Hit), gewinnt der erste Treffer.
     * Innerhalb dieses Fensters wird der gespeicherte Killer nicht überschrieben.
     */
    private static final long ATTR_RESERVE_WINDOW_MS = 250L;

    /**
     * Wie lange eine Attribution als gültig gilt (z.B. Hit -> Opfer fällt ins Void).
     * Innerhalb dieser Zeit verwenden wir unseren Killer statt Bukkit.getKiller().
     */
    private static final long ATTR_MAX_AGE_MS = 2500L;

    private static final class HitInfo {
        final UUID attacker;
        final long ts;
        final boolean arrow;

        HitInfo(UUID attacker, long ts, boolean arrow) {
            this.attacker = attacker;
            this.ts = ts;
            this.arrow = arrow;
        }
    }

    /** victimUUID -> HitInfo (first hit wins für ATTR_RESERVE_WINDOW_MS) */
    private final Map<UUID, HitInfo> lastHit = new HashMap<>();

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

    /**
     * NEU: Merkt sich den Killer beim Treffer.
     * First-hit-wins: innerhalb ATTR_RESERVE_WINDOW_MS überschreiben wir NICHT.
     * (Caller muss bereits geprüft haben, dass Combat erlaubt ist.)
     */
    public void recordDamage(Player attacker, Player victim, boolean arrowHit) {
        if (attacker == null || victim == null) return;

        // Self niemals tracken
        if (attacker.getUniqueId().equals(victim.getUniqueId())) {
            Dbg.d(CombatManager.class, "recordDamage: IGNORE self-hit attacker=victim=" + attacker.getName());
            return;
        }

        // Safety: keine Spectators + nur in gleicher RUNNING-Session
        if (!shouldAllowCombat(attacker, victim)) {
            Dbg.d(CombatManager.class, "recordDamage: IGNORE not-allowed attacker=" + attacker.getName()
                    + " victim=" + victim.getName());
            return;
        }

        long now = System.currentTimeMillis();
        UUID vid = victim.getUniqueId();
        UUID aid = attacker.getUniqueId();

        HitInfo prev = lastHit.get(vid);
        if (prev != null && (now - prev.ts) <= ATTR_RESERVE_WINDOW_MS) {
            // First-hit-wins: nicht überschreiben
            Dbg.d(CombatManager.class, "recordDamage: KEEP(first-hit) victim=" + victim.getName()
                    + " existingAttacker=" + safeName(prev.attacker)
                    + " newAttacker=" + attacker.getName()
                    + " dt=" + (now - prev.ts) + "ms");
            return;
        }

        lastHit.put(vid, new HitInfo(aid, now, arrowHit));
        Dbg.d(CombatManager.class, "recordDamage: SET victim=" + victim.getName()
                + " attacker=" + attacker.getName()
                + " arrow=" + arrowHit);
    }

    private String safeName(UUID id) {
        try {
            var p = org.bukkit.Bukkit.getPlayer(id);
            return p != null ? p.getName() : id.toString();
        } catch (Throwable t) {
            return String.valueOf(id);
        }
    }

    private Player resolveAttributedKiller(Player victim, GameSession s) {
        if (victim == null || s == null) return null;

        long now = System.currentTimeMillis();
        UUID vid = victim.getUniqueId();

        HitInfo info = lastHit.get(vid);
        if (info == null) {
            Dbg.d(CombatManager.class, "resolveAttributedKiller: NONE victim=" + victim.getName());
            return null;
        }

        long age = now - info.ts;

        // Zu alt -> ungültig
        if (age > ATTR_MAX_AGE_MS) {
            Dbg.d(CombatManager.class, "resolveAttributedKiller: EXPIRED victim=" + victim.getName()
                    + " attacker=" + safeName(info.attacker) + " age=" + age + "ms");
            return null;
        }

        Player killer = org.bukkit.Bukkit.getPlayer(info.attacker);
        if (killer == null) {
            Dbg.d(CombatManager.class, "resolveAttributedKiller: OFFLINE victim=" + victim.getName()
                    + " attacker=" + info.attacker + " age=" + age + "ms");
            return null;
        }

        // Killer muss aktiv sein und in derselben Session (RUNNING)
        if (sessions.isSpectator(killer)) {
            Dbg.d(CombatManager.class, "resolveAttributedKiller: REJECT spectator attacker=" + killer.getName()
                    + " victim=" + victim.getName());
            return null;
        }

        Optional<GameSession> sk = sessions.byPlayer(killer);
        if (sk.isEmpty()) {
            Dbg.d(CombatManager.class, "resolveAttributedKiller: REJECT no-session attacker=" + killer.getName()
                    + " victim=" + victim.getName());
            return null;
        }
        if (sk.get() != s) {
            Dbg.d(CombatManager.class, "resolveAttributedKiller: REJECT other-session attacker=" + killer.getName()
                    + " victim=" + victim.getName()
                    + " attackerArena=" + sk.get().getArena().getName()
                    + " victimArena=" + s.getArena().getName());
            return null;
        }
        if (sk.get().getState() != GameState.RUNNING) {
            Dbg.d(CombatManager.class, "resolveAttributedKiller: REJECT attacker-not-running attacker=" + killer.getName()
                    + " victim=" + victim.getName()
                    + " state=" + sk.get().getState());
            return null;
        }

        Dbg.d(CombatManager.class, "resolveAttributedKiller: OK victim=" + victim.getName()
                + " killer=" + killer.getName()
                + " age=" + age + "ms"
                + " arrow=" + info.arrow);
        return killer;
    }

    public void handleDeath(Player victim) {
        long now = System.currentTimeMillis();
        UUID vid = victim.getUniqueId();

        Long last = lastDeathHandledAt.get(vid);
        if (last != null && (now - last) < DEATH_DEDUP_MS) {
            Dbg.d(CombatManager.class, "handleDeath: DUP ignored victim=" + victim.getName() +
                    " dt=" + (now - last) + "ms");
            return;
        }
        lastDeathHandledAt.put(vid, now);

        Optional<GameSession> sv = sessions.byPlayer(victim);
        if (sv.isEmpty() || sv.get().getState() != GameState.RUNNING) {
            Dbg.d(CombatManager.class, "handleDeath: IGNORE not-running/no-session victim=" + victim.getName());
            return;
        }
        GameSession s = sv.get();

        if (sessions.isSpectator(victim)) {
            Dbg.d(CombatManager.class, "handleDeath: IGNORE spectator victim=" + victim.getName());
            return;
        }

        // ------------------------------------------------------------
        // NEU: Killer bevorzugt über unser Attribution-System
        // ------------------------------------------------------------
        Player killer = resolveAttributedKiller(victim, s);
        boolean killerFromAttribution = (killer != null);

        if (killer == null) {
            killer = victim.getKiller(); // Fallback
            if (killer != null) {
                Dbg.d(CombatManager.class, "handleDeath: fallback Bukkit.getKiller() victim=" + victim.getName()
                        + " killer=" + killer.getName());
            } else {
                Dbg.d(CombatManager.class, "handleDeath: fallback Bukkit.getKiller() victim=" + victim.getName()
                        + " killer=NULL");
            }
        }

        // === Fall 1: Killer vorhanden ===
        if (killer != null) {
            // absolute Safety: Self-Kill nie zählen
            if (killer.getUniqueId().equals(victim.getUniqueId())) {
                Dbg.d(CombatManager.class, "handleDeath: BLOCK self-kill victim=" + victim.getName()
                        + " killer=" + killer.getName()
                        + " source=" + (killerFromAttribution ? "ATTR" : "BUKKIT"));
                // Cleanup
                lastHit.remove(vid);
                lastHitByArrow.remove(vid);
                return;
            }

            if (sessions.isSpectator(killer)) {
                Dbg.d(CombatManager.class, "handleDeath: IGNORE killer-is-spectator victim=" + victim.getName()
                        + " killer=" + killer.getName());
                return;
            }

            Optional<GameSession> sk = sessions.byPlayer(killer);
            if (sk.isPresent() && sk.get() == s) {
                int k = s.incrementKills(killer.getUniqueId());

                // ✅ Option A: Killpunkte NUR hier zählen (während RUNNING)
                if (rpHook != null && rpHook.isEnabled()) {
                    rpHook.recordKill(killer);
                }

                // Pfeil geben
                kits.giveOneArrow(killer);

                Dbg.d(CombatManager.class, "handleDeath: KILL credited victim=" + victim.getName()
                        + " killer=" + killer.getName()
                        + " killsNow=" + k
                        + " source=" + (killerFromAttribution ? "ATTR" : "BUKKIT")
                        + " arena=" + s.getArena().getName());

                // Scoreboard aktualisieren
                scoreboards.updateAll(s);

                // Siegbedingung prüfen (Option 1: Kill-Limit nur, wenn > 0)
                int maxKills = s.getArena().getMaxKills();
                if (maxKills > 0) {
                    if (k >= maxKills) {
                        Dbg.d(CombatManager.class, "handleDeath: END reason=max_kills winner=" + killer.getName()
                                + " killsNow=" + k + "/" + maxKills
                                + " arena=" + s.getArena().getName());
                        match.endWithWinners(s, "max_kills");
                    }
                } else {
                    Dbg.d(CombatManager.class, "handleDeath: max_kills deaktiviert (maxKills=" + maxKills +
                            ") → kein Abbruch über Kills, nur Zeit/andere Kriterien. arena=" + s.getArena().getName());
                }

            } else {
                Dbg.d(CombatManager.class, "handleDeath: KILL rejected (different session?) victim=" + victim.getName()
                        + " killer=" + killer.getName()
                        + " killerSession=" + (sk.isPresent() ? sk.get().getArena().getName() : "NONE")
                        + " victimArena=" + s.getArena().getName()
                        + " source=" + (killerFromAttribution ? "ATTR" : "BUKKIT"));
            }

            // Cleanup: Attribution nach verarbeitetem Tod entfernen
            lastHit.remove(vid);
            lastHitByArrow.remove(vid);
            return;
        }

        // === Fall 2: Tod ohne Killer (Void, Fall, etc.) ===
        lastHit.remove(vid);
        lastHitByArrow.remove(vid);

        boolean hasArrow = victim.getInventory().contains(Material.ARROW);

        if (!hasArrow) {
            Dbg.d(CombatManager.class, "handleDeath: ENV death victim=" + victim.getName()
                    + " hasArrow=false → Kit ohne Pfeil"
                    + " arena=" + s.getArena().getName());
            noKitOnNextRespawn.add(victim.getUniqueId());
            kits.giveKitWithoutArrow(victim, s.getArena().isGiveSword());
            return;
        }

        // Normaler Umwelttod → komplettes Kit
        kits.giveKit(victim, s.getArena().isGiveSword());
        Dbg.d(CombatManager.class, "handleDeath: ENV death victim=" + victim.getName()
                + " hasArrow=true → normales Kit"
                + " arena=" + s.getArena().getName());
    }

    // ============================================================
    // KIT-LOGIK BEIM RESPAWN
    // ============================================================

    public boolean shouldGiveKitOnRespawn(Player p) {
        boolean allowed = !noKitOnNextRespawn.remove(p.getUniqueId());
        Dbg.d(CombatManager.class, "shouldGiveKitOnRespawn(" + p.getName() + ") -> " + allowed);
        return allowed;
    }

    public void clearDeathDedupe() {
        lastDeathHandledAt.clear();
    }

    // ============================================================
    // NEU: CLEANUP fürs Attribution-System
    // ============================================================

    /** Entfernt alle gespeicherten Treffer-Attributionen (global). */
    public void clearAllAttributions() {
        lastHit.clear();
        lastHitByArrow.clear();
        Dbg.d(CombatManager.class, "clearAllAttributions: cleared");
    }

    /** Entfernt Attributionen für alle Spieler dieser Session (sauberer nach Match-Ende). */
    public void clearAttributions(GameSession s) {
        if (s == null) return;
        for (UUID id : new HashSet<>(s.getPlayers())) {
            lastHit.remove(id);
            lastHitByArrow.remove(id);
        }
        Dbg.d(CombatManager.class, "clearAttributions: session=" + s.getArena().getName() + " cleared");
    }
}

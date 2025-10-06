package ch.ksrminecraft.kSROITC.managers;

import ch.ksrminecraft.kSROITC.integration.RankPointsHook;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.entity.Player;

import java.util.*;

public class CombatManager {

    private final SessionManager sessions;
    private final KitManager kits;
    private final MatchManager match;
    private final ScoreboardManager scoreboards;
    private final RankPointsHook rpHook;

    private final Map<UUID, Boolean> lastHitByArrow = new HashMap<>();

    public CombatManager(SessionManager sessions, KitManager kits, MatchManager match, ScoreboardManager scoreboards, RankPointsHook rpHook) {
        this.sessions = sessions;
        this.kits = kits;
        this.match = match;
        this.scoreboards = scoreboards;
        this.rpHook = rpHook;
    }

    public boolean shouldAllowCombat(Player attacker, Player victim) {
        return sessions.sameRunningSession(attacker, victim);
    }

    public void recordHit(Player victim, boolean byArrow) {
        lastHitByArrow.put(victim.getUniqueId(), byArrow);
    }

    public void handleDeath(Player victim) {
        Optional<GameSession> sv = sessions.byPlayer(victim);
        if (sv.isEmpty() || sv.get().getState() != GameState.RUNNING) return;
        GameSession s = sv.get();

        Player killer = victim.getKiller();
        if (killer != null) {
            Optional<GameSession> sk = sessions.byPlayer(killer);
            if (sk.isPresent() && sk.get() == s) {
                int k = s.incrementKills(killer.getUniqueId());
                rpHook.awardKill(killer); // Punkte pro Kill
                boolean byArrow = Boolean.TRUE.equals(lastHitByArrow.remove(victim.getUniqueId()));
                if (byArrow) kits.giveOneArrow(killer);
                scoreboards.updateAll(s);
                Dbg.d(CombatManager.class, "handleDeath: killer=" + killer.getName() + " kills=" + k + " byArrow=" + byArrow);

                if (k >= s.getArena().getMaxKills()) {
                    match.endWithWinners(s, "max_kills");
                }
            }
        } else {
            lastHitByArrow.remove(victim.getUniqueId());
            Dbg.d(CombatManager.class, "handleDeath: environment victim=" + victim.getName());
        }
    }
}

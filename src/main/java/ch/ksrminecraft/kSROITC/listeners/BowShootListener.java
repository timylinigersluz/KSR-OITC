package ch.ksrminecraft.kSROITC.listeners;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.game.GameManager;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.MessageLimiter;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Optional;

public class BowShootListener implements Listener {

    private final GameManager games;

    public BowShootListener(KSROITC plugin) {
        this.games = plugin.getGameManager();
        Dbg.d(BowShootListener.class, "ctor: BowShootListener initialisiert");
    }

    /** Schießen nur in RUNNING; in COUNTDOWN/Lobby wird geblockt. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        Optional<GameSession> sopt = games.getSessionManager().byPlayer(p);
        if (sopt.isEmpty()) return; // außerhalb von OITC: nichts tun

        GameSession s = sopt.get();
        if (s.getState() != GameState.RUNNING) {
            e.setCancelled(true);
            MessageLimiter.sendPlayerMessage(p, "shoot.blocked", "§7Schießen ist gerade deaktiviert.");
            Dbg.d(BowShootListener.class, "cancel shoot state=" + s.getState() + " by=" + p.getName());
            return;
        }

        // Pfeile nicht aufsammelbar machen (kein Pfeil-Duplizieren)
        if (e.getProjectile() instanceof AbstractArrow arr) {
            arr.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
        }

        Dbg.d(BowShootListener.class, "shoot ok by=" + p.getName());
    }

    /** Pfeile nach Einschlag entfernen (sauberer Boden, keine Sammel-Exploits). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent e) {
        if (e.getEntity() instanceof AbstractArrow arr && arr.getShooter() instanceof Player p) {
            if (games.getSessionManager().byPlayer(p).isPresent()) {
                arr.remove();
                Dbg.d(BowShootListener.class, "arrow removed after hit (shooter=" + p.getName() + ")");
            }
        }
    }

    /** Aufheben von Pfeil-Items verhindern, falls doch eines entsteht. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getItem().getItemStack().getType() != Material.ARROW) return;

        Optional<GameSession> sopt = games.getSessionManager().byPlayer(p);
        if (sopt.isEmpty()) return;
        if (sopt.get().getState() == GameState.RUNNING) {
            e.setCancelled(true);
            // Optional: Item entfernen, damit es nicht liegen bleibt
            e.getItem().remove();
            Dbg.d(BowShootListener.class, "cancel arrow pickup by=" + p.getName());
        }
    }
}

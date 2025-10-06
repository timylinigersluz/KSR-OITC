package ch.ksrminecraft.kSROITC.listeners;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.GameManager;
import ch.ksrminecraft.kSROITC.managers.KitManager;
import ch.ksrminecraft.kSROITC.managers.TeleportManager;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Optional;

public class OitcCombatListener implements Listener {

    private final KSROITC plugin;
    private final GameManager games;
    private final TeleportManager tp;
    private final KitManager kits;

    public OitcCombatListener(KSROITC plugin) {
        this.plugin = plugin;
        this.games = plugin.getGameManager();
        this.tp = plugin.getTeleportManager();
        this.kits = games.getKits();
        Dbg.d(OitcCombatListener.class, "ctor: OitcCombatListener initialisiert");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        boolean arrow = false;

        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof Arrow a && a.getShooter() instanceof Player p) {
            attacker = p;
            arrow = true;
        }

        if (attacker == null) return;

        // Isolation + Statuscheck: nur Treffer innerhalb derselben RUNNING-Session
        if (!games.shouldAllowCombat(attacker, victim)) {
            e.setCancelled(true);
            Dbg.d(OitcCombatListener.class, "onDamage: CANCEL " + attacker.getName() + " -> " + victim.getName());
            return;
        }

        // OITC: One-Hit-Kill + merken, ob Pfeil
        e.setDamage(1000.0D);
        games.recordHitArrow(victim, arrow);
        Dbg.d(OitcCombatListener.class, "onDamage: one-hit " + attacker.getName() + " -> " + victim.getName() + " arrow=" + arrow);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Optional<GameSession> sv = games.getSessionManager().byPlayer(victim);
        if (sv.isEmpty() || sv.get().getState() != GameState.RUNNING) return;

        // Death-Nebenwirkungen minimieren
        e.getDrops().clear();
        e.setDroppedExp(0);
        try { e.setDeathMessage(null); } catch (Throwable ignored) {}

        games.handleDeath(victim);
        Dbg.d(OitcCombatListener.class, "onDeath: handled for victim=" + victim.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Optional<GameSession> sopt = games.getSessionManager().byPlayer(p);
        if (sopt.isEmpty()) return;
        GameSession s = sopt.get();

        // Nur während RUNNING aktiv
        if (s.getState() != GameState.RUNNING) return;

        // Respawn-Position auf zufälligen Spawn in der Arena
        Location loc = tp.randomSpawnLocation(s.getArena());
        if (loc != null) e.setRespawnLocation(loc);

        // KEIN Restore des alten Inventars hier!
        // Stattdessen das OITC-Kit neu geben, sobald das Inventar leer ist
        Bukkit.getScheduler().runTask(plugin, () -> {
            kits.giveKit(p, s.getArena().isGiveSword());
        });

        Dbg.d(OitcCombatListener.class, "onRespawn: " + p.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (games.getSessionManager().byPlayer(p).isEmpty()) return;

        games.leave(p);
        Dbg.d(OitcCombatListener.class, "onQuit: " + p.getName() + " -> leave()");
    }
}

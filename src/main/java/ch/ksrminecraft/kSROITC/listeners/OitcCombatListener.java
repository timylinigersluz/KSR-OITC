package ch.ksrminecraft.kSROITC.listeners;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.game.GameManager;
import ch.ksrminecraft.kSROITC.managers.game.KitManager;
import ch.ksrminecraft.kSROITC.managers.arena.TeleportManager;
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

/**
 * Handhabt alle Kampfereignisse und Respawn-Logik von OITC.
 * Enthält automatisches Wiederbeleben (kein "Wiederbeleben"-Button mehr).
 */
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

    // ============================================================
    // SCHADEN / TREFFER
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        boolean arrowHit = false;

        // --- Angreifer bestimmen ---
        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof Arrow a && a.getShooter() instanceof Player p) {
            attacker = p;
            arrowHit = true;
        }

        if (attacker == null) return;

        // --- Nur Treffer innerhalb derselben RUNNING-Session ---
        if (!games.shouldAllowCombat(attacker, victim)) {
            e.setCancelled(true);
            Dbg.d(OitcCombatListener.class, "onDamage: CANCEL " + attacker.getName() + " -> " + victim.getName());
            return;
        }

        String item = attacker.getInventory().getItemInMainHand().getType().toString();

        // === Pfeil → One-Hit ===
        if (arrowHit) {
            e.setDamage(1000.0D);
            games.recordHitArrow(victim, true);
            Dbg.d(OitcCombatListener.class, "onDamage: One-Hit durch Pfeil von " + attacker.getName());
            return;
        }

        // === Schwert ===
        if (item.contains("SWORD")) {
            e.setDamage(6.0D); // ca. 3 Herzen
            games.recordHitArrow(victim, false);
            Dbg.d(OitcCombatListener.class, "onDamage: Schwert-Treffer " + attacker.getName() + " -> " + victim.getName());
            return;
        }

        // === Bogen (Nahkampf) ===
        if (item.contains("BOW")) {
            e.setDamage(3.0D); // halber Schwert-Schaden
            games.recordHitArrow(victim, false);
            Dbg.d(OitcCombatListener.class, "onDamage: Bogen-Nahkampf " + attacker.getName() + " -> " + victim.getName());
            return;
        }

        // === Hand oder andere Items ===
        e.setDamage(1.5D); // Viertel Schwert-Schaden
        games.recordHitArrow(victim, false);
        Dbg.d(OitcCombatListener.class, "onDamage: Hand/sonstiger Treffer " + attacker.getName() + " -> " + victim.getName());
    }


    // ============================================================
    // TOD (mit Auto-Respawn)
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Optional<GameSession> sv = games.getSessionManager().byPlayer(victim);
        if (sv.isEmpty() || sv.get().getState() != GameState.RUNNING) return;

        e.getDrops().clear();
        e.setDroppedExp(0);
        try { e.setDeathMessage(null); } catch (Throwable ignored) {}

        games.handleDeath(victim);
        Dbg.d(OitcCombatListener.class, "onDeath: handled for victim=" + victim.getName());

        // --- Automatisches Wiederbeleben (kein Todesbildschirm) ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (victim.isDead()) {
                try {
                    victim.spigot().respawn();
                    Dbg.d(OitcCombatListener.class, "Auto-Respawn ausgelöst für " + victim.getName());
                } catch (Throwable ex) {
                    Dbg.d(OitcCombatListener.class, "Auto-Respawn fehlgeschlagen für " + victim.getName() + ": " + ex.getMessage());
                }
            }
        }, 1L);
    }

    // ============================================================
    // RESPAWN
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Optional<GameSession> sopt = games.getSessionManager().byPlayer(p);
        if (sopt.isEmpty()) return;
        GameSession s = sopt.get();

        if (s.getState() != GameState.RUNNING) return;

        // Zufälliger Spawn in Arena
        Location loc = tp.randomSpawnLocation(s.getArena());
        if (loc != null) e.setRespawnLocation(loc);

        // --- Verzögerter Inventaraufbau nach 5 Ticks (Client stabil) ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean allow = games.getCombat().shouldGiveKitOnRespawn(p);
            if (allow) {
                kits.giveKit(p, s.getArena().isGiveSword());
                Dbg.d(OitcCombatListener.class, "onRespawn: Kit gegeben an " + p.getName());
            } else {
                // Statt sofortigem Text ein Tick Delay, um Doppelmeldungen zu vermeiden
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        p.sendMessage("§cDu bist ins Void gefallen, bevor du einen neuen Pfeil verdient hast."), 2L);
                kits.giveKitWithoutArrow(p, s.getArena().isGiveSword()); // Neuer Helper
                Dbg.d(OitcCombatListener.class, "onRespawn: Void-Respawn-Kit ohne Pfeil an " + p.getName());
            }
        }, 5L);
    }

    // ============================================================
    // SPIELER VERLÄSST SPIEL
    // ============================================================
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (games.getSessionManager().byPlayer(p).isEmpty()) return;

        games.leave(p);
        Dbg.d(OitcCombatListener.class, "onQuit: " + p.getName() + " -> leave()");
    }
}

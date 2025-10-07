package ch.ksrminecraft.kSROITC.managers.game;

import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Zentrale Verwaltung aller Zuschauer (Spectators) in OITC.
 * Verantwortlich für:
 * - Aktivieren/Deaktivieren des Spectator-Modus
 * - Tracking aller Zuschauer
 * - Schutz vor Interaktionen und DeathEvents
 * - Stabilität beim Teleport / Respawn
 */
public class SpectatorManager {

    /** Alle aktiven Zuschauer */
    private final Set<UUID> spectators = new HashSet<>();

    // ============================================================
    // ZUSTAND
    // ============================================================

    /**
     * Aktiviert oder deaktiviert den Spectator-Modus für einen Spieler.
     * Nur bei Zustandsänderung wird eine Aktion durchgeführt.
     */
    public void setSpectator(Player p, boolean enable) {
        UUID id = p.getUniqueId();

        if (enable) {
            if (spectators.add(id)) {
                // nur, wenn er neu Zuschauer wird
                scheduleApplySpectatorMode(p);
                Dbg.d(SpectatorManager.class, "Spectator ON → " + p.getName());
            } else {
                Dbg.d(SpectatorManager.class, "Spectator ON (bereits aktiv) → " + p.getName());
            }
        } else {
            if (spectators.remove(id)) {
                // nur, wenn er wirklich zuvor Zuschauer war
                scheduleResetSpectatorMode(p);
                Dbg.d(SpectatorManager.class, "Spectator OFF → " + p.getName());
            } else {
                Dbg.d(SpectatorManager.class, "Spectator OFF (war kein Spectator) → " + p.getName());
            }
        }
    }

    /** Prüft, ob der Spieler Zuschauer ist. */
    public boolean isSpectator(Player p) {
        return spectators.contains(p.getUniqueId());
    }

    /** Entfernt alle gespeicherten Zuschauer (z. B. beim Arena-Reset). */
    public void clearAll() {
        spectators.clear();
    }

    // ============================================================
    // VISUELLE ANWENDUNG
    // ============================================================

    /**
     * Wendet den Spectator-Modus mit 1-Tick-Verzögerung an,
     * um Kompatibilitätsprobleme mit Teleports zu vermeiden.
     */
    private void scheduleApplySpectatorMode(Player p) {
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("KSR-OITC"), () -> applySpectatorMode(p), 1L);
    }

    /** Stellt normalen Zustand (Adventure) mit 1-Tick-Delay wieder her. */
    private void scheduleResetSpectatorMode(Player p) {
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("KSR-OITC"), () -> resetSpectatorMode(p), 1L);
    }

    /** Aktiviert den Spectator-Modus visuell und spielmechanisch. */
    private void applySpectatorMode(Player p) {
        if (!p.isOnline()) return;

        p.setGameMode(GameMode.SPECTATOR);
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setCollidable(false);
        p.getInventory().clear();

        try {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.4f);
        } catch (Throwable ignored) {}

        p.sendMessage("§7[OITC] §eDu bist nun Zuschauer. §7Kämpfe können beobachtet, aber nicht beeinflusst werden.");
    }

    /** Setzt Spieler wieder auf Standardwerte zurück, wenn er kein Zuschauer mehr ist. */
    private void resetSpectatorMode(Player p) {
        if (!p.isOnline()) return;

        p.setAllowFlight(false);
        p.setFlying(false);
        p.setCollidable(true);

        if (p.getGameMode() == GameMode.SPECTATOR) {
            p.setGameMode(GameMode.ADVENTURE);
        }
    }

    // ============================================================
    // SCHUTZ-METHODEN FÜR EVENTS
    // ============================================================

    /** Prüft, ob ein Todszenario ignoriert werden soll. */
    public boolean shouldIgnoreDeath(Player p) {
        if (isSpectator(p)) {
            Dbg.d(SpectatorManager.class, "DeathEvent unterdrückt für Zuschauer: " + p.getName());
            return true;
        }
        return false;
    }

    /** Prüft, ob ein Angriff blockiert werden soll (Zuschauer beteiligt). */
    public boolean shouldBlockAttack(Player attacker, Player victim) {
        return isSpectator(attacker) || isSpectator(victim);
    }

    // ============================================================
    // HELFER-FUNKTIONEN
    // ============================================================

    /** Erzwingt, dass ein Spieler sicher im Spectator-Modus bleibt (z. B. nach Respawn). */
    public void ensureSpectatorMode(Player p) {
        if (isSpectator(p) && p.getGameMode() != GameMode.SPECTATOR) {
            Dbg.d(SpectatorManager.class, "ensureSpectatorMode: reapply → " + p.getName());
            scheduleApplySpectatorMode(p);
        }
    }

    /** Entfernt alle Spieler vollständig aus Spectator-Status (z. B. Serverstop). */
    public void forceResetAll(Collection<? extends Player> players) {
        for (Player p : players) {
            if (isSpectator(p)) {
                setSpectator(p, false);
            }
        }
    }
}

package ch.ksrminecraft.kSROITC.listeners;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.arena.ArenaManager;
import ch.ksrminecraft.kSROITC.managers.game.GameManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Optional;

public class PlayerSafetyListener implements Listener {

    private final KSROITC plugin;
    private final GameManager gm;
    private final ArenaManager arenas;

    public PlayerSafetyListener(KSROITC plugin) {
        this.plugin = plugin;
        this.gm = plugin.getGameManager();
        this.arenas = plugin.getArenaManager();
    }

    // ============================================================
    // 1. BEIM JOIN
    // ============================================================
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkAndRelocate(p), 10L);
    }

    // ============================================================
    // 2. BEIM RESPAWN
    // ============================================================
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkAndRelocate(p), 10L);
    }

    // ============================================================
    // PRÜFLOGIK
    // ============================================================
    private void checkAndRelocate(Player p) {
        if (p.hasPermission("oitc.admin")) return; // Admins dürfen bleiben
        if (p.getWorld() == null) return;

        String worldName = p.getWorld().getName();
        Optional<Arena> arenaOpt = arenas.all().stream()
                .filter(a -> a.getWorldName().equalsIgnoreCase(worldName))
                .findFirst();

        if (arenaOpt.isEmpty()) return; // keine Arena → kein Problem

        Arena a = arenaOpt.get();
        Optional<GameSession> sessionOpt = gm.getSessionManager().byArena(a.getName());

        // Wenn keine Session oder nicht laufend
        if (sessionOpt.isEmpty() || sessionOpt.get().getState() != GameState.RUNNING) {
            Dbg.d(PlayerSafetyListener.class, "Schutz-Teleport: " + p.getName() + " war in Arena '" + a.getName() + "' ohne aktives Spiel.");

            // Spectator-Status sicher deaktivieren
            gm.getSpectatorManager().setSpectator(p, false);

            // Teleport in Mainlobby
            gm.getPlugin().getTeleportManager().toMainLobby(p);

            // Nur im Debug-Modus Info an Spieler senden
            if (plugin.getConfig().getBoolean("debug", false)) {
                p.sendMessage("§7[OITC] §cDu wurdest automatisch in die Lobby teleportiert, da kein Spiel läuft.");
            }
        }
    }
}

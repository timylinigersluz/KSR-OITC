package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.arena.ArenaManager;
import ch.ksrminecraft.kSROITC.managers.game.GameManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * /oitc reset [arena|all]
 * Setzt eine oder alle Arenen vollständig zurück:
 * - Stoppt laufende Spiele
 * - Teleportiert alle Spieler & Zuschauer in die Mainlobby
 * - Bereinigt Scoreboards, Inventare, Spectator-Modi
 * - 🧩 Stoppt alle Countdowns zuverlässig
 */
public class ResetSubCommand implements SubCommand {

    @Override public String getName() { return "reset"; }
    @Override public String getPermission() { return "oitc.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        KSROITC plugin = KSROITC.get();
        GameManager gm = plugin.getGameManager();
        ArenaManager am = plugin.getArenaManager();

        if (args.length < 1) {
            sender.sendMessage("§7Verwendung: §e/oitc reset <arena|all>");
            return;
        }

        String target = args[0].toLowerCase(Locale.ROOT);
        Dbg.d(ResetSubCommand.class, "execute by=" + sender.getName() + " target=" + target);

        // ============================================================
        // /oitc reset all
        // ============================================================
        if (target.equals("all")) {
            int arenasProcessed = 0;
            int totalTeleported = 0;

            // 🧩 Countdown-Fix: Alle Countdowns stoppen, bevor Arenen zurückgesetzt werden
            gm.getCountdowns().cleanupAll();

            for (Arena a : am.all()) {
                if (a == null || a.getWorldName() == null) continue;
                Optional<GameSession> sopt = gm.getSessionManager().byArena(a.getName());
                int moved = 0;

                if (sopt.isPresent()) {
                    GameSession s = sopt.get();
                    moved = gm.getMatchManager().getEndManager().resetArena(s, false);
                    if (moved == 0) {
                        moved = resetWorldPlayers(a.getWorldName());
                        Dbg.d(ResetSubCommand.class, "resetArena(Fallback) -> " + a.getName() + " moved=" + moved);
                    }
                } else {
                    moved = resetWorldPlayers(a.getWorldName());
                    Dbg.d(ResetSubCommand.class, "resetArena(WorldOnly) -> " + a.getName() + " moved=" + moved);
                }

                totalTeleported += moved;
                arenasProcessed++;
            }

            sender.sendMessage("§a[OITC] §7Alle Arenen (§e" + arenasProcessed + "§7) wurden zurückgesetzt. §8(§e" + totalTeleported + " Spieler teleportiert§8)");
            Bukkit.broadcastMessage("§a[OITC] §7Alle Arenen wurden §azurückgesetzt§7!");
            Dbg.d(ResetSubCommand.class, "reset all -> arenas=" + arenasProcessed + ", teleported=" + totalTeleported);
            return;
        }

        // ============================================================
        // /oitc reset <arena>
        // ============================================================
        Arena arena = am.get(target);
        if (arena == null) {
            sender.sendMessage("§cArena §e" + target + " §cnicht gefunden.");
            return;
        }

        // 🧩 Countdown-Fix: Countdown für diese Arena stoppen
        gm.getSessionManager().byArena(arena.getName())
                .ifPresent(s -> gm.getCountdowns().cleanup(s));

        Optional<GameSession> sopt = gm.getSessionManager().byArena(arena.getName());
        int moved = 0;

        if (sopt.isPresent()) {
            GameSession s = sopt.get();
            moved = gm.getMatchManager().getEndManager().resetArena(s, true);
            if (moved == 0) {
                moved = resetWorldPlayers(arena.getWorldName());
                Dbg.d(ResetSubCommand.class, "reset single(Fallback) -> " + arena.getName() + " moved=" + moved);
            }
        } else {
            moved = resetWorldPlayers(arena.getWorldName());
            Dbg.d(ResetSubCommand.class, "reset single(WorldOnly) -> " + arena.getName() + " moved=" + moved);
        }

        sender.sendMessage("§a[OITC] §7Arena §e" + arena.getName() + " §7zurückgesetzt. Teleportiert: §e" + moved);
    }

    // ============================================================
    // Hilfsmethode
    // ============================================================
    private int resetWorldPlayers(String worldName) {
        var tp = KSROITC.get().getTeleportManager();
        var specs = KSROITC.get().getGameManager().getSpectatorManager();

        int moved = 0;
        Dbg.d(ResetSubCommand.class, "resetWorldPlayers -> world=" + worldName);

        for (var p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (p.getWorld() == null) continue;
            if (!p.getWorld().getName().equalsIgnoreCase(worldName)) continue;

            specs.setSpectator(p, false);
            p.getInventory().clear();
            p.setFireTicks(0);
            p.setHealth(20);
            p.setFoodLevel(20);
            p.setSaturation(5);
            p.setLevel(0);
            p.setExp(0);

            try {
                tp.toMainLobby(p);
                moved++;
                Dbg.d(ResetSubCommand.class, "teleported " + p.getName() + " from " + worldName + " → main lobby");
            } catch (Exception ex) {
                Dbg.d(ResetSubCommand.class, "Teleport failed for " + p.getName() + ": " + ex.getMessage());
            }

            try {
                ch.ksrminecraft.kSROITC.utils.InventoryBackupManager.restoreInventory(p);
            } catch (Exception ignored) {}

            p.sendMessage("§7[OITC] §aArena wurde zurückgesetzt – du bist zurück in der Mainlobby.");
        }

        if (moved == 0) {
            Dbg.d(ResetSubCommand.class, "resetWorldPlayers: keine Spieler in " + worldName + " gefunden.");
        }

        return moved;
    }
}

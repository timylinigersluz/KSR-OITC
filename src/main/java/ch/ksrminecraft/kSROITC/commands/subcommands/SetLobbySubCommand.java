package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.arena.ArenaManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.models.SimpleLocation;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.LocationUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SetLobbySubCommand implements SubCommand {

    @Override public String getName() { return "setlobby"; }
    @Override public String getPermission() { return "oitc.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Dbg.d(SetLobbySubCommand.class, "execute by=" + sender.getName());
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur im Spiel.");
            return;
        }

        final String worldName = p.getWorld().getName();
        final FileConfiguration cfg = KSROITC.get().getConfig();

        // Mainlobby-Weltname aus der Config holen (falls nicht vorhanden, einmalig initialisieren)
        String mainWorld = cfg.getString("main_lobby.world");
        if (mainWorld == null || mainWorld.isBlank()) {
            cfg.set("main_lobby.world", worldName);
            KSROITC.get().saveConfig();
            mainWorld = worldName;
            Dbg.d(SetLobbySubCommand.class, "main_lobby.world fehlte -> initialisiert auf '" + worldName + "'");
        }

        final boolean isMainLobbyWorld = worldName.equalsIgnoreCase(mainWorld);
        final SimpleLocation sl = LocationUtil.toSimple(p.getLocation());

        if (isMainLobbyWorld) {
            // === Mainlobby speichern ===
            cfg.set("main_lobby.world", worldName);
            cfg.set("main_lobby.x", sl.getX());
            cfg.set("main_lobby.y", sl.getY());
            cfg.set("main_lobby.z", sl.getZ());
            cfg.set("main_lobby.yaw", sl.getYaw());
            cfg.set("main_lobby.pitch", sl.getPitch());
            KSROITC.get().saveConfig();

            // Visuelles Feedback
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    p.getLocation().add(0, 1.0, 0),
                    15, 0.4, 0.4, 0.4, 0);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.1f);

            sender.sendMessage("§aLobbyspawn für §eMainlobby §agesetzt (§e" + worldName + "§a).");
            Dbg.d(SetLobbySubCommand.class, String.format(
                    "Mainlobby gesetzt: world=%s x=%.1f y=%.1f z=%.1f yaw=%.1f pitch=%.1f",
                    worldName, sl.getX(), sl.getY(), sl.getZ(), (double) sl.getYaw(), (double) sl.getPitch()
            ));
            return;
        }

        // === Arena-Lobby speichern ===
        ArenaManager am = KSROITC.get().getArenaManager();
        Arena arena = am.ensure(worldName, worldName);
        arena.setLobby(sl);
        am.persistArena(arena);
        am.saveToStorage(); // 💾 Auto-Save nach Änderung

        // 🪧 Schilder sofort aktualisieren
        KSROITC.get().getSignManager().updateAllSigns();

        // ✨ Partikel + Sound
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                p.getLocation().add(0, 1.0, 0),
                15, 0.4, 0.4, 0.4, 0);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);

        sender.sendMessage("§aLobbyspawn für Arena §e" + arena.getName() + " §agesetzt (§e" + worldName + "§a).");
        Dbg.d(SetLobbySubCommand.class, String.format(
                "Arena-Lobby gesetzt: arena=%s world=%s x=%.1f y=%.1f z=%.1f yaw=%.1f pitch=%.1f (mainWorld=%s)",
                arena.getName(), worldName, sl.getX(), sl.getY(), sl.getZ(), (double) sl.getYaw(), (double) sl.getPitch(), mainWorld
        ));
    }
}

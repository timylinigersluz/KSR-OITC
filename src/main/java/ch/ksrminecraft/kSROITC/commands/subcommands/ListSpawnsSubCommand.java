package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.arena.ArenaManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.models.SimpleLocation;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class ListSpawnsSubCommand implements SubCommand {

    private static final int PARTICLE_DURATION_SECONDS = 30;
    private static final int PARTICLE_INTERVAL_TICKS = 10; // 0.5s

    @Override public String getName() { return "listspawns"; }
    @Override public String getPermission() { return "oitc.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Dbg.d(ListSpawnsSubCommand.class, "execute by=" + sender.getName() + " argsLen=" + args.length);
        if (args.length < 1) {
            sender.sendMessage("§7/oitc listspawns <arena>");
            return;
        }

        String arenaName = args[0];
        ArenaManager am = KSROITC.get().getArenaManager();
        Arena a = am.get(arenaName);

        if (a == null) {
            sender.sendMessage("§cArena nicht gefunden.");
            Dbg.d(ListSpawnsSubCommand.class, "arena not found");
            return;
        }

        World world = Bukkit.getWorld(a.getWorldName());
        if (world == null) {
            sender.sendMessage("§cWelt der Arena ist nicht geladen.");
            return;
        }

        sender.sendMessage("§7Arena §e" + a.getName() + " §7(Welt: §e" + a.getWorldName() + "§7)");

        if (a.getLobby() != null) {
            sender.sendMessage("§7Lobby: §e" + fmt(a.getLobby()));
        } else {
            sender.sendMessage("§7Lobby: §c(keine)");
        }

        if (a.getSpawns().isEmpty()) {
            sender.sendMessage("§7Spawns: §c(keine)");
            return;
        }

        int i = 1;
        for (SimpleLocation sl : a.getSpawns()) {
            sender.sendMessage("§7Spawn #" + (i++) + ": §e" + fmt(sl));
            spawnParticles(world, sl);
        }

        sender.sendMessage("§aSpawnpunkte werden für 30 Sekunden mit Partikeln markiert.");
        Dbg.d(ListSpawnsSubCommand.class, "listed spawns: count=" + a.getSpawns().size());
    }

    /**
     * Spawnt für 30 Sekunden Partikel an einem Spawnpunkt
     */
    private void spawnParticles(World world, SimpleLocation sl) {
        Location loc = new Location(
                world,
                sl.getX() + 0.5,
                sl.getY() + 0.1,
                sl.getZ() + 0.5
        );

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = PARTICLE_DURATION_SECONDS * 20;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                world.spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        loc,
                        6,      // Anzahl
                        0.3,    // X-Offset
                        0.5,    // Y-Offset
                        0.3,    // Z-Offset
                        0.0
                );

                ticks += PARTICLE_INTERVAL_TICKS;
            }
        }.runTaskTimer(KSROITC.get(), 0L, PARTICLE_INTERVAL_TICKS);
    }

    private String fmt(SimpleLocation l) {
        return String.format(
                "x=%.1f y=%.1f z=%.1f yaw=%.1f pitch=%.1f",
                l.getX(), l.getY(), l.getZ(),
                (double) l.getYaw(), (double) l.getPitch()
        );
    }
}

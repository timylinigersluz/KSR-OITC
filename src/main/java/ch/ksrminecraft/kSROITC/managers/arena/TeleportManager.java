package ch.ksrminecraft.kSROITC.managers.arena;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.models.SimpleLocation;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class TeleportManager {

    public void toLobby(Player p, Arena a) {
        Dbg.d(TeleportManager.class, "toLobby(player=" + p.getName() + ", arena=" + a.getName() + ")");
        if (a.getLobby() == null) { Dbg.d(TeleportManager.class, "toLobby: lobby null"); return; }
        World w = Bukkit.getWorld(a.getWorldName());
        if (w == null) { Dbg.d(TeleportManager.class, "toLobby: world not loaded: " + a.getWorldName()); return; }
        SimpleLocation sl = a.getLobby();
        p.teleport(toLocation(w, sl));
        Dbg.d(TeleportManager.class, "toLobby: -> " + fmt(sl));
    }

    public void toRandomSpawn(Player p, Arena a) {
        Location loc = randomSpawnLocation(a);
        if (loc == null) return;
        p.teleport(loc);
        Dbg.d(TeleportManager.class, "toRandomSpawn: " + p.getName());
    }

    /** Liefert eine zufällige Spawn-Location oder null. */
    public Location randomSpawnLocation(Arena a) {
        if (a.getSpawns().isEmpty()) {
            Dbg.d(TeleportManager.class, "randomSpawnLocation: no spawns for " + a.getName());
            return null;
        }
        World w = Bukkit.getWorld(a.getWorldName());
        if (w == null) {
            Dbg.d(TeleportManager.class, "randomSpawnLocation: world not loaded: " + a.getWorldName());
            return null;
        }
        int i = ThreadLocalRandom.current().nextInt(a.getSpawns().size());
        return toLocation(w, a.getSpawns().get(i));
    }

    public void toMainLobby(Player p) {
        FileConfiguration cfg = KSROITC.get().getConfig();
        String world = cfg.getString("main_lobby.world");
        if (world == null) { Dbg.d(TeleportManager.class, "toMainLobby: main_lobby.world missing"); return; }
        World w = Bukkit.getWorld(world);
        if (w == null) { Dbg.d(TeleportManager.class, "toMainLobby: world not loaded: " + world); return; }

        double x = cfg.getDouble("main_lobby.x", 0.5);
        double y = cfg.getDouble("main_lobby.y", 64.0);
        double z = cfg.getDouble("main_lobby.z", 0.5);
        float yaw = (float) cfg.getDouble("main_lobby.yaw", 0.0);
        float pitch = (float) cfg.getDouble("main_lobby.pitch", 0.0);

        p.teleport(new Location(w, x, y, z, yaw, pitch));
        Dbg.d(TeleportManager.class, "toMainLobby: player=" + p.getName() + " world=" + world);
    }

    private Location toLocation(World w, SimpleLocation s) {
        return new Location(w, s.getX(), s.getY(), s.getZ(), s.getYaw(), s.getPitch());
    }

    private String fmt(SimpleLocation s) {
        return String.format("x=%.1f y=%.1f z=%.1f yaw=%.1f pitch=%.1f",
                s.getX(), s.getY(), s.getZ(), (double) s.getYaw(), (double) s.getPitch());
    }
}

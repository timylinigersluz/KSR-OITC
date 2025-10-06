package ch.ksrminecraft.kSROITC.managers;

import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;

public class SpectatorManager {

    // Spieler, die aktuell spectaten (pro Session in GameSession ablegen ist auch ok)
    private final Set<UUID> spectators = new HashSet<>();

    public void setSpectator(Player p, boolean enable) {
        if (enable) {
            p.setGameMode(GameMode.SPECTATOR);
            spectators.add(p.getUniqueId());
            Dbg.d(SpectatorManager.class, "setSpectator ON -> " + p.getName());
        } else {
            p.setGameMode(GameMode.SURVIVAL);
            spectators.remove(p.getUniqueId());
            Dbg.d(SpectatorManager.class, "setSpectator OFF -> " + p.getName());
        }
    }

    public boolean isSpectator(Player p) { return spectators.contains(p.getUniqueId()); }

    public void clearAll() { spectators.clear(); }
}

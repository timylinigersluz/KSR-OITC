package ch.ksrminecraft.kSROITC.listeners;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.game.GameManager;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.MessageLimiter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

/**
 * Verhindert unerlaubte Änderungen in Arenen,
 * erlaubt aber Mechanik-Interaktionen wie Hebel, Knöpfe,
 * Druckplatten, Strings oder Skulksensoren im laufenden Spiel.
 */
public class MiscProtectionListener implements Listener {

    private final KSROITC plugin;
    private final GameManager gm;

    public MiscProtectionListener(KSROITC plugin) {
        this.plugin = plugin;
        this.gm = plugin.getGameManager();
    }

    // --- Blöcke abbauen verbieten ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Optional<GameSession> sopt = gm.getSessionManager().byPlayer(p);
        if (sopt.isEmpty()) return;

        e.setCancelled(true);
        MessageLimiter.sendPlayerMessage(p, "blockBreak", "§cDu darfst hier keine Blöcke abbauen.");
        Dbg.d(MiscProtectionListener.class, "onBlockBreak: cancel by " + p.getName());
    }

    // --- Blöcke platzieren verbieten ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Optional<GameSession> sopt = gm.getSessionManager().byPlayer(p);
        if (sopt.isEmpty()) return;

        e.setCancelled(true);
        MessageLimiter.sendPlayerMessage(p, "blockPlace", "§cDu darfst hier keine Blöcke platzieren.");
        Dbg.d(MiscProtectionListener.class, "onBlockPlace: cancel by " + p.getName());
    }

    // --- Interaktionen differenziert behandeln ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Optional<GameSession> sopt = gm.getSessionManager().byPlayer(p);
        if (sopt.isEmpty()) return;

        GameSession s = sopt.get();
        Block b = e.getClickedBlock();

        // Interaktion mit Luft ignorieren
        if (b == null) return;

        Material type = b.getType();

        // --- Erlaubte Mechanik-Interaktionen ---
        boolean isMechanic =
                type == Material.LEVER ||
                        type.name().contains("BUTTON") ||
                        type.name().contains("PRESSURE_PLATE") ||
                        type.name().contains("TRIPWIRE") ||
                        type.name().contains("STRING") ||
                        type.name().contains("SKULK_SENSOR") ||
                        type.name().contains("SKULK_SHRIEKER") ||
                        type.name().contains("DOOR") ||
                        type.name().contains("TRAPDOOR") ||
                        type.name().contains("FENCE_GATE");

        // --- Laufendes Spiel → Hebel, Knöpfe etc. erlaubt ---
        if (s.getState() == GameState.RUNNING && isMechanic) {
            Dbg.d(MiscProtectionListener.class, "onInteract: allowed mechanic " + type + " by " + p.getName());
            return; // keine Blockierung
        }

        // --- Sonst: blockieren (z. B. Kisten, ItemFrames, etc.) ---
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            MessageLimiter.sendPlayerMessage(p, "blockInteract", "§cInteraktionen mit diesem Block sind im Spiel deaktiviert.");
            Dbg.d(MiscProtectionListener.class, "onInteract: cancel " + type + " by " + p.getName());
        }
    }
}

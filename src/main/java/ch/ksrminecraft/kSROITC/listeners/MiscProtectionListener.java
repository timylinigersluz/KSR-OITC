package ch.ksrminecraft.kSROITC.listeners;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.GameManager;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.MessageLimiter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.Optional;

public class MiscProtectionListener implements Listener {

    private final GameManager games;

    public MiscProtectionListener(KSROITC plugin) {
        this.games = plugin.getGameManager();
        Dbg.d(MiscProtectionListener.class, "ctor: MiscProtectionListener initialisiert");
    }

    private boolean isProtected(Player p) {
        Optional<GameSession> s = games.getSessionManager().byPlayer(p);
        return s.isPresent() && (s.get().getState() == GameState.COUNTDOWN || s.get().getState() == GameState.RUNNING);
    }

    // ---- Block Break/Place ----
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission("oitc.bypass")) return;
        if (isProtected(p)) {
            e.setCancelled(true);
            MessageLimiter.sendPlayerMessage(p, "protect.build", "§7In OITC ist Bauen aktuell deaktiviert.");
            Dbg.d(MiscProtectionListener.class, "cancel BlockBreak by " + p.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission("oitc.bypass")) return;
        if (isProtected(p)) {
            e.setCancelled(true);
            MessageLimiter.sendPlayerMessage(p, "protect.build", "§7In OITC ist Bauen aktuell deaktiviert.");
            Dbg.d(MiscProtectionListener.class, "cancel BlockPlace by " + p.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission("oitc.bypass")) return;
        if (isProtected(p)) {
            e.setCancelled(true);
            MessageLimiter.sendPlayerMessage(p, "protect.drop", "§7Droppen ist während OITC deaktiviert.");
            Dbg.d(MiscProtectionListener.class, "cancel Drop by " + p.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (p.hasPermission("oitc.bypass")) return;
        if (isProtected(p)) {
            e.setCancelled(true);
            MessageLimiter.sendPlayerMessage(p, "protect.inv", "§7Inventar verschieben ist während OITC deaktiviert.");
            Dbg.d(MiscProtectionListener.class, "cancel InventoryClick by " + p.getName());
        }
    }
}

package ch.ksrminecraft.kSROITC.listeners;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.arena.SignManager;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.MessageLimiter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Globaler Schutz für ALLE OITC-Welten (Lobby + Arenen), unabhängig von Sessions.
 *
 * Regeln:
 * - Non-Mod:
 *   - in ALLEN OITC-Welten: kein BlockBreak, kein BlockPlace
 *   - in Lobby-Welt: keine Interaktionen, AUSSER Klick auf registriertes OITC-Join-Schild
 *   - in Arena-Welten: NUR Buttons & Hebel sind ok, alles andere verboten
 * - Mod (oitc.mod): keine Einschränkung durch diesen Listener
 *
 * Wenn debug=false → keine Nachrichten an Spieler, nur interne Logs.
 */
public class MiscProtectionListener implements Listener {

    private final KSROITC plugin;
    private final SignManager signManager;

    public MiscProtectionListener(KSROITC plugin) {
        this.plugin = plugin;
        this.signManager = plugin.getSignManager();
    }

    private boolean isDebug() {
        return plugin.getConfig().getBoolean("debug", false);
    }

    private boolean isMod(Player p) {
        return p.hasPermission("oitc.mod");
    }

    private String lobbyWorldName() {
        return plugin.getConfig().getString("main_lobby.world", "");
    }

    /**
     * Arena-Welten aus config:
     * arenas:
     *   arena1:
     *     world: "arena1"
     */
    private Set<String> arenaWorldNames() {
        Set<String> set = new HashSet<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("arenas");
        if (sec == null) return set;

        for (String arenaKey : sec.getKeys(false)) {
            String w = plugin.getConfig().getString("arenas." + arenaKey + ".world", "");
            if (w != null && !w.isBlank()) set.add(w);
        }
        return set;
    }

    private boolean isLobbyWorld(World w) {
        if (w == null) return false;
        String lw = lobbyWorldName();
        return lw != null && !lw.isBlank() && w.getName().equalsIgnoreCase(lw);
    }

    private boolean isArenaWorld(World w) {
        if (w == null) return false;
        String name = w.getName();
        for (String aw : arenaWorldNames()) {
            if (name.equalsIgnoreCase(aw)) return true;
        }
        return false;
    }

    private boolean isOitcWorld(World w) {
        return isLobbyWorld(w) || isArenaWorld(w);
    }

    private void dbgMsg(Player p, String key, String msg) {
        if (!isDebug()) return;
        MessageLimiter.sendPlayerMessage(p, key, msg);
    }

    // --- Blöcke abbauen verbieten (Non-Mod in ALLEN OITC-Welten) ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();

        if (isMod(p)) return; // Mod: alles erlaubt
        if (!isOitcWorld(e.getBlock().getWorld())) return; // nur OITC-Welten

        e.setCancelled(true);

        // ✅ FIX: Wenn ein Spieler ein Schild "anschlägt" und der Break gecancelt wird,
        // kann der Client den Text verlieren. Daher: Schild-State 1 Tick später neu senden.
        if (e.getBlock().getState() instanceof Sign signState) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    signState.update(true, false);
                } catch (Throwable ignored) {}
            });
        }

        dbgMsg(p, "oitc.protect.break", "§cDu darfst in OITC-Welten keine Blöcke abbauen.");
        Dbg.d(MiscProtectionListener.class, "onBlockBreak: cancel in OITC world by " + p.getName());
    }

    // --- Blöcke platzieren verbieten (Non-Mod in ALLEN OITC-Welten) ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();

        if (isMod(p)) return; // Mod: alles erlaubt
        if (!isOitcWorld(e.getBlock().getWorld())) return; // nur OITC-Welten

        e.setCancelled(true);
        dbgMsg(p, "oitc.protect.place", "§cDu darfst in OITC-Welten keine Blöcke platzieren.");
        Dbg.d(MiscProtectionListener.class, "onBlockPlace: cancel in OITC world by " + p.getName());
    }

    // --- Interaktionen regeln (weltbasiert, unabhängig von Sessions) ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (isMod(p)) return; // Mod: alles erlaubt

        Block b = e.getClickedBlock();
        if (b == null) return;

        if (!isOitcWorld(b.getWorld())) return; // nur OITC-Welten

        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_BLOCK && a != Action.LEFT_CLICK_BLOCK) return;

        // --- Lobby: einzig erlaubte Interaktion = registriertes OITC-Join-Schild ---
        if (isLobbyWorld(b.getWorld())) {
            if (b.getState() instanceof Sign) {
                Optional<String> arena = signManager.arenaFor(b);
                if (arena.isPresent()) {
                    // OITC-Join-Schild ist ok (SignListener übernimmt Join-Logik)
                    Dbg.d(MiscProtectionListener.class, "onInteract: allowed OITC sign in lobby by " + p.getName());
                    return;
                }
            }

            e.setCancelled(true);
            dbgMsg(p, "oitc.protect.lobby.interact", "§cIn der OITC-Lobby sind Interaktionen deaktiviert.");
            Dbg.d(MiscProtectionListener.class, "onInteract: cancel in lobby (" + b.getType() + ") by " + p.getName());
            return;
        }

        // --- Arenen: NUR Hebel & Buttons erlaubt, alles andere verboten ---
        if (isArenaWorld(b.getWorld())) {
            Material type = b.getType();
            boolean allowedMechanic =
                    type == Material.LEVER ||
                            type.name().contains("BUTTON");

            if (allowedMechanic) {
                Dbg.d(MiscProtectionListener.class, "onInteract: allowed mechanic " + type + " by " + p.getName());
                return;
            }

            e.setCancelled(true);
            dbgMsg(p, "oitc.protect.arena.interact", "§cInteraktionen sind in OITC-Arenen (ausser Hebel/Buttons) deaktiviert.");
            Dbg.d(MiscProtectionListener.class, "onInteract: cancel in arena (" + type + ") by " + p.getName());
        }
    }
}

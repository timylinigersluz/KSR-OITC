package ch.ksrminecraft.kSROITC.listeners;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.arena.SignManager;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.MessageLimiter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignListener implements Listener {

    private final KSROITC plugin;
    private final SignManager signs;

    public SignListener(KSROITC plugin) {
        this.plugin = plugin;
        this.signs  = plugin.getSignManager();
        Dbg.d(SignListener.class, "ctor: SignListener ready");
    }

    // ============================================================
    // 🪧 Schild-Erstellung
    // ============================================================
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSignCreate(SignChangeEvent e) {
        Player p = e.getPlayer();
        var plain = PlainTextComponentSerializer.plainText();

        String l1 = e.line(0) == null ? "" : plain.serialize(e.line(0)).trim();
        String l2 = e.line(1) == null ? "" : plain.serialize(e.line(1)).trim();

        // Erkennung unabhängig von Farb-/Formatcodes
        if ("[OITC]".equalsIgnoreCase(l1.replace("&", "").replace("§", ""))) {
            if (l2.isEmpty()) {
                MessageLimiter.sendPlayerMessage(p, "sign.create.empty", "§c2. Zeile: Arenaname erwartet.");
                return;
            }

            // Registrieren
            signs.registerSign(e.getBlock(), l2);

            // Direkt hübsch formatieren
            e.line(0, Component.text("[OITC]").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
            e.line(1, Component.text(l2).color(NamedTextColor.WHITE));
            e.line(2, Component.text("wird aktualisiert...").color(NamedTextColor.GRAY));
            e.line(3, Component.empty());

            MessageLimiter.sendPlayerMessage(p, "sign.create.ok", "§aOITC-Schild registriert für §e" + l2);
        }
    }

    // ============================================================
    // ❌ Schild-Abbau
    // ============================================================
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSignBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!(b.getState() instanceof Sign)) return;
        signs.unregisterSign(b);
    }

    // ============================================================
    // 🖱️ Klick auf Schild
    // ============================================================
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSignClick(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        if (!(b.getState() instanceof Sign)) return;

        Player p = e.getPlayer();

        signs.arenaFor(b).ifPresent(arena -> {
            // Creative darf bearbeiten, nicht joinen
            if (p.getGameMode() == GameMode.CREATIVE) {
                Dbg.d(SignListener.class, p.getName() + " klickt Schild im Creative-Modus → darf bearbeiten.");
                return;
            }

            // Nur Survival darf joinen
            if (p.getGameMode() != GameMode.SURVIVAL) {
                MessageLimiter.sendPlayerMessage(p, "sign.join.invalidmode",
                        "§cDu kannst Arenen nur im §eSurvival-Modus §cbetreten.");
                e.setCancelled(true);
                Dbg.d(SignListener.class, p.getName() + " im " + p.getGameMode() + "-Modus → kein Join erlaubt.");
                return;
            }

            // Survival → Join
            e.setCancelled(true); // kein Edit/Rotate
            Dbg.d(SignListener.class, p.getName() + " klickt Schild → versuche Join in " + arena);
            signs.handleClick(p, arena, plugin.getGameManager());
        });
    }
}

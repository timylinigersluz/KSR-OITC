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

    private boolean isTournamentLockdown() {
        return plugin.getTournamentManager().isEnabled();
    }

    private boolean isMod(Player p) {
        return p.hasPermission("oitc.mod");
    }

    private boolean isOitcHeader(String line) {
        if (line == null) return false;
        String cleaned = line.trim().replace("&", "").replace("§", "");
        return "[OITC]".equalsIgnoreCase(cleaned);
    }

    // ============================================================
    // 🪧 Schild-Erstellung / Umbenennen / Ändern (NUR OITC-Schilder)
    // ============================================================
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSignCreateOrEdit(SignChangeEvent e) {
        Player p = e.getPlayer();
        var plain = PlainTextComponentSerializer.plainText();

        String l1 = e.line(0) == null ? "" : plain.serialize(e.line(0)).trim();
        String l2 = e.line(1) == null ? "" : plain.serialize(e.line(1)).trim();

        boolean isOitcSignEdit = isOitcHeader(l1) || signs.arenaFor(e.getBlock()).isPresent();
        if (!isOitcSignEdit) return; // normale Schilder: nichts definieren

        // A) Turniermodus EIN: gar nichts mit OITC-Schildern
        if (isTournamentLockdown()) {
            e.setCancelled(true);
            MessageLimiter.sendPlayerMessage(p, "sign.tournament.locked",
                    "§cTurniermodus aktiv – OITC-Schilder sind gesperrt.");
            Dbg.d(SignListener.class, p.getName() + " versucht OITC-Schild zu ändern (Turniermodus) -> blockiert");
            return;
        }

        // B) Turniermodus AUS:
        // Mod nur im Creative darf OITC-Schilder platzieren/umbenennen/ändern
        if (!isMod(p) || p.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            MessageLimiter.sendPlayerMessage(p, "sign.edit.denied",
                    "§cOITC-Schilder dürfen nur von §eMods im Creative§c erstellt oder bearbeitet werden.");
            Dbg.d(SignListener.class, p.getName() + " versucht OITC-Schild zu ändern (kein Mod-Creative) -> blockiert");
            return;
        }

        // Ab hier: Mod + Creative -> erlaubt

        // Wenn es als neues OITC-Schild erstellt wird ([OITC] in Zeile 1), registrieren/formatieren
        if (isOitcHeader(l1)) {
            if (l2.isEmpty()) {
                MessageLimiter.sendPlayerMessage(p, "sign.create.empty", "§c2. Zeile: Arenaname erwartet.");
                // Nicht canceln, damit der Spieler weiter editieren kann
                return;
            }

            signs.registerSign(e.getBlock(), l2);

            e.line(0, Component.text("[OITC]").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
            e.line(1, Component.text(l2).color(NamedTextColor.WHITE));
            e.line(2, Component.text("wird aktualisiert...").color(NamedTextColor.GRAY));
            e.line(3, Component.empty());

            MessageLimiter.sendPlayerMessage(p, "sign.create.ok", "§aOITC-Schild registriert für §e" + l2);
            Dbg.d(SignListener.class, p.getName() + " erstellt OITC-Schild -> registriert arena=" + l2);
        } else {
            // Existing registered OITC sign edited by mod in creative:
            // Wir lassen Edit zu, aber aktualisieren danach wieder live (damit der Inhalt wieder "korrekt" ist)
            // -> OITC-Schilder sind eigentlich "system-managed"
            // Falls du Edits komplett verhindern willst, hier einfach canceln.
            signs.updateAllSigns();
            Dbg.d(SignListener.class, p.getName() + " bearbeitet registriertes OITC-Schild (Creative) -> updateAllSigns()");
        }
    }

    // ============================================================
    // ❌ Schild-Abbau (NUR OITC-Schilder)
    // ============================================================
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSignBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!(b.getState() instanceof Sign)) return;

        // nur OITC-Schilder regeln
        if (signs.arenaFor(b).isEmpty()) return;

        Player p = e.getPlayer();

        // A) Turniermodus EIN: gar nichts
        if (isTournamentLockdown()) {
            e.setCancelled(true);
            MessageLimiter.sendPlayerMessage(p, "sign.tournament.locked",
                    "§cTurniermodus aktiv – OITC-Schilder sind gesperrt.");
            Dbg.d(SignListener.class, p.getName() + " versucht OITC-Schild abzubauen (Turniermodus) -> blockiert");
            return;
        }

        // B) Turniermodus AUS: nur Mod im Creative darf abbauen
        if (!isMod(p) || p.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            MessageLimiter.sendPlayerMessage(p, "sign.break.denied",
                    "§cOITC-Schilder dürfen nur von §eMods im Creative§c abgebaut werden.");
            Dbg.d(SignListener.class, p.getName() + " versucht OITC-Schild abzubauen (kein Mod-Creative) -> blockiert");
            return;
        }

        // erlaubt -> unregister
        signs.unregisterSign(b);
        Dbg.d(SignListener.class, p.getName() + " baut OITC-Schild ab -> unregister");
    }

    // ============================================================
    // 🖱️ Klick auf Schild (NUR OITC-Schilder)
    // ============================================================
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSignClick(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        if (!(b.getState() instanceof Sign)) return;

        Player p = e.getPlayer();

        signs.arenaFor(b).ifPresent(arena -> {
            // A) Turniermodus EIN: gar nichts
            if (isTournamentLockdown()) {
                e.setCancelled(true);
                MessageLimiter.sendPlayerMessage(p, "sign.tournament.locked",
                        "§cTurniermodus aktiv – OITC-Schilder sind gesperrt.");
                Dbg.d(SignListener.class, p.getName() + " klickt OITC-Schild (Turniermodus) -> blockiert");
                return;
            }

            // B) Turniermodus AUS:
            // Mod im Survival soll interagieren (joinen) können, aber nicht bearbeiten.
            // Spieler in Creative sollen NICHT joinen (und können sonst normal editieren – aber fürs OITC-Schild verhindern wir das via Cancel)
            if (p.getGameMode() == GameMode.CREATIVE) {
                e.setCancelled(true);
                Dbg.d(SignListener.class, p.getName() + " klickt OITC-Schild im Creative -> cancelled (kein Join, kein Edit per Klick)");
                return;
            }

            if (p.getGameMode() != GameMode.SURVIVAL) {
                e.setCancelled(true);
                MessageLimiter.sendPlayerMessage(p, "sign.join.invalidmode",
                        "§cDu kannst Arenen nur im §eSurvival-Modus §cbetreten.");
                Dbg.d(SignListener.class, p.getName() + " im " + p.getGameMode() + " klickt OITC -> blockiert");
                return;
            }

            // Survival → Join (für Mod und Non-Mod, Turniermodus ist ja aus)
            e.setCancelled(true);
            Dbg.d(SignListener.class, p.getName() + " klickt OITC-Schild -> Join arena=" + arena);
            signs.handleClick(p, arena, plugin.getGameManager());
        });
    }
}

package ch.ksrminecraft.kSROITC.managers.arena;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.game.GameManager;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import ch.ksrminecraft.kSROITC.utils.DataStorage;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.MessageLimiter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Verwaltet Arena-Join-Schilder mit automatischer Aktualisierung
 * und JSON-basierter Persistenz über DataStorage.
 */
public class SignManager {

    private final KSROITC plugin;
    private final Map<Location, String> signs = new HashMap<>();

    public SignManager(KSROITC plugin) {
        this.plugin = plugin;
        startUpdater();
        Dbg.d(SignManager.class, "ctor: SignManager initialisiert");
    }

    // -------------------------
    // Registrierung / Verwaltung
    // -------------------------

    public void registerSign(Block block, String arenaName) {
        Location loc = block.getLocation();
        signs.put(loc, arenaName.toLowerCase(Locale.ROOT));
        plugin.getLogger().info("[OITC] Schild registriert für Arena '" + arenaName + "'");
        Dbg.d(SignManager.class, "registerSign " + fmt(loc) + " -> " + arenaName);
        updateAllSigns(); // direkt anzeigen
    }

    public void unregisterSign(Block block) {
        Location loc = block.getLocation();
        if (signs.remove(loc) != null) {
            plugin.getLogger().info("[OITC] Schild entfernt: " + fmt(loc));
            Dbg.d(SignManager.class, "unregisterSign " + fmt(loc));
            updateAllSigns();
        }
    }

    public Optional<String> arenaFor(Block block) {
        return Optional.ofNullable(signs.get(block.getLocation()));
    }

    // -------------------------
    // Klicklogik
    // -------------------------

    public void handleClick(Player p, String arenaName, GameManager games) {
        // 🔧 Globale und spielerbezogene Spam-Protection
        if (!MessageLimiter.canSend(p, "sign_click")) {
            return; // Falls der Spieler gerade eine Nachricht erhalten hat → keine neue senden
        }

        Optional<GameSession> sopt = games.getSessionManager().byArena(arenaName);
        GameState st = sopt.map(GameSession::getState).orElse(GameState.IDLE);

        // === Wenn Runde läuft ===
        if (st == GameState.RUNNING) {
            MessageLimiter.sendPlayerMessage(
                    p,
                    "sign_click",
                    "§cDiese Runde läuft bereits – Beitritt ist aktuell nicht möglich."
            );

            // Hinweis auf Spectator-Join anzeigen
            boolean allowSpectate = plugin.getConfig().getBoolean("signs.allow_spectate_on_running", true);
            if (allowSpectate) {
                MessageLimiter.sendPlayerMessage(
                        p,
                        "sign_spectate_hint",
                        "§7Du kannst mit §e/oitc join " + arenaName + " §7als Zuschauer beitreten."
                );
            }
            return;
        }

        // === Wenn Countdown läuft ===
        if (st == GameState.COUNTDOWN) {
            MessageLimiter.sendPlayerMessage(
                    p,
                    "sign_click",
                    "§eDer Countdown läuft bereits – du kannst noch beitreten!"
            );
        }

        // === Normaler Join ===
        games.join(p, arenaName);
        updateAllSigns();
    }



    // -------------------------
    // Live-Update-System
    // -------------------------

    private void startUpdater() {
        int interval = Math.max(1, plugin.getConfig().getInt("signs.update_interval_ticks", 20));
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllSigns, interval, interval);
        Dbg.d(SignManager.class, "startUpdater: alle " + interval + " Ticks");
    }

    /**
     * Öffentliche Methode, um Schilder sofort zu aktualisieren.
     * Wird auch aus GameManager / CountdownManager / MatchManager aufgerufen.
     */
    public void updateAllSigns() {
        // Immer auf dem Main-Thread ausführen
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::updateAllSigns);
            return;
        }

        if (signs.isEmpty()) return;

        for (Map.Entry<Location, String> e : signs.entrySet()) {
            Block b = e.getKey().getBlock();
            if (!(b.getState() instanceof Sign sign)) continue;

            String arena = e.getValue();
            var sopt = plugin.getGameManager().getSessionManager().byArena(arena);
            GameState st = sopt.map(GameSession::getState).orElse(GameState.IDLE);

            Component l1 = Component.text("[OITC]").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD);
            Component l2 = Component.text(arena).color(NamedTextColor.WHITE);
            Component l3, l4;

            if (st == GameState.COUNTDOWN) {
                long left = sopt.map(GameSession::getCountdownEndTimestamp)
                        .map(ts -> Math.max(0, (ts - System.currentTimeMillis()) / 1000L)).orElse(0L);
                l3 = Component.text("Countdown").color(NamedTextColor.RED);
                l4 = Component.text(formatTime(left)).color(NamedTextColor.DARK_RED);
            } else if (st == GameState.RUNNING) {
                long left = sopt.map(GameSession::getEndTimestamp)
                        .map(ts -> ts > 0 ? Math.max(0, (ts - System.currentTimeMillis()) / 1000L) : -1L).orElse(-1L);
                l3 = Component.text("Spiel läuft").color(NamedTextColor.RED);
                l4 = Component.text(formatTime(left)).color(NamedTextColor.RED);
            } else {
                int players = sopt.map(s -> s.getPlayers().size()).orElse(0);
                int min = sopt.map(s -> s.getArena().getMinPlayers()).orElse(2);
                l3 = Component.text("Bereit").color(NamedTextColor.GREEN);
                l4 = Component.text(players + " von min. " + min + " Spieler").color(NamedTextColor.GRAY);
            }

            try {
                var front = sign.getSide(Side.FRONT);
                front.line(0, l1);
                front.line(1, l2);
                front.line(2, l3);
                front.line(3, l4);
                sign.update(true, false);
            } catch (Throwable ex) {
                Dbg.d(SignManager.class, "updateAllSigns: Fehler bei " + fmt(b.getLocation()) + ": " + ex.getMessage());
            }
        }
    }

    // -------------------------
    // Persistenz
    // -------------------------

    public void saveToStorage() {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<Location, String> e : signs.entrySet()) {
            Location l = e.getKey();
            String key = l.getWorld().getName() + ":" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
            map.put(key, e.getValue());
        }
        DataStorage.saveSigns(map);
        Dbg.d(SignManager.class, "saveToStorage: " + signs.size() + " Schilder gespeichert.");
    }

    public void loadFromStorage() {
        Map<String, String> map = DataStorage.loadSigns();
        signs.clear();

        for (Map.Entry<String, String> e : map.entrySet()) {
            try {
                String[] parts = e.getKey().split(":");
                if (parts.length != 2) continue;
                World w = Bukkit.getWorld(parts[0]);
                if (w == null) continue;

                String[] xyz = parts[1].split(",");
                Location loc = new Location(w,
                        Integer.parseInt(xyz[0]),
                        Integer.parseInt(xyz[1]),
                        Integer.parseInt(xyz[2]));
                signs.put(loc, e.getValue());
            } catch (Exception ex) {
                plugin.getLogger().warning("[OITC] Fehler beim Laden eines Schilds: " + e.getKey());
            }
        }

        Dbg.d(SignManager.class, "loadFromStorage: " + signs.size() + " Schilder wiederhergestellt.");
        updateAllSigns(); // Nach Laden sofort aktualisieren
    }

    // -------------------------
    // Hilfsmethoden
    // -------------------------

    private String fmt(Location l) {
        return l.getWorld().getName() + "@" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }
    private String formatTime(long seconds) {
        if (seconds < 0) return "∞";
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }
}

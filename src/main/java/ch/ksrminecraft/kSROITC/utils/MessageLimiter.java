package ch.ksrminecraft.kSROITC.utils;

import ch.ksrminecraft.kSROITC.KSROITC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MessageLimiter {

    private static final Map<String, Long> cooldowns = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();

    private static long globalCooldown;
    private static long playerCooldown;

    private MessageLimiter() {}

    /** Im onEnable() und nach /oitc reload aufrufen. */
    public static void init(KSROITC plugin) {
        globalCooldown = plugin.getConfig().getLong("message-cooldown.global", 3000);
        playerCooldown = plugin.getConfig().getLong("message-cooldown.player", 2000);
        Dbg.d(MessageLimiter.class,
                "init: global=" + globalCooldown + "ms, player=" + playerCooldown + "ms");
    }

    // ============================================================
    // 🟢 Globale Nachrichten
    // ============================================================
    public static void sendBroadcast(String key, String message) {
        if (isOnCooldown("broadcast:" + key, globalCooldown)) return;
        Bukkit.broadcastMessage(message);
        setCooldown("broadcast:" + key);
    }

    // ============================================================
    // 🟡 Konsolen-Logs (nicht Debug-Logs)
    // ============================================================
    public static void logConsole(String key, String message) {
        if (isOnCooldown("console:" + key, globalCooldown)) return;
        Bukkit.getLogger().info(message);
        setCooldown("console:" + key);
    }

    // ============================================================
    // 🔵 Spieler-Nachrichten (mit Cooldown)
    // ============================================================
    public static void sendPlayerMessage(Player player, String key, String message) {
        UUID uuid = player.getUniqueId();
        playerCooldowns.putIfAbsent(uuid, new HashMap<>());
        Map<String, Long> map = playerCooldowns.get(uuid);

        Long last = map.get(key);
        if (last != null && (System.currentTimeMillis() - last) < playerCooldown) {
            return; // noch auf Cooldown
        }

        player.sendMessage(message);
        map.put(key, System.currentTimeMillis());
    }

    // ============================================================
    // 🧠 Prüfung, ob Spieler überhaupt eine Nachricht bekommen darf
    // ============================================================
    public static boolean canSend(Player player, String key) {
        UUID uuid = player.getUniqueId();
        playerCooldowns.putIfAbsent(uuid, new HashMap<>());
        Map<String, Long> map = playerCooldowns.get(uuid);

        Long last = map.get(key);
        if (last == null || (System.currentTimeMillis() - last) >= playerCooldown) {
            map.put(key, System.currentTimeMillis());
            return true; // darf senden
        }
        return false; // auf Cooldown
    }

    // ============================================================
    // ⚙️ Interne Cooldown-Helfer
    // ============================================================
    private static boolean isOnCooldown(String key, long cd) {
        Long last = cooldowns.get(key);
        return last != null && (System.currentTimeMillis() - last) < cd;
    }

    private static void setCooldown(String key) {
        cooldowns.put(key, System.currentTimeMillis());
    }
}

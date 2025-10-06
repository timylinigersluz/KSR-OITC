package ch.ksrminecraft.kSROITC.utils;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class Dbg {
    private static final String PREFIX = "[KSR-OITC]";
    private static Supplier<Boolean> enabled = () -> false;
    private static Logger log = Logger.getLogger("KSROITC");
    private static ConsoleCommandSender console;

    private Dbg() {}

    /** Im onEnable() aufrufen: bind(plugin, () -> configManager.isDebug()) */
    public static void bind(JavaPlugin plugin, Supplier<Boolean> enabledSupplier) {
        log = plugin.getLogger();
        console = plugin.getServer().getConsoleSender();
        enabled = Objects.requireNonNullElse(enabledSupplier, () -> false);
    }

    public static boolean isEnabled() {
        try { return enabled.get(); } catch (Exception e) { return false; }
    }

    /** Debug-Ausgabe exakt im Format: [KSR-OITC] [DEBUG] [KLASSE] Message */
    public static void d(Class<?> cls, String msg) { d(cls.getSimpleName(), msg); }

    public static void d(String tag, String msg) {
        if (!isEnabled() || console == null) return;
        console.sendMessage(PREFIX + " [DEBUG] [" + tag + "] " + msg);
    }

    /** Optionale Standard-Logs (verwenden das Plugin-Logger-Format) */
    public static void warn(String msg) { log.warning(PREFIX + " [WARN] " + msg); }
    public static void err(String msg)  { log.severe(PREFIX + " [ERROR] " + msg); }
}

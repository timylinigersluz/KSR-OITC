package ch.ksrminecraft.kSROITC.managers.system;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration cfg;

    // --- neue Felder ---
    private boolean protectMainLobby;
    private String mainWorld;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        cfg = plugin.getConfig();

        // --- neue Werte laden ---
        this.protectMainLobby = cfg.getBoolean("protect-main_lobby", true);
        this.mainWorld = cfg.getString("main_world", "world"); // Fallback auf "world"
    }

    // === Getter ===

    /** Gibt an, ob die Hauptlobby-Welt vor Änderungen geschützt ist */
    public boolean isProtectMainLobby() {
        return protectMainLobby;
    }

    /** Liefert den Namen der Hauptlobby-Welt */
    public String getMainWorld() {
        return mainWorld;
    }

    public boolean isDebug() {
        return cfg.getBoolean("debug", false);
    }

    public ConfigurationSection defaults() {
        return cfg.getConfigurationSection("default");
    }

    public ConfigurationSection arenas() {
        return cfg.getConfigurationSection("arenas");
    }

    public ConfigurationSection messages() {
        return cfg.getConfigurationSection("messages");
    }

    public FileConfiguration raw() {
        return cfg;
    }

    public int getCountdownSeconds() {
        var def = defaults();
        return def != null ? def.getInt("countdown_seconds", 15) : 15;
    }
}

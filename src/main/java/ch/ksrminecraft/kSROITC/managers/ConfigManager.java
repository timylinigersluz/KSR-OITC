package ch.ksrminecraft.kSROITC.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration cfg;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        cfg = plugin.getConfig();
    }

    public boolean isDebug() { return cfg.getBoolean("debug", false); }

    public ConfigurationSection defaults() { return cfg.getConfigurationSection("default"); }
    public ConfigurationSection arenas() { return cfg.getConfigurationSection("arenas"); }
    public ConfigurationSection messages() { return cfg.getConfigurationSection("messages"); }

    public FileConfiguration raw() { return cfg; }

    public int getCountdownSeconds() {
        var def = defaults();
        return def != null ? def.getInt("countdown_seconds", 15) : 15;
    }
}

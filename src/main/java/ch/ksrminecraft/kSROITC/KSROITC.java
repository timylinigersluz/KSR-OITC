package ch.ksrminecraft.kSROITC;

import ch.ksrminecraft.kSROITC.commands.OitcCommand;
import ch.ksrminecraft.kSROITC.commands.OitcTabCompleter;
import ch.ksrminecraft.kSROITC.integration.RankPointsHook;
import ch.ksrminecraft.kSROITC.listeners.PlayerSafetyListener;
import ch.ksrminecraft.kSROITC.listeners.AdvancementBlockListener;
import ch.ksrminecraft.kSROITC.managers.arena.ArenaManager;
import ch.ksrminecraft.kSROITC.managers.arena.SignManager;
import ch.ksrminecraft.kSROITC.managers.arena.TeleportManager;
import ch.ksrminecraft.kSROITC.managers.game.GameManager;
import ch.ksrminecraft.kSROITC.managers.system.ConfigManager;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.MessageLimiter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hauptklasse des KSR-OITC Plugins.
 * Initialisiert alle Manager, lädt Konfigurationen und registriert Listener.
 */
public final class KSROITC extends JavaPlugin {

    private static KSROITC instance;

    private ConfigManager configManager;
    private ArenaManager arenaManager;
    private TeleportManager teleportManager;
    private GameManager gameManager;
    private RankPointsHook rankPointsHook;
    private SignManager signManager;

    public static KSROITC get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // === Grundkomponenten ===
        configManager = new ConfigManager(this);
        Dbg.bind(this, configManager::isDebug);
        MessageLimiter.init(this);

        // === Manager initialisieren ===
        arenaManager    = new ArenaManager(this, configManager);
        teleportManager = new TeleportManager();
        rankPointsHook  = new RankPointsHook(this);
        gameManager     = new GameManager(this, arenaManager, teleportManager, rankPointsHook);
        signManager     = new SignManager(this);

        // === Befehle registrieren ===
        var cmd = getCommand("oitc");
        if (cmd != null) {
            cmd.setExecutor(new OitcCommand());
            cmd.setTabCompleter(new OitcTabCompleter(this));
        }

        // === Listener registrieren ===
        var pm = getServer().getPluginManager();
        pm.registerEvents(new ch.ksrminecraft.kSROITC.listeners.OitcCombatListener(this), this);
        pm.registerEvents(new ch.ksrminecraft.kSROITC.listeners.BowShootListener(this), this);
        pm.registerEvents(new ch.ksrminecraft.kSROITC.listeners.MiscProtectionListener(this), this);
        pm.registerEvents(new ch.ksrminecraft.kSROITC.listeners.SignListener(this), this);
        pm.registerEvents(new ch.ksrminecraft.kSROITC.listeners.GameModeChangeListener(), this);
        pm.registerEvents(new ch.ksrminecraft.kSROITC.listeners.CommandBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerSafetyListener(this), this);
        getServer().getPluginManager().registerEvents(new AdvancementBlockListener(this), this);

        // === Daten laden ===
        arenaManager.loadFromStorage();
        signManager.loadFromStorage();
        gameManager.loadFromStorage(arenaManager);

        Dbg.d(KSROITC.class, "Startup abgeschlossen – alle Manager initialisiert.");
    }

    @Override
    public void onDisable() {
        Dbg.d(KSROITC.class, "onDisable: beginne persistentes Speichern ...");
        try {
            if (arenaManager != null) arenaManager.saveToStorage();
            if (gameManager != null) gameManager.saveToStorage();
            if (signManager != null) signManager.saveToStorage();
        } catch (Exception e) {
            getLogger().warning("[KSROITC] Fehler beim Speichern: " + e.getMessage());
        }
    }

    // ============================================================
    // GETTER
    // ============================================================
    public ConfigManager getConfigManager() { return configManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public TeleportManager getTeleportManager() { return teleportManager; }
    public GameManager getGameManager() { return gameManager; }
    public RankPointsHook getRankPointsHook() { return rankPointsHook; }
    public SignManager getSignManager() { return signManager; }
}

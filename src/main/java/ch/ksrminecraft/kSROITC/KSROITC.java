package ch.ksrminecraft.kSROITC;

import ch.ksrminecraft.kSROITC.integration.RankPointsHook;
import ch.ksrminecraft.kSROITC.managers.*;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.MessageLimiter;
import org.bukkit.plugin.java.JavaPlugin;

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

        configManager = new ConfigManager(this);
        Dbg.bind(this, () -> configManager.isDebug());
        MessageLimiter.init(this);

        arenaManager   = new ArenaManager(this, configManager);
        teleportManager = new TeleportManager();
        rankPointsHook  = new RankPointsHook(this);
        gameManager     = new GameManager(this, arenaManager, teleportManager, rankPointsHook);
        signManager     = new SignManager(this);

        var cmd = getCommand("oitc");
        if (cmd != null) {
            cmd.setExecutor(new ch.ksrminecraft.kSROITC.commands.OitcCommand());
            cmd.setTabCompleter(new ch.ksrminecraft.kSROITC.commands.OitcTabCompleter(this));
        }

        getServer().getPluginManager().registerEvents(new ch.ksrminecraft.kSROITC.listeners.OitcCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new ch.ksrminecraft.kSROITC.listeners.BowShootListener(this), this);
        getServer().getPluginManager().registerEvents(new ch.ksrminecraft.kSROITC.listeners.MiscProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ch.ksrminecraft.kSROITC.listeners.SignListener(this), this);

        arenaManager.loadFromStorage();
        signManager.loadFromStorage();
        gameManager.loadFromStorage(arenaManager);

        Dbg.d(KSROITC.class, "Startup abgeschlossen.");
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

    // Getter
    public ConfigManager getConfigManager() { return configManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public TeleportManager getTeleportManager() { return teleportManager; }
    public GameManager getGameManager() { return gameManager; }
    public RankPointsHook getRankPointsHook() { return rankPointsHook; }
    public SignManager getSignManager() { return signManager; }
}

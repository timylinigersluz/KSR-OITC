package ch.ksrminecraft.kSROITC.managers.arena;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.system.ConfigManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.models.SimpleLocation;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.logging.Logger;

/**
 * Verwalter aller Arenen.
 * Lädt Arenen AUSSCHLIESSLICH aus config.yml und speichert Änderungen persistent in config.yml.
 *
 * (DataStorage/arenas.json wird nicht mehr verwendet.)
 */
public class ArenaManager {

    private final KSROITC plugin;
    private final ConfigManager cfg;
    private final Logger log;
    private final Map<String, Arena> arenas = new HashMap<>(); // key: lowercase

    public ArenaManager(KSROITC plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.log = plugin.getLogger();
        Dbg.d(ArenaManager.class, "ctor: initialisiere ArenaManager");
        loadFromConfig();
    }

    public void loadFromConfig() {
        Dbg.d(ArenaManager.class, "loadFromConfig: start");
        arenas.clear();

        ConfigurationSection def = cfg.defaults();
        int dMin   = def != null ? def.getInt("min_players", 2) : 2;
        int dMax   = def != null ? def.getInt("max_players", 12) : 12;
        int dKills = def != null ? def.getInt("max_kills", 20) : 20;

        // ✅ Source of Truth: config.yml (default/max_seconds)
        //    WICHTIG: Fallback soll konsistent zu deiner config sein:
        //    - in config.yml: max_seconds: 300 (5 min)
        //    - falls key fehlt: 300 statt 600
        int dSecs  = def != null ? def.getInt("max_seconds", 300) : 300;

        boolean dJoin  = def != null && def.getBoolean("allow_join_in_progress", true);
        boolean dSword = def != null && def.getBoolean("give_sword", true);

        ConfigurationSection sec = cfg.arenas();
        if (sec == null) {
            Dbg.d(ArenaManager.class, "loadFromConfig: keine Arenen in config.yml");
            return;
        }

        for (String name : sec.getKeys(false)) {
            ConfigurationSection a = sec.getConfigurationSection(name);
            if (a == null) continue;

            String worldName = a.getString("world");
            if (worldName == null) {
                log.warning("[OITC] Arena '" + name + "': world fehlt.");
                continue;
            }

            Arena arena = new Arena(name, worldName, dMin, dMax, dKills, dSecs, dJoin, dSword);

            // lobby
            if (a.isConfigurationSection("lobby")) {
                arena.setLobby(readSimple(a.getConfigurationSection("lobby")));
            }

            // spawns
            if (a.isList("spawns")) {
                List<?> list = a.getList("spawns");
                for (Object o : list) {
                    if (o instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) o;
                        arena.getSpawns().add(readSimple(m));
                    }
                }
            }

            // overrides
            if (a.isConfigurationSection("overrides")) {
                ConfigurationSection ov = a.getConfigurationSection("overrides");
                arena.setOverrides(
                        ov != null && ov.isInt("min_players") ? ov.getInt("min_players") : null,
                        ov != null && ov.isInt("max_players") ? ov.getInt("max_players") : null,
                        ov != null && ov.isInt("max_kills") ? ov.getInt("max_kills") : null,
                        ov != null && ov.isInt("max_seconds") ? ov.getInt("max_seconds") : null,
                        ov != null && ov.isBoolean("allow_join_in_progress") ? ov.getBoolean("allow_join_in_progress") : null,
                        ov != null && ov.isBoolean("give_sword") ? ov.getBoolean("give_sword") : null
                );
            }

            arenas.put(name.toLowerCase(Locale.ROOT), arena);
        }

        Dbg.d(ArenaManager.class, "loadFromConfig: done – arenas=" + arenas.size());
    }

    public Collection<Arena> all() {
        return arenas.values();
    }

    public Arena get(String name) {
        if (name == null) return null;
        return arenas.get(name.toLowerCase(Locale.ROOT));
    }

    public Arena ensure(String name, String worldName) {
        Arena existing = get(name);
        if (existing != null) return existing;

        ConfigurationSection def = cfg.defaults();
        int dMin   = def != null ? def.getInt("min_players", 2) : 2;
        int dMax   = def != null ? def.getInt("max_players", 12) : 12;
        int dKills = def != null ? def.getInt("max_kills", 20) : 20;
        int dSecs  = def != null ? def.getInt("max_seconds", 300) : 300;
        boolean dJoin  = def != null && def.getBoolean("allow_join_in_progress", true);
        boolean dSword = def != null && def.getBoolean("give_sword", true);

        Arena created = new Arena(name, worldName, dMin, dMax, dKills, dSecs, dJoin, dSword);
        arenas.put(name.toLowerCase(Locale.ROOT), created);
        return created;
    }

    /**
     * Persistiert Arena-Daten in config.yml (Source of Truth).
     * Hinweis: Overrides werden hier bewusst nicht automatisch geschrieben,
     * weil deine bisherigen Commands primär Lobby/Spawns setzen.
     * (Wenn du Overrides persistent brauchst, können wir das gezielt ergänzen.)
     */
    public void persistArena(Arena arena) {
        String base = "arenas." + arena.getName();
        plugin.getConfig().set(base + ".world", arena.getWorldName());

        if (arena.getLobby() != null) {
            // existierende Section überschreiben, damit keine alten Keys hängen bleiben
            plugin.getConfig().set(base + ".lobby", null);
            plugin.getConfig().createSection(base + ".lobby", toMap(arena.getLobby()));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (SimpleLocation sl : arena.getSpawns()) list.add(toMap(sl));
        plugin.getConfig().set(base + ".spawns", list);

        plugin.saveConfig();
    }

    // ---------- DataStorage Integration ----------
    // ❌ Wird nicht mehr verwendet (Arenen sollen nur aus config.yml kommen)

    public void saveToStorage() {
        // absichtlich leer
        Dbg.d(ArenaManager.class, "saveToStorage: deaktiviert (Arenen werden nur in config.yml verwaltet).");
    }

    public void loadFromStorage() {
        // absichtlich leer – wir laden immer aus config.yml
        Dbg.d(ArenaManager.class, "loadFromStorage: deaktiviert -> loadFromConfig()");
        loadFromConfig();
    }

    // ---------- Helpers ----------

    private SimpleLocation readSimple(ConfigurationSection s) {
        return new SimpleLocation(
                s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw", 0.0), (float) s.getDouble("pitch", 0.0)
        );
    }

    private SimpleLocation readSimple(Map<String, Object> m) {
        double x = asD(m.get("x")), y = asD(m.get("y")), z = asD(m.get("z"));
        float yaw = (float) asD(m.getOrDefault("yaw", 0.0));
        float pitch = (float) asD(m.getOrDefault("pitch", 0.0));
        return new SimpleLocation(x, y, z, yaw, pitch);
    }

    private Map<String, Object> toMap(SimpleLocation sl) {
        Map<String, Object> m = new HashMap<>();
        m.put("x", sl.getX());
        m.put("y", sl.getY());
        m.put("z", sl.getZ());
        m.put("yaw", sl.getYaw());
        m.put("pitch", sl.getPitch());
        return m;
    }

    private double asD(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        try {
            String s = String.valueOf(o);
            if (s.equalsIgnoreCase("null") || s.isBlank()) return 0.0;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public boolean isArenaWorld(String worldName) {
        if (worldName == null) return false;
        return arenas.values().stream()
                .anyMatch(a -> a.getWorldName().equalsIgnoreCase(worldName));
    }
}

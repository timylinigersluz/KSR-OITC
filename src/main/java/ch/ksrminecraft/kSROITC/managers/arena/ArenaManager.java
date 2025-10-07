package ch.ksrminecraft.kSROITC.managers.arena;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.system.ConfigManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.models.SimpleLocation;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.DataStorage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.logging.Logger;

/**
 * Verwalter aller Arenen.
 * Lädt Daten aus config.yml oder arenas.json und speichert Änderungen persistent.
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
        int dSecs  = def != null ? def.getInt("max_seconds", 600) : 600;
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
            if (a.isConfigurationSection("lobby")) arena.setLobby(readSimple(a.getConfigurationSection("lobby")));

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
                        ov.isInt("min_players") ? ov.getInt("min_players") : null,
                        ov.isInt("max_players") ? ov.getInt("max_players") : null,
                        ov.isInt("max_kills") ? ov.getInt("max_kills") : null,
                        ov.isInt("max_seconds") ? ov.getInt("max_seconds") : null,
                        ov.isBoolean("allow_join_in_progress") ? ov.getBoolean("allow_join_in_progress") : null,
                        ov.isBoolean("give_sword") ? ov.getBoolean("give_sword") : null
                );
            }

            arenas.put(name.toLowerCase(Locale.ROOT), arena);
        }

        Dbg.d(ArenaManager.class, "loadFromConfig: done – arenas=" + arenas.size());
    }

    public Collection<Arena> all() { return arenas.values(); }

    public Arena get(String name) {
        return arenas.get(name.toLowerCase(Locale.ROOT));
    }

    public Arena ensure(String name, String worldName) {
        Arena existing = get(name);
        if (existing != null) return existing;

        ConfigurationSection def = cfg.defaults();
        int dMin   = def != null ? def.getInt("min_players", 2) : 2;
        int dMax   = def != null ? def.getInt("max_players", 12) : 12;
        int dKills = def != null ? def.getInt("max_kills", 20) : 20;
        int dSecs  = def != null ? def.getInt("max_seconds", 600) : 600;
        boolean dJoin  = def != null && def.getBoolean("allow_join_in_progress", true);
        boolean dSword = def != null && def.getBoolean("give_sword", true);

        Arena created = new Arena(name, worldName, dMin, dMax, dKills, dSecs, dJoin, dSword);
        arenas.put(name.toLowerCase(Locale.ROOT), created);
        return created;
    }

    public void persistArena(Arena arena) {
        String base = "arenas." + arena.getName();
        plugin.getConfig().set(base + ".world", arena.getWorldName());
        if (arena.getLobby() != null) plugin.getConfig().createSection(base + ".lobby", toMap(arena.getLobby()));

        List<Map<String, Object>> list = new ArrayList<>();
        for (SimpleLocation sl : arena.getSpawns()) list.add(toMap(sl));
        plugin.getConfig().set(base + ".spawns", list);

        plugin.saveConfig();
    }

    // ---------- DataStorage Integration ----------

    public void saveToStorage() {
        Map<String, Map<String, Object>> data = new HashMap<>();

        for (Arena a : arenas.values()) {
            Map<String, Object> m = new HashMap<>();
            m.put("world", a.getWorldName());

            if (a.getLobby() != null) {
                m.put("lobby", toMap(a.getLobby()));
            }

            List<Map<String, Object>> spawns = new ArrayList<>();
            for (SimpleLocation sl : a.getSpawns()) {
                spawns.add(toMap(sl));
            }
            m.put("spawns", spawns);

            m.put("min_players", a.getMinPlayers());
            m.put("max_players", a.getMaxPlayers());
            m.put("max_kills", a.getMaxKills());
            m.put("max_seconds", a.getMaxSeconds());
            m.put("allow_join_in_progress", a.isAllowJoinInProgress());
            m.put("give_sword", a.isGiveSword());

            data.put(a.getName().toLowerCase(Locale.ROOT), m);
        }

        DataStorage.saveArenas(data);
        Dbg.d(ArenaManager.class, "saveToStorage: " + arenas.size() + " Arenen gespeichert.");
    }

    @SuppressWarnings("unchecked")
    public void loadFromStorage() {
        Map<String, Map<String, Object>> data = DataStorage.loadArenas();
        if (data == null || data.isEmpty()) {
            Dbg.d(ArenaManager.class, "loadFromStorage: keine gespeicherten Arenen gefunden.");
            return;
        }

        arenas.clear();

        for (Map.Entry<String, Map<String, Object>> e : data.entrySet()) {
            try {
                Map<String, Object> m = e.getValue();
                String world = Objects.toString(m.get("world"), null);
                if (world == null) {
                    Dbg.d(ArenaManager.class, "loadFromStorage: Arena '" + e.getKey() + "' ohne Welt – übersprungen.");
                    continue;
                }

                Arena a = new Arena(
                        e.getKey(),
                        world,
                        ((Number) m.getOrDefault("min_players", 2)).intValue(),
                        ((Number) m.getOrDefault("max_players", 12)).intValue(),
                        ((Number) m.getOrDefault("max_kills", 20)).intValue(),
                        ((Number) m.getOrDefault("max_seconds", 600)).intValue(),
                        (boolean) m.getOrDefault("allow_join_in_progress", true),
                        (boolean) m.getOrDefault("give_sword", true)
                );

                // Lobby
                if (m.containsKey("lobby")) {
                    Object lobbyObj = m.get("lobby");
                    if (lobbyObj instanceof Map<?, ?> map) {
                        a.setLobby(readSimple((Map<String, Object>) map));
                    }
                }

                // Spawns
                if (m.containsKey("spawns")) {
                    Object spawnsObj = m.get("spawns");
                    if (spawnsObj instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof Map<?, ?> map) {
                                a.getSpawns().add(readSimple((Map<String, Object>) map));
                            }
                        }
                    }
                }

                arenas.put(e.getKey().toLowerCase(Locale.ROOT), a);
            } catch (Exception ex) {
                Bukkit.getLogger().warning("[KSROITC] Fehler beim Laden einer Arena '" + e.getKey() + "': " + ex.getMessage());
            }
        }

        Dbg.d(ArenaManager.class, "loadFromStorage: " + arenas.size() + " Arenen wiederhergestellt.");
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

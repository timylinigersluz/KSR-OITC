package ch.ksrminecraft.kSROITC.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class DataStorage {

    private static final File DIR = new File("plugins/KSR-OITC/data");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        if (!DIR.exists() && !DIR.mkdirs()) {
            Bukkit.getLogger().warning("[KSROITC] Konnte Datenordner nicht erstellen: " + DIR.getAbsolutePath());
        }
    }

    private static void save(String name, Object data) {
        File file = new File(DIR, name + ".json");
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[KSROITC] Fehler beim Speichern von " + name + ": " + e.getMessage());
        }
    }

    private static <T> T load(String name, Type type, T def) {
        File file = new File(DIR, name + ".json");
        if (!file.exists()) return def;
        try (Reader reader = new FileReader(file)) {
            return GSON.fromJson(reader, type);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[KSROITC] Fehler beim Laden von " + name + ": " + e.getMessage());
            return def;
        }
    }

    // Arenen
    public static void saveArenas(Map<String, Map<String, Object>> data) {
        save("arenas", data);
    }
    public static Map<String, Map<String, Object>> loadArenas() {
        Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
        return load("arenas", type, new HashMap<>());
    }

    // Sessions
    public static void saveSessions(Map<String, Map<String, Object>> data) {
        save("sessions", data);
    }
    public static Map<String, Map<String, Object>> loadSessions() {
        Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
        return load("sessions", type, new HashMap<>());
    }

    // Signs
    public static void saveSigns(Map<String, String> data) {
        save("signs", data);
    }
    public static Map<String, String> loadSigns() {
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        return load("signs", type, new HashMap<>());
    }

    public static File getDataDir() { return DIR; }
}

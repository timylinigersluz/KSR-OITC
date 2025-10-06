package ch.ksrminecraft.kSROITC.managers;

import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import ch.ksrminecraft.kSROITC.utils.DataStorage;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.entity.Player;

import java.util.*;

public class SessionManager {

    private final Map<String, GameSession> sessions = new HashMap<>();
    private final Map<UUID, String> playerArena = new HashMap<>();

    public GameSession ensure(Arena a) {
        String key = a.getName().toLowerCase(Locale.ROOT);
        return sessions.computeIfAbsent(key, k -> new GameSession(a));
    }

    public Optional<GameSession> byArena(String arenaName) {
        return Optional.ofNullable(sessions.get(arenaName.toLowerCase(Locale.ROOT)));
    }

    public Optional<GameSession> byPlayer(Player p) {
        String a = playerArena.get(p.getUniqueId());
        return (a == null) ? Optional.empty() : byArena(a);
    }

    public void addPlayer(Player p, GameSession s) {
        s.addPlayer(p.getUniqueId());
        playerArena.put(p.getUniqueId(), s.getArena().getName().toLowerCase(Locale.ROOT));
    }

    public void removePlayer(Player p, GameSession s) {
        s.removePlayer(p.getUniqueId());
        playerArena.remove(p.getUniqueId());
    }

    public Collection<GameSession> allSessions() { return sessions.values(); }

    // ----------------- Persistenz -----------------

    public void saveToStorage() {
        Map<String, Map<String, Object>> data = new HashMap<>();

        for (GameSession s : sessions.values()) {
            Map<String, Object> m = new HashMap<>();
            m.put("arena", s.getArena().getName());
            m.put("state", s.getState().name());
            m.put("players", s.getPlayers().stream().map(UUID::toString).toList());
            m.put("start_ts", s.getStartTimestamp());
            m.put("end_ts", s.getEndTimestamp());
            data.put(s.getArena().getName().toLowerCase(Locale.ROOT), m);
        }

        DataStorage.saveSessions(data);
        Dbg.d(SessionManager.class, "saveToStorage: " + sessions.size() + " Sessions gespeichert.");
    }

    @SuppressWarnings("unchecked")
    public void loadFromStorage(ArenaManager arenaManager) {
        Map<String, Map<String, Object>> data = DataStorage.loadSessions();
        if (data.isEmpty()) return;

        long cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L;
        int removed = 0;
        Iterator<Map.Entry<String, Map<String, Object>>> it = data.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Map<String, Object>> e = it.next();
            Map<String, Object> m = e.getValue();

            long endTs = ((Number) m.getOrDefault("end_ts", 0L)).longValue();
            if (endTs > 0 && endTs < cutoff) { it.remove(); removed++; continue; }

            String arenaName = (String) m.get("arena");
            Arena a = arenaManager.get(arenaName);
            if (a == null) continue;

            GameSession s = new GameSession(a);
            s.setState(GameState.valueOf((String) m.getOrDefault("state", "IDLE")));
            s.setStartTimestamp(((Number) m.getOrDefault("start_ts", System.currentTimeMillis())).longValue());
            s.setEndTimestamp(endTs);

            List<String> list = (List<String>) m.getOrDefault("players", new ArrayList<>());
            for (String id : list) {
                UUID u = UUID.fromString(id);
                s.addPlayer(u);
                playerArena.put(u, a.getName().toLowerCase(Locale.ROOT));
            }

            sessions.put(arenaName.toLowerCase(Locale.ROOT), s);
        }

        if (removed > 0) DataStorage.saveSessions(data);
        Dbg.d(SessionManager.class, "loadFromStorage: " + sessions.size() + " Sessions aktiv, " + removed + " gelöscht.");
    }
    public void clearMappings(GameSession s) {
        for (UUID u : new HashSet<>(s.getPlayers())) {
            playerArena.remove(u);
        }
    }
    public boolean sameRunningSession(Player a, Player b) {
        Optional<GameSession> sa = byPlayer(a);
        Optional<GameSession> sb = byPlayer(b);
        return sa.isPresent() && sb.isPresent()
                && sa.get() == sb.get()
                && sa.get().getState() == GameState.RUNNING;
    }


}

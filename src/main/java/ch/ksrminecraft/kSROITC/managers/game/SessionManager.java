package ch.ksrminecraft.kSROITC.managers.game;

import ch.ksrminecraft.kSROITC.managers.arena.ArenaManager;
import ch.ksrminecraft.kSROITC.models.*;
import ch.ksrminecraft.kSROITC.utils.DataStorage;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Verwalter aller GameSessions (aktive Spiele) und Spielerzuordnungen.
 * Speichert: Arena → Session, PlayerUUID → Arena.
 * Delegiert Zuschauer-Handling an SpectatorManager.
 */
public class SessionManager {

    private final Map<String, GameSession> sessions = new HashMap<>();
    private final Map<UUID, String> playerArena = new HashMap<>();
    private final SpectatorManager spectatorManager;

    public SessionManager(SpectatorManager spectatorManager) {
        this.spectatorManager = spectatorManager;
    }

    // ============================================================
    // BASISZUGRIFFE
    // ============================================================

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

    public Collection<GameSession> allSessions() {
        return sessions.values();
    }

    // ============================================================
    // SPIELER-HINZUFÜGUNG / ENTFERNUNG
    // ============================================================

    /**
     * Registriert einen Spieler in einer Session.
     * Zuschauer werden NICHT in der Playerliste der Session geführt.
     */
    public void addPlayer(Player p, GameSession s, boolean spectator) {
        UUID id = p.getUniqueId();
        playerArena.put(id, s.getArena().getName().toLowerCase(Locale.ROOT));

        if (spectator) {
            spectatorManager.setSpectator(p, true);
            Dbg.d(SessionManager.class, "addPlayer: " + p.getName() + " als Zuschauer in " + s.getArena().getName());
        } else {
            s.addPlayer(id);
            spectatorManager.setSpectator(p, false);
            Dbg.d(SessionManager.class, "addPlayer: " + p.getName() + " als Spieler in " + s.getArena().getName());
        }
    }

    /**
     * Entfernt einen Spieler komplett aus der Session.
     * Zuschauer werden dabei ebenfalls entfernt.
     */
    public void removePlayer(Player p, GameSession s) {
        s.removePlayer(p.getUniqueId());
        playerArena.remove(p.getUniqueId());
        spectatorManager.setSpectator(p, false);
        Dbg.d(SessionManager.class, "removePlayer: " + p.getName() + " aus " + s.getArena().getName());
    }

    /**
     * Entfernt alle Zuordnungen zu einer Arena (z. B. nach Match-Ende).
     */
    public void clearMappings(GameSession s) {
        for (UUID u : new HashSet<>(s.getPlayers())) {
            playerArena.remove(u);
        }
        spectatorManager.clearAll();
        Dbg.d(SessionManager.class, "clearMappings: Session " + s.getArena().getName() + " geleert.");
    }

    // ============================================================
    // STATUS-CHECKS
    // ============================================================

    /** Prüft, ob ein Spieler aktuell Zuschauer ist. */
    public boolean isSpectator(Player p) {
        return spectatorManager.isSpectator(p);
    }

    /** Gibt nur aktive Spieler (ohne Zuschauer) zurück. */
    public List<Player> getActivePlayers(GameSession s) {
        List<Player> list = new ArrayList<>();
        for (UUID id : s.getPlayers()) {
            Player p = org.bukkit.Bukkit.getPlayer(id);
            if (p != null && !spectatorManager.isSpectator(p)) list.add(p);
        }
        return list;
    }

    /** Gibt die Anzahl der aktiven Spieler (ohne Zuschauer) zurück. */
    public int getActiveCount(GameSession s) {
        int count = 0;
        for (UUID id : s.getPlayers()) {
            Player p = org.bukkit.Bukkit.getPlayer(id);
            if (p != null && !spectatorManager.isSpectator(p)) count++;
        }
        return count;
    }

    /**
     * Prüft, ob zwei Spieler in derselben laufenden Session sind UND keine Zuschauer sind.
     */
    public boolean sameRunningSession(Player a, Player b) {
        Optional<GameSession> sa = byPlayer(a);
        Optional<GameSession> sb = byPlayer(b);
        return sa.isPresent() && sb.isPresent()
                && sa.get() == sb.get()
                && sa.get().getState() == GameState.RUNNING
                && !spectatorManager.isSpectator(a)
                && !spectatorManager.isSpectator(b);
    }

    // ============================================================
    // PERSISTENZ
    // ============================================================

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
            if (endTs > 0 && endTs < cutoff) {
                it.remove();
                removed++;
                continue;
            }

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
}

package ch.ksrminecraft.kSROITC.models;

import java.util.*;

public class GameSession {
    private final Arena arena;
    private GameState state = GameState.IDLE;

    private final Set<UUID> players = new HashSet<>();
    private final Map<UUID, Integer> kills = new HashMap<>();

    // -1 = ohne Timer
    private long endTimestamp = -1L;
    private long startTimestamp = System.currentTimeMillis();

    // Countdown-Ende für Pre-Game (optional; -1 = keiner)
    private long countdownEndTimestamp = -1L;

    // Bukkit Scheduler Task-ID für den Session-Timer (-1 = keiner)
    private int taskId = -1;

    public GameSession(Arena arena) { this.arena = arena; }

    public Arena getArena() { return arena; }
    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }

    public Set<UUID> getPlayers() { return players; }
    public Map<UUID, Integer> getKills() { return kills; }

    public void addPlayer(UUID uuid) { players.add(uuid); kills.putIfAbsent(uuid, 0); }
    public void removePlayer(UUID uuid) { players.remove(uuid); kills.remove(uuid); }

    public int incrementKills(UUID uuid) { return kills.merge(uuid, 1, Integer::sum); }

    public long getEndTimestamp() { return endTimestamp; }
    public void setEndTimestamp(long endTimestamp) { this.endTimestamp = endTimestamp; }

    public long getCountdownEndTimestamp() { return countdownEndTimestamp; }
    public void setCountdownEndTimestamp(long countdownEndTimestamp) { this.countdownEndTimestamp = countdownEndTimestamp; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public long getStartTimestamp() { return startTimestamp; }
    public void setStartTimestamp(long startTimestamp) { this.startTimestamp = startTimestamp; }

}

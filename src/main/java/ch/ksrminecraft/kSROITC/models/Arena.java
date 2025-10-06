package ch.ksrminecraft.kSROITC.models;

import java.util.ArrayList;
import java.util.List;

public class Arena {
    private final String name;
    private final String worldName;

    private SimpleLocation lobby;
    private final List<SimpleLocation> spawns = new ArrayList<>();

    // Settings (mit Defaults, durch overrides änderbar)
    private int minPlayers, maxPlayers, maxKills, maxSeconds;
    private boolean allowJoinInProgress, giveSword;

    public Arena(String name, String worldName,
                 int minPlayers, int maxPlayers, int maxKills, int maxSeconds,
                 boolean allowJoinInProgress, boolean giveSword) {
        this.name = name;
        this.worldName = worldName;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.maxKills = maxKills;
        this.maxSeconds = maxSeconds;
        this.allowJoinInProgress = allowJoinInProgress;
        this.giveSword = giveSword;
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }

    public SimpleLocation getLobby() { return lobby; }
    public void setLobby(SimpleLocation lobby) { this.lobby = lobby; }

    public List<SimpleLocation> getSpawns() { return spawns; }

    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getMaxKills() { return maxKills; }
    public int getMaxSeconds() { return maxSeconds; }
    public boolean isAllowJoinInProgress() { return allowJoinInProgress; }
    public boolean isGiveSword() { return giveSword; }

    public void setOverrides(Integer minPlayers, Integer maxPlayers, Integer maxKills, Integer maxSeconds,
                             Boolean allowJoinInProgress, Boolean giveSword) {
        if (minPlayers != null) this.minPlayers = minPlayers;
        if (maxPlayers != null) this.maxPlayers = maxPlayers;
        if (maxKills != null) this.maxKills = maxKills;
        if (maxSeconds != null) this.maxSeconds = maxSeconds;
        if (allowJoinInProgress != null) this.allowJoinInProgress = allowJoinInProgress;
        if (giveSword != null) this.giveSword = giveSword;
    }
}

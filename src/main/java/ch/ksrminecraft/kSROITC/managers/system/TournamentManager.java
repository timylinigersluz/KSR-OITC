package ch.ksrminecraft.kSROITC.managers.system;

import ch.ksrminecraft.kSROITC.KSROITC;

/**
 * Verwaltet den globalen Turniermodus (on/off).
 * Wird bei Pluginstart und bei /oitc reload neu geladen.
 */
public class TournamentManager {

    private final KSROITC plugin;
    private boolean enabled;

    public TournamentManager(KSROITC plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Liest den Turniermodus aus der Config neu ein.
     */
    public void reload() {
        try {
            this.enabled = plugin.getConfig().getBoolean("tournament.enabled", false);
        } catch (Exception e) {
            this.enabled = false;
        }
    }

    /**
     * @return true, wenn Turniermodus aktiv ist
     */
    public boolean isEnabled() {
        return enabled;
    }
}

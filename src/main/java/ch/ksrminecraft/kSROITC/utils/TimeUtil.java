package ch.ksrminecraft.kSROITC.utils;

/**
 * Formatiert Sekunden in mm:ss oder m:ss min.
 */
public class TimeUtil {

    /**
     * Gibt eine kompakte Zeitangabe zurück, z. B. 05:07
     */
    public static String formatCompact(long seconds) {
        if (seconds < 0) return "∞";
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    /**
     * Gibt eine lesbare Zeitangabe zurück, z. B. 5:07 min
     */
    public static String formatReadable(long seconds) {
        if (seconds < 0) return "∞";
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%d:%02d min", min, sec);
    }
}

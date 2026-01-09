package ch.ksrminecraft.kSROITC.managers.system;

import ch.ksrminecraft.kSROITC.KSROITC;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TournamentLogger {

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KSROITC plugin;
    private final File file;

    public TournamentLogger(KSROITC plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "tournament.txt");
        ensureFileExists();
    }

    public synchronized void prependBlock(List<String> blockLines) {
        try {
            List<String> existing = Files.readAllLines(file.toPath());
            blockLines.add("");
            blockLines.addAll(existing);
            Files.write(file.toPath(), blockLines);
        } catch (IOException e) {
            plugin.getLogger().warning(
                    "[OITC] Konnte Turnier-Log nicht schreiben: " + e.getMessage());
        }
    }

    public static String now() {
        return LocalDateTime.now().format(TS_FORMAT);
    }

    private void ensureFileExists() {
        try {
            File dir = plugin.getDataFolder();
            if (!dir.exists()) dir.mkdirs();

            if (!file.exists()) {
                Files.write(file.toPath(), List.of(
                        "### KSR-OITC TOURNAMENT LOG ###",
                        "# Neueste Runde immer OBEN",
                        "# ---",
                        "# [YYYY-MM-DD HH:MM:SS] Arena=<name> Round=<round> Players=<count>",
                        "# <platz> <spieler> (<uuid>) Kills=<n>",
                        ""
                ));
            }
        } catch (IOException e) {
            plugin.getLogger().warning(
                    "[OITC] Konnte tournament.txt nicht erstellen: " + e.getMessage());
        }
    }
}

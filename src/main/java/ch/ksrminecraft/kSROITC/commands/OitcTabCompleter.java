package ch.ksrminecraft.kSROITC.commands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.arena.ArenaManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tab-Completion für den /oitc-Befehl.
 * Spieler ohne Rechte sehen nur Befehle, die sie auch ausführen dürfen.
 */
public class OitcTabCompleter implements TabCompleter {

    private final KSROITC plugin;

    // Subcommands, die ein Arena-Argument erwarten
    private static final Set<String> NEEDS_ARENA = Set.of(
            "join", "start", "reset", "addspawn", "clearspawns", "listspawns"
    );

    public OitcTabCompleter(KSROITC plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("oitc")) return Collections.emptyList();

        // === Hole erlaubte Subcommands aus OitcCommand ===
        List<String> allowedSubs = Collections.emptyList();
        CommandExecutor executor = plugin.getCommand("oitc").getExecutor();
        if (executor instanceof OitcCommand oitcCmd) {
            allowedSubs = oitcCmd.getAvailableSubCommands(sender);
        }

        // --- 1. Argument: Subcommands (gefiltert nach Permission) ---
        if (args.length == 1) {
            return filterPrefix(allowedSubs, args[0]);
        }

        // --- 2. Argument: Arena oder Spezialwert ---
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            // STAFF: /oitc join <player>
            if (sub.equals("join")
                    && sender.hasPermission("oitc.staff")) {

                List<String> players = plugin.getServer().getOnlinePlayers().stream()
                        .map(p -> p.getName())
                        .sorted()
                        .collect(Collectors.toList());

                return filterPrefix(players, args[1]);
            }

            // NORMAL: /oitc <sub> <arena>
            if (NEEDS_ARENA.contains(sub) && allowedSubs.contains(sub)) {
                List<String> arenas = new ArrayList<>();
                if (sub.equals("reset")) arenas.add("all");

                ArenaManager am = plugin.getArenaManager();
                if (am != null) {
                    arenas.addAll(am.all().stream()
                            .map(Arena::getName)
                            .sorted()
                            .collect(Collectors.toList()));
                }

                if (arenas.isEmpty()) arenas = List.of("<arena>");
                return filterPrefix(arenas, args[1]);
            }
        }

        // --- 3. Argument: Arena für STAFF join ---
        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            if (sub.equals("join")
                    && sender.hasPermission("oitc.staff")) {

                List<String> arenas = new ArrayList<>();
                ArenaManager am = plugin.getArenaManager();
                if (am != null) {
                    arenas.addAll(am.all().stream()
                            .map(Arena::getName)
                            .sorted()
                            .collect(Collectors.toList()));
                }

                return filterPrefix(arenas, args[2]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> source, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : source) {
            if (s.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(s);
            }
        }
        return out;
    }
}

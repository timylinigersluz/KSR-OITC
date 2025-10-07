package ch.ksrminecraft.kSROITC.commands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.arena.ArenaManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tab-Completion für den /oitc-Befehl.
 * Unterstützt alle Subcommands und bietet Arena-Namen oder spezielle Argumente an.
 */
public class OitcTabCompleter implements TabCompleter {

    private final KSROITC plugin;

    private static final List<String> SUBS = List.of(
            "join", "leave", "start", "reset",
            "setlobby", "addspawn", "clearspawns", "listspawns",
            "reload"
    );

    // Subcommands, die einen Arena-Namen als 2. Argument erwarten
    private static final Set<String> NEEDS_ARENA = Set.of(
            "join", "start", "reset", "addspawn", "clearspawns", "listspawns"
    );

    public OitcTabCompleter(KSROITC plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("oitc")) return Collections.emptyList();

        // --- 1. Argument: Subcommand-Vorschläge ---
        if (args.length == 1) {
            return filterPrefix(SUBS, args[0]);
        }

        // --- 2. Argument: Arena oder Spezialwert (z.B. all) ---
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (NEEDS_ARENA.contains(sub)) {
                // Bei /oitc reset → auch "all" anbieten
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

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> source, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>(source.size());
        for (String s : source) {
            if (s.toLowerCase(Locale.ROOT).startsWith(p)) out.add(s);
        }
        return out;
    }
}

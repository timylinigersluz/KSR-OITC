package ch.ksrminecraft.kSROITC.commands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.ArenaManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

public class OitcTabCompleter implements TabCompleter {

    private final KSROITC plugin;

    private static final List<String> SUBS = List.of(
            "join", "leave", "start", "stop",
            "setlobby", "addspawn", "clearspawns", "listspawns",
            "reload"
    );

    // Subcommands, die einen Arena-Namen als 2. Argument erwarten
    private static final Set<String> NEEDS_ARENA = Set.of(
            "join", "start", "stop", "addspawn", "clearspawns", "listspawns"
            // setlobby wurde entfernt (kein Arena-Arg mehr)
    );

    public OitcTabCompleter(KSROITC plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("oitc")) return Collections.emptyList();

        if (args.length == 1) {
            return filterPrefix(SUBS, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (NEEDS_ARENA.contains(sub)) {
                ArenaManager am = plugin.getArenaManager();
                List<String> arenas = am == null ? List.of() :
                        am.all().stream().map(Arena::getName).sorted().collect(Collectors.toList());
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

package ch.ksrminecraft.kSROITC.commands;

import ch.ksrminecraft.kSROITC.commands.subcommands.*;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class OitcCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public OitcCommand() {
        register(new JoinSubCommand());
        register(new LeaveSubCommand());
        register(new StartSubCommand());
        register(new StopSubCommand());
        register(new SetLobbySubCommand());
        register(new AddSpawnSubCommand());
        register(new ClearSpawnsSubCommand());
        register(new ListSpawnsSubCommand());
        register(new ReloadSubCommand());
        Dbg.d(OitcCommand.class, "registered subcommands: " + subCommands.keySet());
    }

    private void register(SubCommand sub) {
        subCommands.put(sub.getName().toLowerCase(Locale.ROOT), sub);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Dbg.d(OitcCommand.class, "onCommand by=" + sender.getName() + " label=/" + label + " args=" + Arrays.toString(args));
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        final String key = args[0].toLowerCase(Locale.ROOT);
        final SubCommand sub = subCommands.get(key);

        if (sub == null) {
            Dbg.d(OitcCommand.class, "unknown subcommand: " + key);
            sender.sendMessage("§cUnbekannter Subcommand. §7Nutze §e/" + label + " <" + String.join("|", subCommands.keySet()) + ">");
            return true;
        }

        if (sub.getPermission() != null && !sub.getPermission().isEmpty() && !sender.hasPermission(sub.getPermission())) {
            Dbg.d(OitcCommand.class, "missing permission: required=" + sub.getPermission());
            sender.sendMessage("§cDu hast keine Berechtigung dafür.");
            return true;
        }

        final String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        Dbg.d(OitcCommand.class, "dispatch -> " + sub.getClass().getSimpleName() + " args=" + Arrays.toString(subArgs));
        sub.execute(sender, subArgs);
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§7Verfügbare Befehle: §e" + String.join("§7, §e", subCommands.keySet()));
        sender.sendMessage("§7Beispiel: §e/" + label + " join <arena> §7oder §e/" + label + " setlobby");
        // setlobby ohne <arena>
    }

    // -------- (Optional) interner Tab-Completer (konsistent zu OitcTabCompleter) --------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String cur = args[0].toLowerCase(Locale.ROOT);
            List<String> list = new ArrayList<>(subCommands.keySet());
            list.removeIf(s -> !s.startsWith(cur));
            return list;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (Arrays.asList("join","start","stop","addspawn","clearspawns","listspawns").contains(sub)) {
                return Collections.singletonList("<arena>");
            }
            // setlobby hat kein 2. Argument
        }

        return Collections.emptyList();
    }
}

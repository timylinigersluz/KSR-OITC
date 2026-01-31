package ch.ksrminecraft.kSROITC.commands;

import ch.ksrminecraft.kSROITC.commands.subcommands.*;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

/**
 * Hauptbefehl /oitc
 * Leitet alle Unterbefehle (join, leave, reset, etc.) an die jeweiligen SubCommand-Klassen weiter.
 */
public class OitcCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public OitcCommand() {
        // === Registrierung der Subcommands ===
        register(new JoinSubCommand());
        register(new LeaveSubCommand());
        register(new WatchSubCommand());     // ✅ NEU
        register(new StartSubCommand());
        register(new ResetSubCommand());
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

    // ============================================================
    // COMMAND-AUSFÜHRUNG
    // ============================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Dbg.d(OitcCommand.class, "onCommand by=" + sender.getName() + " label=/" + label + " args=" + Arrays.toString(args));

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        final String key = args[0].toLowerCase(Locale.ROOT);
        final SubCommand sub = subCommands.get(key);

        // --- Unbekannter Subcommand ---
        if (sub == null) {
            Dbg.d(OitcCommand.class, "unknown subcommand: " + key);
            sender.sendMessage("§cUnbekannter Befehl. §7Nutze §e/" + label + " <" + String.join("|", subCommands.keySet()) + ">");
            return true;
        }

        // --- Berechtigungsprüfung (Grundpermission pro Subcommand) ---
        if (sub.getPermission() != null && !sub.getPermission().isEmpty() && !sender.hasPermission(sub.getPermission())) {
            Dbg.d(OitcCommand.class, "missing permission: required=" + sub.getPermission());
            sender.sendMessage("§cDu hast keine Berechtigung dafür.");
            return true;
        }

        // --- SubCommand ausführen ---
        final String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        Dbg.d(OitcCommand.class, "dispatch -> " + sub.getClass().getSimpleName() + " args=" + Arrays.toString(subArgs));

        sub.execute(sender, subArgs);
        return true;
    }

    // ============================================================
    // TAB-COMPLETE & HILFE
    // ============================================================

    private void sendHelp(CommandSender sender, String label) {
        List<String> allowed = getAvailableSubCommands(sender);
        sender.sendMessage("§7Verfügbare Befehle: §e" + String.join("§7, §e", allowed));
        sender.sendMessage("§7Beispiele:");
        sender.sendMessage("§7- §e/" + label + " join <arena>");
        sender.sendMessage("§7- §e/" + label + " watch <arena>");
        sender.sendMessage("§7- §e/" + label + " reset all");
    }

    /**
     * Liefert alle Subcommands, die der Sender sehen darf.
     */
    public List<String> getAvailableSubCommands(CommandSender sender) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, SubCommand> e : subCommands.entrySet()) {
            SubCommand sub = e.getValue();
            String perm = sub.getPermission();
            if (perm == null || perm.isEmpty() || sender.hasPermission(perm)) {
                list.add(e.getKey());
            }
        }
        return list;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Wenn der TabCompleter separat registriert ist, hier nichts tun
        return Collections.emptyList();
    }
}

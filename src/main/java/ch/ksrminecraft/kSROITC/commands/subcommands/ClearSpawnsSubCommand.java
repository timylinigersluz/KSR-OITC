package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.ArenaManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.command.CommandSender;

public class ClearSpawnsSubCommand implements SubCommand {

    @Override public String getName() { return "clearspawns"; }
    @Override public String getPermission() { return "oitc.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Dbg.d(ClearSpawnsSubCommand.class, "execute by=" + sender.getName() + " argsLen=" + args.length);
        if (args.length < 1) { sender.sendMessage("§7/oitc clearspawns <arena>"); return; }
        String arenaName = args[0];

        Dbg.d(ClearSpawnsSubCommand.class, "arena=" + arenaName);
        ArenaManager am = KSROITC.get().getArenaManager();
        Arena a = am.get(arenaName);
        if (a == null) { sender.sendMessage("§cArena nicht gefunden."); Dbg.d(ClearSpawnsSubCommand.class, "arena not found"); return; }

        int removed = a.getSpawns().size();
        a.getSpawns().clear();
        am.persistArena(a);

        Dbg.d(ClearSpawnsSubCommand.class, "cleared spawns: removed=" + removed);
        sender.sendMessage("§a" + removed + " §7Spawns in §e" + arenaName + " §7gelöscht.");
    }
}

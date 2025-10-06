package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.command.CommandSender;

public class StopSubCommand implements SubCommand {
    @Override public String getName() { return "stop"; }
    @Override public String getPermission() { return "oitc.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Dbg.d(StopSubCommand.class, "execute by=" + sender.getName() + " argsLen=" + args.length);
        if (args.length < 1) { sender.sendMessage("§7/oitc stop <arena>"); return; }
        Dbg.d(StopSubCommand.class, "stop arena=" + args[0]);
        KSROITC.get().getGameManager().stop(args[0]);
        sender.sendMessage("§7Stop ausgeführt für §e" + args[0]);
    }
}

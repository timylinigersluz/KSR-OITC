package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.command.CommandSender;

public class StartSubCommand implements SubCommand {
    @Override public String getName() { return "start"; }
    @Override public String getPermission() { return "oitc.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Dbg.d(StartSubCommand.class, "execute by=" + sender.getName() + " argsLen=" + args.length);
        if (args.length < 1) { sender.sendMessage("§7/oitc start <arena>"); return; }
        Dbg.d(StartSubCommand.class, "start arena=" + args[0]);
        KSROITC.get().getGameManager().start(args[0]);
        sender.sendMessage("§7Start versucht für §e" + args[0]);
    }
}

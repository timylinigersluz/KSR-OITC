package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinSubCommand implements SubCommand {
    @Override public String getName() { return "join"; }
    @Override public String getPermission() { return "oitc.use"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Dbg.d(JoinSubCommand.class, "execute by=" + sender.getName() + " argsLen=" + args.length);
        if (!(sender instanceof Player p)) { sender.sendMessage("§cNur als Spieler."); return; }
        if (args.length < 1) { p.sendMessage("§7/oitc join <arena>"); return; }
        Dbg.d(JoinSubCommand.class, "join player=" + p.getName() + " arena=" + args[0]);
        KSROITC.get().getGameManager().join(p, args[0]);
    }
}

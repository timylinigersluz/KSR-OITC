package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveSubCommand implements SubCommand {
    @Override public String getName() { return "leave"; }
    @Override public String getPermission() { return "oitc.use"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Dbg.d(LeaveSubCommand.class, "execute by=" + sender.getName());
        if (!(sender instanceof Player p)) { sender.sendMessage("§cNur als Spieler."); return; }
        Dbg.d(LeaveSubCommand.class, "leave player=" + p.getName());
        KSROITC.get().getGameManager().leave(p);
    }
}

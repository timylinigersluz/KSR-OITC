package ch.ksrminecraft.kSROITC.commands.subcommands;

import org.bukkit.command.CommandSender;

public interface SubCommand {
    String getName();
    String getPermission();
    void execute(CommandSender sender, String[] args);
}

package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.ArenaManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.models.SimpleLocation;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.command.CommandSender;

public class ListSpawnsSubCommand implements SubCommand {

    @Override public String getName() { return "listspawns"; }
    @Override public String getPermission() { return "oitc.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Dbg.d(ListSpawnsSubCommand.class, "execute by=" + sender.getName() + " argsLen=" + args.length);
        if (args.length < 1) { sender.sendMessage("§7/oitc listspawns <arena>"); return; }
        String arenaName = args[0];

        Dbg.d(ListSpawnsSubCommand.class, "arena=" + arenaName);
        ArenaManager am = KSROITC.get().getArenaManager();
        Arena a = am.get(arenaName);
        if (a == null) { sender.sendMessage("§cArena nicht gefunden."); Dbg.d(ListSpawnsSubCommand.class, "arena not found"); return; }

        sender.sendMessage("§7Arena §e" + a.getName() + " §7(Welt: §e" + a.getWorldName() + "§7)");
        if (a.getLobby() != null) {
            SimpleLocation l = a.getLobby();
            sender.sendMessage("§7Lobby: §e" + fmt(l));
        } else sender.sendMessage("§7Lobby: §c(keine)");

        if (a.getSpawns().isEmpty()) {
            sender.sendMessage("§7Spawns: §c(keine)");
        } else {
            int i = 1;
            for (SimpleLocation sl : a.getSpawns()) {
                sender.sendMessage("§7Spawn #" + (i++) + ": §e" + fmt(sl));
            }
        }
        Dbg.d(ListSpawnsSubCommand.class, "listed spawns: count=" + a.getSpawns().size());
    }

    private String fmt(SimpleLocation l) {
        return String.format("x=%.1f y=%.1f z=%.1f yaw=%.1f pitch=%.1f",
                l.getX(), l.getY(), l.getZ(), (double) l.getYaw(), (double) l.getPitch());
    }
}

package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /oitc join <arena>
 * Lässt den Spieler einer bestimmten Arena beitreten oder als Zuschauer beitreten,
 * wenn das Spiel bereits läuft.
 */
public class JoinSubCommand implements SubCommand {

    @Override
    public String getName() { return "join"; }

    @Override
    public String getPermission() { return "oitc.use"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // --- Absender prüfen ---
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler können diesem Befehl verwenden.");
            return;
        }

        Dbg.d(JoinSubCommand.class, "execute by=" + p.getName() + " argsLen=" + args.length);

        // --- Argumente prüfen ---
        if (args.length < 1) {
            p.sendMessage("§7Verwendung: §e/oitc join <arena>");
            return;
        }

        String arenaName = args[0];
        Dbg.d(JoinSubCommand.class, "join player=" + p.getName() + " arena=" + arenaName);

        // --- Join-Versuch ---
        boolean success = KSROITC.get().getGameManager().join(p, arenaName);

        // --- Rückmeldung ---
        if (!success) {
            p.sendMessage("§cBeitritt fehlgeschlagen. Prüfe, ob die Arena existiert oder bereits voll ist.");
            Dbg.d(JoinSubCommand.class, "join failed for " + p.getName() + " -> " + arenaName);
        } else {
            Dbg.d(JoinSubCommand.class, "join success for " + p.getName() + " -> " + arenaName);
        }
    }
}

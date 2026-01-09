package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
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

        // =====================================================
        // STAFF: /oitc join <player> <arena>
        // =====================================================
        if (args.length == 2) {

            if (!sender.hasPermission("oitc.staff")) {
                sender.sendMessage("§cDafür hast du keine Rechte.");
                return;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("§cSpieler §e" + args[0] + " §cist nicht online.");
                return;
            }

            String arenaName = args[1];

            boolean success = KSROITC.get()
                    .getGameManager()
                    .join(target, arenaName);

            if (success) {
                sender.sendMessage("§aSpieler §e" + target.getName()
                        + " §awurde in die Arena §e" + arenaName + " §ateleportiert.");
                target.sendMessage("§e[OITC] §7Du wurdest vom Staff in die Arena §e"
                        + arenaName + " §7verschoben.");
            } else {
                sender.sendMessage("§cJoin fehlgeschlagen. Prüfe Arena oder Spielerstatus.");
            }
            return;
        }

        // =====================================================
        // SPIELER: /oitc join <arena>
        // =====================================================
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl verwenden.");
            return;
        }

        // Turniermodus → blockieren
        if (KSROITC.get().getTournamentManager().isEnabled()
                && !p.hasPermission("oitc.staff")) {

            p.sendMessage("§cOITC läuft im Turniermodus. Beitritt nur durch den Staff.");
            return;
        }

        if (args.length < 1) {
            p.sendMessage("§7Verwendung: §e/oitc join <arena>");
            return;
        }

        String arenaName = args[0];

        boolean success = KSROITC.get()
                .getGameManager()
                .join(p, arenaName);

        if (!success) {
            p.sendMessage("§cBeitritt fehlgeschlagen. Prüfe, ob die Arena existiert oder bereits voll ist.");
        }
    }

}

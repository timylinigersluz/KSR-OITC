package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinSubCommand implements SubCommand {

    @Override public String getName() { return "join"; }
    @Override public String getPermission() { return "oitc.use"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        KSROITC pl = KSROITC.get();
        boolean tournament = pl.getTournamentManager().isEnabled();

        // =====================================================
        // MOD: /oitc join <player> <arena>
        // =====================================================
        if (args.length == 2) {
            if (!sender.hasPermission("oitc.mod")) {
                sender.sendMessage("§cDafür hast du keine Rechte. (§7oitc.mod§c)");
                return;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("§cSpieler §e" + args[0] + " §cist nicht online.");
                return;
            }

            String arenaName = args[1];

            boolean success = pl.getGameManager().join(target, arenaName);
            if (success) {
                sender.sendMessage("§aSpieler §e" + target.getName()
                        + " §awurde in die Arena §e" + arenaName + " §ateleportiert.");
                target.sendMessage("§e[OITC] §7Du wurdest verschoben nach §e" + arenaName + "§7.");
            } else {
                sender.sendMessage("§cJoin fehlgeschlagen. Prüfe Arena oder Spielerstatus.");
            }
            return;
        }

        // =====================================================
        // SELF: /oitc join <arena>
        // =====================================================
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl verwenden.");
            return;
        }

        // Argument check
        if (args.length < 1) {
            p.sendMessage("§7Verwendung: §e/oitc join <arena> §8(oder als Mod: §e/oitc join <player> <arena>§8)");
            return;
        }

        String arenaName = args[0];

        // Turniermodus-Regel:
        // - use: block
        // - mod: allow
        if (tournament && !p.hasPermission("oitc.mod")) {
            p.sendMessage("§cOITC läuft im Turniermodus. §7Beitritt ist aktuell deaktiviert.");
            return;
        }

        boolean success = pl.getGameManager().join(p, arenaName);
        if (!success) {
            p.sendMessage("§cBeitritt fehlgeschlagen. Prüfe, ob die Arena existiert oder bereits voll ist.");
        }
    }
}

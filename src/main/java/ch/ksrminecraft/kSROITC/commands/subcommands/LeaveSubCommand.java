package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class LeaveSubCommand implements SubCommand {

    @Override public String getName() { return "leave"; }
    @Override public String getPermission() { return "oitc.use"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        KSROITC pl = KSROITC.get();
        boolean tournament = pl.getTournamentManager().isEnabled();
        var gm = pl.getGameManager();

        // =====================================================
        // MOD: /oitc leave <player>
        // =====================================================
        if (args.length == 1) {
            if (!sender.hasPermission("oitc.mod")) {
                sender.sendMessage("§cDafür hast du keine Rechte. (§7oitc.mod§c)");
                return;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("§cSpieler §e" + args[0] + " §cist nicht online.");
                return;
            }

            Optional<GameSession> session = gm.getSessionManager().byPlayer(target);
            if (session.isEmpty()) {
                sender.sendMessage("§c" + target.getName() + " ist aktuell in keiner Arena.");
                return;
            }

            gm.leave(target);
            session.ifPresent(s -> gm.getCountdowns().handlePlayerLeave(s));

            sender.sendMessage("§a" + target.getName() + " wurde aus der Arena entfernt.");
            target.sendMessage("§7[OITC] §cDu wurdest aus der Arena entfernt.");
            return;
        }

        // =====================================================
        // SELF: /oitc leave
        // =====================================================
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl nutzen.");
            return;
        }

        Dbg.d(LeaveSubCommand.class, "execute by=" + p.getName());

        // Turniermodus-Regel:
        // - use: block
        // - mod: allow
        if (tournament && !p.hasPermission("oitc.mod")) {
            p.sendMessage("§cOITC läuft im Turniermodus. §7Leave ist aktuell deaktiviert.");
            return;
        }

        Optional<GameSession> session = gm.getSessionManager().byPlayer(p);
        if (session.isEmpty()) {
            p.sendMessage("§cDu bist aktuell in keiner Arena.");
            Dbg.d(LeaveSubCommand.class, "abgebrochen – kein Arena-Eintrag für " + p.getName());
            return;
        }

        gm.leave(p);
        Dbg.d(LeaveSubCommand.class, "Leave erfolgreich für " + p.getName());

        session.ifPresent(s -> gm.getCountdowns().handlePlayerLeave(s));
    }
}

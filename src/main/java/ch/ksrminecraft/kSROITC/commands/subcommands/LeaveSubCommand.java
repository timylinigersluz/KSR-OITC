package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * /oitc leave
 * Lässt den Spieler seine aktuelle Arena verlassen oder das Zuschauen beenden.
 */
public class LeaveSubCommand implements SubCommand {

    @Override
    public String getName() { return "leave"; }

    @Override
    public String getPermission() { return "oitc.use"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl nutzen.");
            return;
        }

        Dbg.d(LeaveSubCommand.class, "execute by=" + p.getName());

        var gm = KSROITC.get().getGameManager();
        Optional<GameSession> session = gm.getSessionManager().byPlayer(p);

        if (session.isEmpty()) {
            p.sendMessage("§cDu bist aktuell in keiner Arena.");
            Dbg.d(LeaveSubCommand.class, "abgebrochen – kein Arena-Eintrag für " + p.getName());
            return;
        }

        // reguläres Leave ausführen
        gm.leave(p);
        Dbg.d(LeaveSubCommand.class, "Leave erfolgreich für " + p.getName());

        // Countdown-Fix: Falls Spieler die Mindestanzahl unterschreitet → Countdown abbrechen
        session.ifPresent(s -> gm.getCountdowns().handlePlayerLeave(s));
    }
}

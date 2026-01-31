package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.models.GameState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WatchSubCommand implements SubCommand {

    @Override public String getName() { return "watch"; }
    @Override public String getPermission() { return "oitc.use"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur Spieler können zuschauen.");
            return;
        }

        if (args.length < 1) {
            p.sendMessage("§7Verwendung: §e/oitc watch <arena>");
            return;
        }

        String arenaName = args[0];
        var gm = KSROITC.get().getGameManager();

        GameSession s = gm.getSessionManager().byArena(arenaName).orElse(null);
        if (s == null) {
            p.sendMessage("§cArena §e" + arenaName + " §cwurde nicht gefunden.");
            return;
        }

        if (s.getState() != GameState.RUNNING) {
            p.sendMessage("§cDiese Arena läuft aktuell nicht. §7Du kannst nur laufende Spiele beobachten.");
            return;
        }

        boolean ok = gm.join(p, arenaName); // Join-Logik soll RUNNING => Spectator
        if (!ok) {
            p.sendMessage("§cZuschauen fehlgeschlagen. Prüfe ob du bereits in einer anderen Arena bist.");
        }
    }
}

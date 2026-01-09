package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.models.GameSession;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import org.bukkit.command.CommandSender;

public class StartSubCommand implements SubCommand {

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getPermission() {
        return "oitc.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Dbg.d(StartSubCommand.class,
                "execute by=" + sender.getName() + " argsLen=" + args.length);

        boolean tournament = KSROITC.get().getTournamentManager().isEnabled();

        // --- Turniermodus: nur Staff ---
        if (tournament && !sender.hasPermission("oitc.staff")) {
            sender.sendMessage("§cIm Turniermodus darf nur der Staff ein Spiel starten.");
            return;
        }

        // --- Argumente prüfen ---
        if (args.length < 1) {
            sender.sendMessage("§7Verwendung: §e/oitc start <arena>" +
                    (tournament ? " <runde>" : ""));
            return;
        }

        String arenaName = args[0];

        // --- Turniermodus: Rundennamen erzwingen ---
        String roundName = null;
        if (tournament) {
            if (args.length < 2) {
                sender.sendMessage("§cIm Turniermodus musst du einen Rundennamen angeben.");
                sender.sendMessage("§7Beispiel: §e/oitc start " + arenaName + " qualiA/semiB/final");
                return;
            }
            roundName = args[1];
        }

        // --- Session laden ---
        GameSession session = KSROITC.get()
                .getGameManager()
                .getSessionManager()
                .byArena(arenaName)
                .orElse(null);

        if (session == null) {
            sender.sendMessage("§cArena §e" + arenaName + " §cwurde nicht gefunden.");
            return;
        }

        // --- Rundennamen setzen (Turniermodus) ---
        if (tournament) {
            session.setTournamentRound(roundName);
            Dbg.d(StartSubCommand.class,
                    "Tournament round set: arena=" + arenaName + " round=" + roundName);
        }

        // --- Spiel starten ---
        KSROITC.get().getGameManager().start(arenaName);

        // --- Feedback ---
        if (tournament) {
            sender.sendMessage("§aTurnierspiel gestartet:");
            sender.sendMessage("§7Arena: §e" + arenaName);
            sender.sendMessage("§7Runde: §e" + roundName);
        } else {
            sender.sendMessage("§aSpiel gestartet für Arena §e" + arenaName);
        }

        Dbg.d(StartSubCommand.class,
                "start issued arena=" + arenaName +
                        (roundName != null ? " round=" + roundName : ""));
    }
}

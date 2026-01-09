package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.MessageLimiter;
import org.bukkit.command.CommandSender;

public class ReloadSubCommand implements SubCommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return "oitc.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        KSROITC pl = KSROITC.get();
        Dbg.d(ReloadSubCommand.class, "execute by=" + sender.getName());

        // 1) Bukkit-Config neu laden
        pl.reloadConfig();

        // 2) Eigene ConfigManager neu laden
        try {
            pl.getConfigManager().reload();
        } catch (Throwable ignored) {}

        // 3) TournamentManager neu laden (⭐ WICHTIG ⭐)
        if (pl.getTournamentManager() != null) {
            pl.getTournamentManager().reload();
            Dbg.d(ReloadSubCommand.class,
                    "TournamentMode=" + pl.getTournamentManager().isEnabled());
        }

        // 4) MessageLimiter & Arena-Config neu initialisieren
        MessageLimiter.init(pl);
        pl.getArenaManager().loadFromConfig();

        // 5) Debug-Flag neu binden
        Dbg.bind(pl, () -> pl.getConfig().getBoolean("debug", false));

        // 6) Feedback an CommandSender
        boolean tournament = pl.getTournamentManager() != null
                && pl.getTournamentManager().isEnabled();

        sender.sendMessage("§a[OITC] §7Config neu geladen. Laufende Runden bleiben aktiv.");
        sender.sendMessage("§7Turniermodus: "
                + (tournament ? "§aAKTIV" : "§cINAKTIV"));

        Dbg.d(ReloadSubCommand.class, "reload done");
    }
}

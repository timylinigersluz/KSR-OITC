package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.MessageLimiter;
import org.bukkit.command.CommandSender;

public class ReloadSubCommand implements SubCommand {

    @Override public String getName() { return "reload"; }
    @Override public String getPermission() { return "oitc.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        KSROITC pl = KSROITC.get();
        Dbg.d(ReloadSubCommand.class, "execute by=" + sender.getName());

        // 1) Konfiguration neu laden
        pl.reloadConfig();
        try {
            pl.getConfigManager().reload(); // falls vorhanden; sonst no-op
        } catch (Throwable ignored) {}

        // Cooldowns neu aus der Config laden
        MessageLimiter.init(pl);
        pl.getArenaManager().loadFromConfig();

        // 2) Debug-Flag neu binden
        Dbg.bind(pl, () -> pl.getConfig().getBoolean("debug", false));

        // 3) Arenen neu einlesen (Sessions bleiben bestehen)
        pl.getArenaManager().loadFromConfig();

        sender.sendMessage("§a[OITC] §7Config neu geladen. Laufende Runden bleiben aktiv.");
        Dbg.d(ReloadSubCommand.class, "reload done");
    }
}

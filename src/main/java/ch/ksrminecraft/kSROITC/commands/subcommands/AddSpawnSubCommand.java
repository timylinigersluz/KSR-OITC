package ch.ksrminecraft.kSROITC.commands.subcommands;

import ch.ksrminecraft.kSROITC.KSROITC;
import ch.ksrminecraft.kSROITC.managers.arena.ArenaManager;
import ch.ksrminecraft.kSROITC.models.Arena;
import ch.ksrminecraft.kSROITC.utils.Dbg;
import ch.ksrminecraft.kSROITC.utils.LocationUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddSpawnSubCommand implements SubCommand {

    @Override public String getName() { return "addspawn"; }
    @Override public String getPermission() { return "oitc.admin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Dbg.d(AddSpawnSubCommand.class, "execute by=" + sender.getName() + " argsLen=" + args.length);
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur im Spiel.");
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§7/oitc addspawn <arena>");
            return;
        }

        String arenaName = args[0];
        ArenaManager am = KSROITC.get().getArenaManager();
        Arena a = am.ensure(arenaName, p.getWorld().getName());

        int before = a.getSpawns().size();
        a.getSpawns().add(LocationUtil.toSimple(p.getLocation()));
        am.persistArena(a);
        am.saveToStorage(); // 💾 Auto-Save nach jeder Änderung

        // 🔁 Schilder sofort aktualisieren
        KSROITC.get().getSignManager().updateAllSigns();

        // ✨ Visuelles Feedback (richtiger Partikelname)
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                p.getLocation().add(0, 1.0, 0),
                15, 0.4, 0.4, 0.4, 0);

        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);

        int after = a.getSpawns().size();
        Dbg.d(AddSpawnSubCommand.class, "added spawn: before=" + before + " after=" + after);
        sender.sendMessage("§aSpawn hinzugefügt für §e" + arenaName + "§a. Gesamt: §e" + after + " Spawns§a.");
    }
}

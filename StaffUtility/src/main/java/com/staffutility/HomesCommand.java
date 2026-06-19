package com.staffutility;

import java.util.Map;
import java.util.UUID;

import net.pulsar.core.PulsarCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /homes <player> [home]} — staff inspection of another player's homes (from PulsarCore).
 * With no home name, lists all of the target's homes and their coordinates. With a home name,
 * teleports the staff member to that home (online sender only).
 */
public class HomesCommand implements CommandExecutor {

    private final StaffUtility plugin;

    public HomesCommand(StaffUtility plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("staffutility.homes")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /homes <player> [home]");
            return true;
        }

        PulsarCore core = resolveCore();
        if (core == null) {
            sender.sendMessage("§cPulsarCore is not available; cannot read homes.");
            return true;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        UUID targetId = target != null ? target.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();

        Map<String, Location> homes = core.getHomeManager().getHomes(targetId);
        if (homes == null || homes.isEmpty()) {
            sender.sendMessage("§e" + targetName + " §7has no homes.");
            return true;
        }

        // Teleport to a specific home
        if (args.length >= 2) {
            if (!(sender instanceof Player staff)) {
                sender.sendMessage("§cOnly players can teleport to a home.");
                return true;
            }
            Location loc = homes.get(args[1]);
            if (loc == null) {
                sender.sendMessage("§c" + targetName + " has no home named §f" + args[1] + "§c.");
                return true;
            }
            staff.teleportAsync(loc);
            sender.sendMessage("§aTeleporting to §f" + targetName + "§a's home §f" + args[1] + "§a.");
            return true;
        }

        // List all homes
        sender.sendMessage("§6Homes of §f" + targetName + " §7(" + homes.size() + "):");
        for (Map.Entry<String, Location> e : homes.entrySet()) {
            Location l = e.getValue();
            String world = l.getWorld() != null ? l.getWorld().getName() : "?";
            sender.sendMessage("§7- §e" + e.getKey() + " §8» §f"
                    + world + " " + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ()
                    + " §7(/homes " + targetName + " " + e.getKey() + ")");
        }
        return true;
    }

    private PulsarCore resolveCore() {
        try {
            org.bukkit.plugin.Plugin p = Bukkit.getPluginManager().getPlugin("PulsarCore");
            if (p instanceof PulsarCore core) {
                return core;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}

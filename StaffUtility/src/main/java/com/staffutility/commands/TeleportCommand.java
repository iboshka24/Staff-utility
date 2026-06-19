package com.staffutility.commands;

import com.staffutility.StaffUtility;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeleportCommand implements CommandExecutor {

    private final StaffUtility plugin;

    public TeleportCommand(StaffUtility plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.getStaffManager().isInStaffMode(player)) {
            player.sendMessage("§cYou must be in staff mode to use this command!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§cUsage: /gtp <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        player.teleport(target.getLocation());
        player.sendMessage("§aTeleported to §e" + target.getName());

        return true;
    }
}

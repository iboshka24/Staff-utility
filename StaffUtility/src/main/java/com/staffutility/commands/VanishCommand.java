package com.staffutility.commands;

import com.staffutility.StaffUtility;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand implements CommandExecutor {

    private final StaffUtility plugin;

    public VanishCommand(StaffUtility plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.getStaffManager().isStaff(player)) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        // Check if player is in staff mode
        if (!plugin.getStaffManager().isInStaffMode(player)) {
            player.sendMessage("§cYou must be in staff mode to use vanish!");
            return true;
        }

        if (plugin.getStaffManager().isVanished(player)) {
            plugin.getStaffManager().setVanished(player, false);
            player.sendMessage("§aYou are now visible!");
        } else {
            plugin.getStaffManager().setVanished(player, true);
            player.sendMessage("§aYou are now vanished!");
        }

        return true;
    }
}

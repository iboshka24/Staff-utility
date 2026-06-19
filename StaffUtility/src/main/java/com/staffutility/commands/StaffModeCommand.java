package com.staffutility.commands;

import com.staffutility.StaffUtility;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffModeCommand implements CommandExecutor {

    private final StaffUtility plugin;

    public StaffModeCommand(StaffUtility plugin) {
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
            player.sendMessage("§cYou are not a staff member!");
            return true;
        }

        plugin.getStaffManager().toggleStaffMode(player);

        return true;
    }
}

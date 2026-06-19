package com.staffutility.commands;

import com.staffutility.StaffUtility;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddStaffCommand implements CommandExecutor {

    private final StaffUtility plugin;

    public AddStaffCommand(StaffUtility plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("staffutility.addstaff")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /addstaff <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }

        if (plugin.getStaffManager().isStaff(target)) {
            sender.sendMessage("§e" + target.getName() + " §cis already a staff member!");
            return true;
        }

        plugin.getStaffManager().addStaff(target.getUniqueId());
        sender.sendMessage("§aAdded §e" + target.getName() + " §ato staff!");
        target.sendMessage("§aYou have been added to the staff team!");

        return true;
    }
}

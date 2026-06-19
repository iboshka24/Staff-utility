package com.staffutility.commands;

import com.staffutility.StaffUtility;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReportCommand implements CommandExecutor {

    private final StaffUtility plugin;

    public ReportCommand(StaffUtility plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("§cUsage: /report <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§cYou cannot report yourself!");
            return true;
        }

        plugin.getReportManager().addReport(target.getUniqueId(), player.getUniqueId(), "No reason provided");
        player.sendMessage("§aYou have reported §e" + target.getName() + "§a. Staff has been notified.");

        return true;
    }
}

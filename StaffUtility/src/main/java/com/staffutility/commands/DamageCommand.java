package com.staffutility.commands;

import com.staffutility.StaffUtility;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DamageCommand implements CommandExecutor {

    private final StaffUtility plugin;

    public DamageCommand(StaffUtility plugin) {
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

        if (args.length < 2) {
            player.sendMessage("§cUsage: /damage <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        double damage;
        try {
            damage = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid damage amount!");
            return true;
        }

        if (damage < 0) {
            player.sendMessage("§cDamage must be positive!");
            return true;
        }

        target.damage(damage);
        player.sendMessage("§aDamaged §e" + target.getName() + " §afor §e" + damage + " §ahearts");

        return true;
    }
}

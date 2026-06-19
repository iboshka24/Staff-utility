package com.staffutility.commands;

import com.staffutility.StaffUtility;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpectatorCommand implements CommandExecutor {

    private final StaffUtility plugin;

    public SpectatorCommand(StaffUtility plugin) {
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

        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage("§aYou are now in survival mode!");
        } else {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage("§aYou are now in spectator mode!");
        }

        return true;
    }
}

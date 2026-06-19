package com.staffutility;

import java.util.Date;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /ipban <player> <duration> [reason]} — temporarily IP-bans a player using the server's
 * native IP {@link BanList}. Duration accepts suffixes: {@code s, m, h, d, w} (e.g. {@code 7d},
 * {@code 12h}); {@code perm}/{@code 0} bans permanently. The target is kicked if online.
 */
public class IpBanCommand implements CommandExecutor {

    private final StaffUtility plugin;

    public IpBanCommand(StaffUtility plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("staffutility.ipban")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /ipban <player> <duration> [reason]");
            sender.sendMessage("§7Duration examples: §f30m, 12h, 7d, 2w, perm");
            return true;
        }

        String targetName = args[0];
        Player online = Bukkit.getPlayerExact(targetName);

        // Resolve the IP: from the online player, or fall back to the name (vanilla supports
        // banning by stored IP string only for online players, so require online for accuracy).
        String ip = null;
        if (online != null && online.getAddress() != null) {
            ip = online.getAddress().getAddress().getHostAddress();
        }
        if (ip == null) {
            sender.sendMessage("§cPlayer must be online to resolve their IP for an IP ban.");
            return true;
        }

        Date expiry = parseDuration(args[1]);
        if (expiry == null && !args[1].equalsIgnoreCase("perm") && !args[1].equals("0")) {
            sender.sendMessage("§cInvalid duration. Use e.g. 30m, 12h, 7d, 2w or perm.");
            return true;
        }

        StringBuilder reason = new StringBuilder("IP banned by " + sender.getName());
        if (args.length > 2) {
            reason.setLength(0);
            for (int i = 2; i < args.length; i++) {
                if (i > 2) reason.append(' ');
                reason.append(args[i]);
            }
        }

        BanList<?> banList = Bukkit.getBanList(BanList.Type.IP);
        banList.addBan(ip, reason.toString(), expiry, sender.getName());

        String durationText = (expiry == null) ? "permanently" : "until " + expiry;
        if (online != null) {
            final Player toKick = online;
            final String kickMsg = "§c§lYou have been IP-banned.\n§7Reason: §f" + reason
                    + "\n§7Duration: §f" + (expiry == null ? "permanent" : durationText);
            toKick.getScheduler().run(plugin, t -> toKick.kick(net.kyori.adventure.text.Component.text(kickMsg)), null);
        }

        sender.sendMessage("§aIP-banned §f" + targetName + " §a(" + ip + ") " + durationText + ".");
        Bukkit.getLogger().info("[StaffUtility] " + sender.getName() + " IP-banned " + targetName
                + " (" + ip + ") " + durationText + " — " + reason);
        return true;
    }

    /** Parses a duration like "30m"/"12h"/"7d"/"2w" into an expiry Date, or null for permanent/invalid. */
    private Date parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        if (input.equalsIgnoreCase("perm") || input.equals("0")) {
            return null;
        }
        char unit = Character.toLowerCase(input.charAt(input.length() - 1));
        String numberPart = input.substring(0, input.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(numberPart);
            if (amount <= 0) {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        long millis;
        switch (unit) {
            case 's' -> millis = amount * 1000L;
            case 'm' -> millis = amount * 60_000L;
            case 'h' -> millis = amount * 3_600_000L;
            case 'd' -> millis = amount * 86_400_000L;
            case 'w' -> millis = amount * 604_800_000L;
            default -> {
                return null;
            }
        }
        return new Date(System.currentTimeMillis() + millis);
    }
}

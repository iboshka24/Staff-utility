package com.staffutility.commands;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.staffutility.StaffUtility;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /offend <player> <reason>} — bans a player using a preset reason mapped to the server
 * rules. Macro/script/auto-clicker bans trigger a full account wipe (via PulsarCore). RMT/IRL
 * trading, doxxing/death-threats and ban-evasion are permanent.
 */
public class OffendCommand implements CommandExecutor {

    private final StaffUtility plugin;

    public OffendCommand(StaffUtility plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        if (!plugin.getStaffManager().isInStaffMode(player)) {
            player.sendMessage("§cYou must be in staff mode to use this command!");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("§cUsage: /offend <player> <reason>");
            player.sendMessage("§eReasons: §7" + validReasons());
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        String key = args[1].toLowerCase();
        long duration = banDuration(key);          // -2 = invalid, -1 = permanent
        if (duration == -2) {
            player.sendMessage("§cInvalid reason! Valid: §7" + validReasons());
            return true;
        }
        String reasonText = reasonText(key);
        boolean fullWipe = shouldFullWipe(key);

        if (fullWipe) {
            // Full account wipe on the target's region thread via PulsarCore.
            final Player t = target;
            t.getScheduler().run(plugin, task -> {
                org.bukkit.plugin.Plugin core = Bukkit.getPluginManager().getPlugin("PulsarCore");
                if (core instanceof net.pulsar.core.PulsarCore pc) {
                    pc.getPlayerWipeService().wipeOnline(t);
                }
            }, null);
        }

        Date expiry = duration < 0 ? null : new Date(System.currentTimeMillis() + duration);
        String durationLabel = (duration < 0 ? "PERMANENT" : formatDuration(duration));
        String fullReason = reasonText + (fullWipe ? " (Full Wipe)" : "");

        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), fullReason, expiry, player.getName());
        final Player toKick = target;
        toKick.getScheduler().run(plugin, task -> toKick.kick(net.kyori.adventure.text.Component.text(
                "§c§lYou have been banned!\n§fReason: §e" + fullReason + "\n§fDuration: §e" + durationLabel)), null);

        Bukkit.broadcast(net.kyori.adventure.text.Component.text(
                "§c" + target.getName() + " §fwas banned by §a" + player.getName()
                        + " §ffor §e" + fullReason + " §7(" + durationLabel + ")"));

        plugin.getReportManager().clearReports(target.getUniqueId());
        return true;
    }

    private String validReasons() {
        return "hacks, xray, esp, freecam, movemods, invmods, macros, scripts, autoclicker, "
                + "killaura, damageindicators, radar, bugabuse, dupe, rmt, irl, seed, alts, "
                + "banevasion, muteevasion, spam, harassment, racism, hatespeech, doxxing, "
                + "deaththreats, advertising, impersonation, lying, cheating";
    }

    /** Ban length in millis; {@code -1} permanent, {@code -2} invalid reason. */
    private long banDuration(String key) {
        return switch (key) {
            // Permanent
            case "rmt", "irl", "doxxing", "deaththreats", "banevasion" -> -1;
            // Cheats / clients
            case "hacks", "killaura", "xray", "esp", "freecam" -> TimeUnit.DAYS.toMillis(60);
            case "movemods", "invmods" -> TimeUnit.DAYS.toMillis(30);
            case "macros", "scripts", "autoclicker" -> TimeUnit.DAYS.toMillis(7);
            case "damageindicators", "radar" -> TimeUnit.DAYS.toMillis(14);
            case "bugabuse", "dupe" -> TimeUnit.DAYS.toMillis(30);
            case "seed" -> TimeUnit.DAYS.toMillis(14);
            case "alts" -> TimeUnit.DAYS.toMillis(7);
            case "cheating" -> TimeUnit.DAYS.toMillis(14);
            // Chat / community
            case "spam", "advertising" -> TimeUnit.DAYS.toMillis(3);
            case "harassment", "racism", "hatespeech", "impersonation", "muteevasion" -> TimeUnit.DAYS.toMillis(14);
            case "lying" -> TimeUnit.DAYS.toMillis(7);
            default -> -2;
        };
    }

    private boolean shouldFullWipe(String key) {
        return key.equals("macros") || key.equals("scripts") || key.equals("autoclicker");
    }

    private String reasonText(String key) {
        return switch (key) {
            case "hacks" -> "Hacked/Unauthorized Client";
            case "killaura" -> "Kill Aura";
            case "xray" -> "X-Ray";
            case "esp" -> "ESP";
            case "freecam" -> "Freecam";
            case "movemods" -> "Movement Modifications";
            case "invmods" -> "Inventory Modifications";
            case "macros" -> "Macros";
            case "scripts" -> "Scripts";
            case "autoclicker" -> "Auto-Clicker";
            case "damageindicators" -> "Damage Indicators";
            case "radar" -> "Entity Radar";
            case "bugabuse" -> "Bug Abuse";
            case "dupe" -> "Item Duplication";
            case "rmt" -> "Real Money Trading (RMT)";
            case "irl" -> "IRL / Cross-Server Trading";
            case "seed" -> "World Seed Cracking";
            case "alts" -> "Account Limit Exceeded (max 5)";
            case "banevasion" -> "Ban Evasion";
            case "muteevasion" -> "Mute Evasion";
            case "spam" -> "Chat/Voice Spamming";
            case "harassment" -> "Harassment";
            case "racism" -> "Racism";
            case "hatespeech" -> "Hate Speech / Discrimination";
            case "doxxing" -> "Doxxing / Sharing Personal Data";
            case "deaththreats" -> "Death Threats";
            case "advertising" -> "Advertising / Promotion";
            case "impersonation" -> "Impersonating Staff";
            case "lying" -> "Lying to Staff";
            case "cheating" -> "Cheating (AntiCheat)";
            default -> key;
        };
    }

    private String formatDuration(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        if (days >= 1) {
            return days + " days";
        }
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        return hours + " hours";
    }
}

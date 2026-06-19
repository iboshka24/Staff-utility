package com.staffutility.commands;

import com.staffutility.StaffUtility;
import com.staffutility.managers.SusManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class SusCommand implements CommandExecutor {

    private final StaffUtility plugin;
    private static final int ITEMS_PER_PAGE = 45; // 5 rows of 9 slots

    public SusCommand(StaffUtility plugin) {
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

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid page number!");
                return true;
            }
        }

        openSusGUI(player, page);
        return true;
    }

    public void openSusGUI(Player player, int page) {
        List<UUID> flaggedPlayers = plugin.getSusManager().getFlaggedPlayers();

        if (flaggedPlayers.isEmpty()) {
            player.sendMessage("§aNo flagged players at the moment.");
            return;
        }

        int totalPages = (int) Math.ceil((double) flaggedPlayers.size() / ITEMS_PER_PAGE);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        Inventory gui = Bukkit.createInventory(null, 54, "§c§lSus | Page " + page);

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, flaggedPlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            UUID uuid = flaggedPlayers.get(i);
            SusManager.PlayerFlagData data = plugin.getSusManager().getFlagData(uuid);
            
            if (data == null) continue;

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            meta.setOwningPlayer(offlinePlayer);
            
            meta.setDisplayName("§e" + data.getPlayerName());
            
            List<String> lore = new ArrayList<>();
            
            // Group flags by reason and sum their VL
            Map<String, Double> reasonVLMap = new LinkedHashMap<>();
            for (SusManager.FlagEntry flag : data.getFlags()) {
                reasonVLMap.merge(flag.getReason(), flag.getVL(), Double::sum);
            }
            
            // Display each unique reason with total VL
            lore.add("§7Flags:");
            for (Map.Entry<String, Double> entry : reasonVLMap.entrySet()) {
                lore.add(String.format("  §c%s §7- §e%.1f VL", entry.getKey(), entry.getValue()));
            }
            
            lore.add("");
            lore.add(String.format("§7Total VL: §e%.1f", data.getTotalVL()));
            lore.add("§7Total Flags: §e" + data.getTotalFlags());
            lore.add("§7Last Seen: §e" + data.getFormattedLastSeen());
            lore.add("§7World: §e" + data.getLastWorld());
            lore.add("");
            lore.add("§aClick to teleport");
            
            meta.setLore(lore);
            skull.setItemMeta(meta);
            
            gui.setItem(i - startIndex, skull);
        }

        // Navigation items
        if (page > 1) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            var prevMeta = prevPage.getItemMeta();
            prevMeta.setDisplayName("§e← Previous Page");
            prevPage.setItemMeta(prevMeta);
            gui.setItem(45, prevPage);
        }

        if (page < totalPages) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            var nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName("§eNext Page →");
            nextPage.setItemMeta(nextMeta);
            gui.setItem(53, nextPage);
        }

        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        var closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§cClose");
        close.setItemMeta(closeMeta);
        gui.setItem(49, close);

        player.openInventory(gui);
    }
}

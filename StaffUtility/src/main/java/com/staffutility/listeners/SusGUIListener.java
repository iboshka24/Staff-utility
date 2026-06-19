package com.staffutility.listeners;

import com.staffutility.StaffUtility;
import com.staffutility.commands.SusCommand;
import com.staffutility.managers.SusManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class SusGUIListener implements Listener {

    private final StaffUtility plugin;

    public SusGUIListener(StaffUtility plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        
        if (!title.startsWith("§c§lSus | Page ")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        // Handle navigation
        if (clicked.getType() == Material.ARROW) {
            String displayName = clicked.getItemMeta().getDisplayName();
            int currentPage = extractPageNumber(title);
            
            if (displayName.contains("Previous")) {
                new SusCommand(plugin).openSusGUI(player, currentPage - 1);
            } else if (displayName.contains("Next")) {
                new SusCommand(plugin).openSusGUI(player, currentPage + 1);
            }
            return;
        }

        // Handle close button
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Handle player skull click
        if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            OfflinePlayer target = meta.getOwningPlayer();
            
            if (target == null) {
                player.sendMessage("§cCould not identify player!");
                return;
            }

            UUID targetUUID = target.getUniqueId();
            
            // Проверяем, онлайн ли игрок
            Player onlineTarget = Bukkit.getPlayer(targetUUID);
            
            if (onlineTarget != null && onlineTarget.isOnline()) {
                // Игрок онлайн - телепортируем к текущей позиции
                player.closeInventory();
                player.teleport(onlineTarget.getLocation());
                player.sendMessage("§aTeleported to §e" + onlineTarget.getName() + " §a(online)");
                player.sendMessage("§7World: §e" + onlineTarget.getWorld().getName());
            } else {
                // Игрок оффлайн - используем последнюю известную локацию
                SusManager.PlayerFlagData data = plugin.getSusManager().getFlagData(targetUUID);
                
                if (data == null) {
                    player.sendMessage("§cNo flag data found for this player!");
                    return;
                }

                Location lastLocation = data.getLastLocation();
                
                if (lastLocation == null) {
                    player.sendMessage("§cNo last known location for this player!");
                    return;
                }

                player.closeInventory();
                player.teleport(lastLocation);
                player.sendMessage("§aTeleported to §e" + data.getPlayerName() + "§a's last known location §7(offline)");
                player.sendMessage("§7World: §e" + data.getLastWorld() + " §7| Last seen: §e" + data.getFormattedLastSeen());
            }
        }
    }

    private int extractPageNumber(String title) {
        try {
            String[] parts = title.split("Page ");
            if (parts.length > 1) {
                return Integer.parseInt(parts[1].replaceAll("§.", "").trim());
            }
        } catch (Exception e) {
            // Ignore
        }
        return 1;
    }
}

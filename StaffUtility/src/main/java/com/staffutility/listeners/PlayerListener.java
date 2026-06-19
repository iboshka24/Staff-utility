package com.staffutility.listeners;

import com.staffutility.StaffUtility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final StaffUtility plugin;

    public PlayerListener(StaffUtility plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getStaffManager().isStaff(player)) {
            player.addAttachment(plugin, "staffutility.staff", true);
            player.addAttachment(plugin, "staffutility.offend", true);
            player.addAttachment(plugin, "staffutility.sus", true);
            player.addAttachment(plugin, "staffutility.gmsp", true);
            player.addAttachment(plugin, "staffutility.gtp", true);
            player.addAttachment(plugin, "staffutility.damage", true);
            player.addAttachment(plugin, "staffutility.vanish", true);
        }

        // Hide vanished players from non-staff
        if (!plugin.getStaffManager().isStaff(player)) {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getStaffManager().isVanished(online)) {
                    player.hidePlayer(plugin, online);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getStaffManager().isInStaffMode(event.getPlayer())) {
            plugin.getStaffManager().disableStaffMode(event.getPlayer());
        }
    }
}

package com.staffutility.managers;

import com.staffutility.StaffUtility;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StaffManager {

    private final StaffUtility plugin;
    private final Set<UUID> staffMembers;
    private final Set<UUID> staffModeEnabled;
    private final Set<UUID> vanishedPlayers;
    private final Map<UUID, GameMode> previousGameMode;
    private File staffFile;
    private FileConfiguration staffConfig;

    public StaffManager(StaffUtility plugin) {
        this.plugin = plugin;
        this.staffMembers = new HashSet<>();
        this.staffModeEnabled = new HashSet<>();
        this.vanishedPlayers = new HashSet<>();
        this.previousGameMode = new HashMap<>();
        loadStaffData();
    }

    private void loadStaffData() {
        staffFile = new File(plugin.getDataFolder(), "staff.yml");
        if (!staffFile.exists()) {
            plugin.saveResource("staff.yml", false);
        }
        staffConfig = YamlConfiguration.loadConfiguration(staffFile);

        List<String> staffList = staffConfig.getStringList("staff");
        for (String uuidString : staffList) {
            try {
                staffMembers.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in staff.yml: " + uuidString);
            }
        }
    }

    public void saveStaffData() {
        List<String> staffList = new ArrayList<>();
        for (UUID uuid : staffMembers) {
            staffList.add(uuid.toString());
        }
        staffConfig.set("staff", staffList);
        try {
            staffConfig.save(staffFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save staff.yml!");
            e.printStackTrace();
        }
    }

    public boolean isStaff(Player player) {
        return staffMembers.contains(player.getUniqueId());
    }

    public boolean isInStaffMode(Player player) {
        return staffModeEnabled.contains(player.getUniqueId());
    }

    public void addStaff(UUID uuid) {
        staffMembers.add(uuid);
        saveStaffData();

        // Grant permissions to online player
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            grantStaffPermissions(player);
        }
    }

    public void removeStaff(UUID uuid) {
        staffMembers.remove(uuid);
        staffModeEnabled.remove(uuid);
        saveStaffData();

        // Remove permissions from online player
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            revokeStaffPermissions(player);
        }
    }

    private void grantStaffPermissions(Player player) {
        player.addAttachment(plugin, "staffutility.staff", true);
        player.addAttachment(plugin, "staffutility.offend", true);
        player.addAttachment(plugin, "staffutility.sus", true);
        player.addAttachment(plugin, "staffutility.gmsp", true);
        player.addAttachment(plugin, "staffutility.gtp", true);
        player.addAttachment(plugin, "staffutility.damage", true);
        player.addAttachment(plugin, "staffutility.vanish", true);
    }

    private void revokeStaffPermissions(Player player) {
        player.addAttachment(plugin, "staffutility.staff", false);
        player.addAttachment(plugin, "staffutility.offend", false);
        player.addAttachment(plugin, "staffutility.sus", false);
        player.addAttachment(plugin, "staffutility.gmsp", false);
        player.addAttachment(plugin, "staffutility.gtp", false);
        player.addAttachment(plugin, "staffutility.damage", false);
        player.addAttachment(plugin, "staffutility.vanish", false);
    }

    public void enableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();
        if (!staffModeEnabled.contains(uuid)) {
            previousGameMode.put(uuid, player.getGameMode());
            staffModeEnabled.add(uuid);
            player.sendMessage("§a§lStaff Mode enabled!");
        }
    }

    public void disableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();
        if (staffModeEnabled.contains(uuid)) {
            staffModeEnabled.remove(uuid);
            
            // Disable vanish if enabled
            if (isVanished(player)) {
                setVanished(player, false);
            }
            
            GameMode previousMode = previousGameMode.getOrDefault(uuid, GameMode.SURVIVAL);
            player.setGameMode(previousMode);
            previousGameMode.remove(uuid);
            player.sendMessage("§c§lStaff Mode disabled!");
        }
    }

    public void toggleStaffMode(Player player) {
        if (isInStaffMode(player)) {
            disableStaffMode(player);
        } else {
            enableStaffMode(player);
        }
    }

    public void disableAllStaffMode() {
        for (UUID uuid : new HashSet<>(staffModeEnabled)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                disableStaffMode(player);
            }
        }
    }

    public Set<UUID> getStaffMembers() {
        return new HashSet<>(staffMembers);
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    public void setVanished(Player player, boolean vanished) {
        UUID uuid = player.getUniqueId();
        if (vanished) {
            vanishedPlayers.add(uuid);
            // Hide player from all non-staff players (both in-game and in tab)
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!isStaff(online)) {
                    online.hidePlayer(plugin, player);
                }
            }
            plugin.getLogger().info(player.getName() + " is now vanished");
        } else {
            vanishedPlayers.remove(uuid);
            // Show player to all players
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
            plugin.getLogger().info(player.getName() + " is no longer vanished");
        }
    }
}

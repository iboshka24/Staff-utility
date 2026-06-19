package com.staffutility.managers;

import com.staffutility.StaffUtility;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SusManager {

    private final StaffUtility plugin;
    private final Map<UUID, PlayerFlagData> flaggedPlayers;
    private final SimpleDateFormat dateFormat;

    public SusManager(StaffUtility plugin) {
        this.plugin = plugin;
        this.flaggedPlayers = new ConcurrentHashMap<>();
        this.dateFormat = new SimpleDateFormat("HH:mm:ss");
    }

    /**
     * Add a flag to a player with a specific reason and VL increment
     * @param player The player to flag
     * @param reason The reason for the flag
     * @param vlIncrement The VL (Violation Level) to add
     */
    public void addFlag(Player player, String reason, double vlIncrement) {
        UUID uuid = player.getUniqueId();
        PlayerFlagData data = flaggedPlayers.computeIfAbsent(uuid, k -> new PlayerFlagData(player.getName()));
        
        data.addFlag(reason, vlIncrement, player.getLocation(), player.getWorld().getName());
        
        // Notify staff
        notifyStaff(player, reason, vlIncrement, data.getTotalVL());
    }

    /**
     * Add a flag with default VL of 1.0
     */
    public void addFlag(Player player, String reason) {
        addFlag(player, reason, 1.0);
    }

    private void notifyStaff(Player player, String reason, double vlIncrement, double totalVL) {
        String message = String.format("§c§l[ANTICHEAT] §e%s §fflagged: §c%s §7(+%.1f VL, Total: %.1f VL)",
            player.getName(), reason, vlIncrement, totalVL);

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("staffutility.admin") || plugin.getStaffManager().isStaff(staff)) {
                staff.sendMessage(message);
            }
        }

        plugin.getLogger().info(String.format("Player %s flagged: %s (+%.1f VL, Total: %.1f VL)",
            player.getName(), reason, vlIncrement, totalVL));
    }

    public List<UUID> getFlaggedPlayers() {
        return new ArrayList<>(flaggedPlayers.keySet());
    }

    public PlayerFlagData getFlagData(UUID uuid) {
        return flaggedPlayers.get(uuid);
    }

    public void clearFlags(UUID uuid) {
        flaggedPlayers.remove(uuid);
    }

    public boolean hasFlagsAboveThreshold(UUID uuid, double threshold) {
        PlayerFlagData data = flaggedPlayers.get(uuid);
        return data != null && data.getTotalVL() >= threshold;
    }

    public class PlayerFlagData {
        private final String playerName;
        private final List<FlagEntry> flags;
        private double totalVL;
        private Location lastLocation;
        private String lastWorld;
        private long lastFlagTime;

        public PlayerFlagData(String playerName) {
            this.playerName = playerName;
            this.flags = new ArrayList<>();
            this.totalVL = 0.0;
            this.lastFlagTime = System.currentTimeMillis();
        }

        public void addFlag(String reason, double vl, Location location, String world) {
            flags.add(new FlagEntry(reason, vl, System.currentTimeMillis()));
            totalVL += vl;
            lastLocation = location.clone();
            lastWorld = world;
            lastFlagTime = System.currentTimeMillis();
        }

        public String getPlayerName() {
            return playerName;
        }

        public List<FlagEntry> getFlags() {
            return new ArrayList<>(flags);
        }

        public FlagEntry getLatestFlag() {
            return flags.isEmpty() ? null : flags.get(flags.size() - 1);
        }

        public double getTotalVL() {
            return totalVL;
        }

        public int getTotalFlags() {
            return flags.size();
        }

        public Location getLastLocation() {
            return lastLocation;
        }

        public String getLastWorld() {
            return lastWorld;
        }

        public long getLastFlagTime() {
            return lastFlagTime;
        }

        public String getFormattedLastSeen() {
            return dateFormat.format(new Date(lastFlagTime));
        }
    }

    public class FlagEntry {
        private final String reason;
        private final double vl;
        private final long timestamp;

        public FlagEntry(String reason, double vl, long timestamp) {
            this.reason = reason;
            this.vl = vl;
            this.timestamp = timestamp;
        }

        public String getReason() {
            return reason;
        }

        public double getVL() {
            return vl;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getFormattedTime() {
            return dateFormat.format(new Date(timestamp));
        }
    }
}

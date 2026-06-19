package com.staffutility.listeners;

import com.staffutility.StaffUtility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GrimListener implements Listener {

    private final StaffUtility plugin;
    private boolean registered = false;
    private final Map<UUID, Double> lastViolations = new HashMap<>();
    private Object grimAPI;
    private Method getPlayerDataManagerMethod;
    private Method getPlayerMethod;

    public GrimListener(StaffUtility plugin) {
        this.plugin = plugin;
    }

    public void register() {
        try {
            // Get GrimAPI instance using reflection
            Class<?> grimAPIClass = Class.forName("ac.grim.grimac.GrimAPI");
            Object grimAPIInstance = grimAPIClass.getField("INSTANCE").get(null);
            this.grimAPI = grimAPIInstance;

            // Get PlayerDataManager
            getPlayerDataManagerMethod = grimAPIClass.getMethod("getPlayerDataManager");

            // Get methods for checking violations
            Object playerDataManager = getPlayerDataManagerMethod.invoke(grimAPI);
            Class<?> playerDataManagerClass = playerDataManager.getClass();
            getPlayerMethod = playerDataManagerClass.getMethod("getPlayer", UUID.class);

            // Start polling task - check every 2 seconds
            Bukkit.getScheduler().runTaskTimer(plugin, this::checkAllPlayers, 40L, 40L);

            Bukkit.getPluginManager().registerEvents(this, plugin);

            registered = true;
            plugin.getLogger().info("Successfully registered GrimAC integration (polling mode)");

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register Grim listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkAllPlayers() {
        if (grimAPI == null) {
            return;
        }

        try {
            Object playerDataManager = getPlayerDataManagerMethod.invoke(grimAPI);

            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    Object grimPlayer = getPlayerMethod.invoke(playerDataManager, player.getUniqueId());
                    if (grimPlayer == null) {
                        continue;
                    }

                    // Get all checks using getChecks() method
                    Method getChecksMethod = grimPlayer.getClass().getMethod("getChecks");
                    Collection<?> checks = (Collection<?>) getChecksMethod.invoke(grimPlayer);

                    // Calculate total violations from all checks
                    double totalViolations = 0;
                    String highestCheckName = "";
                    double highestCheckVL = 0;

                    for (Object check : checks) {
                        try {
                            Method getViolationsMethod = check.getClass().getMethod("getViolations");
                            Method getCheckNameMethod = check.getClass().getMethod("getCheckName");

                            double violations = (double) getViolationsMethod.invoke(check);
                            String checkName = (String) getCheckNameMethod.invoke(check);

                            // Filter out "time limit" and "time out" checks
                            if (checkName != null) {
                                String checkNameLower = checkName.toLowerCase();
                                if (checkNameLower.contains("time limit") ||
                                    checkNameLower.contains("timeout") ||
                                    checkNameLower.contains("time out")) {
                                    // Skip these checks - don't add to total violations
                                    continue;
                                }
                            }

                            totalViolations += violations;

                            if (violations > highestCheckVL) {
                                highestCheckVL = violations;
                                highestCheckName = checkName;
                            }
                        } catch (Exception e) {
                            // Skip this check if error
                        }
                    }

                    UUID uuid = player.getUniqueId();
                    Double lastVL = lastViolations.get(uuid);

                    if (lastVL == null) {
                        lastViolations.put(uuid, totalViolations);
                        continue;
                    }

                    // If violations increased and reached threshold
                    if (totalViolations > lastVL && totalViolations >= 10) {
                        double vlIncrement = totalViolations - lastVL;

                        // Use more detailed reason with check name
                        String reason = String.format("GrimAC: %s", highestCheckName);

                        // Use new SusManager API
                        plugin.getSusManager().addFlag(player, reason, vlIncrement);

                        plugin.getLogger().info(String.format("Added %s to /sus: %s (VL: +%.1f, Total: %.0f)",
                            player.getName(), reason, vlIncrement, totalViolations));
                    }

                    lastViolations.put(uuid, totalViolations);

                } catch (Exception e) {
                    // Skip this player if error
                    plugin.getLogger().warning("Error checking player " + player.getName() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking Grim violations: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        lastViolations.remove(event.getPlayer().getUniqueId());
    }

    public boolean isRegistered() {
        return registered;
    }
}

package com.staffutility.listeners;

import com.staffutility.StaffUtility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to PulsarAntiCheat Baritone flags and adds them to /sus
 */
public class PulsarListener implements Listener {

    private final StaffUtility plugin;
    private boolean registered = false;
    private final Map<UUID, Double> lastBaritoneVL = new HashMap<>();
    private Object pulsarPlugin;
    private Method getViolationManagerMethod;

    public PulsarListener(StaffUtility plugin) {
        this.plugin = plugin;
    }

    public void register() {
        try {
            // Get PulsarAntiCheat instance
            pulsarPlugin = Bukkit.getPluginManager().getPlugin("PulsarAntiCheat");
            if (pulsarPlugin == null) {
                plugin.getLogger().warning("PulsarAntiCheat not found. Pulsar integration disabled.");
                return;
            }

            plugin.getLogger().info("Found PulsarAntiCheat: " + ((org.bukkit.plugin.Plugin) pulsarPlugin).getDescription().getVersion());
            plugin.getLogger().info("PulsarAntiCheat class: " + pulsarPlugin.getClass().getName());

            // List ALL methods in PulsarAntiCheat
            plugin.getLogger().info("=== Available methods in PulsarAntiCheat ===");
            for (Method m : pulsarPlugin.getClass().getMethods()) {
                if (!m.getDeclaringClass().getName().startsWith("java.") &&
                    !m.getDeclaringClass().getName().startsWith("org.bukkit.plugin")) {
                    plugin.getLogger().info("  - " + m.getName() + " returns " + m.getReturnType().getSimpleName());
                }
            }
            plugin.getLogger().info("===========================================");

            // Try to get ViolationManager
            Class<?> pulsarClass = pulsarPlugin.getClass();
            getViolationManagerMethod = pulsarClass.getMethod("getViolationManager");
            plugin.getLogger().info("✓ Found method: getViolationManager");

            // Test invocation
            Object violationManager = getViolationManagerMethod.invoke(pulsarPlugin);
            plugin.getLogger().info("✓ Successfully got ViolationManager: " + violationManager.getClass().getName());

            // List ALL methods in ViolationManager
            plugin.getLogger().info("=== Available methods in ViolationManager ===");
            for (Method m : violationManager.getClass().getMethods()) {
                if (!m.getDeclaringClass().getName().startsWith("java.")) {
                    plugin.getLogger().info("  - " + m.getName() + " returns " + m.getReturnType().getSimpleName());
                }
            }
            plugin.getLogger().info("===========================================");

            // Start polling task - check every 3 seconds
            Bukkit.getScheduler().runTaskTimer(plugin, this::checkAllPlayers, 60L, 60L);

            Bukkit.getPluginManager().registerEvents(this, plugin);

            registered = true;
            plugin.getLogger().info("✓ PulsarAntiCheat integration registered (polling mode)");

        } catch (NoSuchMethodException e) {
            plugin.getLogger().severe("Method not found: " + e.getMessage());
            plugin.getLogger().severe("Available methods in PulsarAntiCheat:");
            for (Method m : pulsarPlugin.getClass().getMethods()) {
                plugin.getLogger().severe("  - " + m.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register Pulsar listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkAllPlayers() {
        if (pulsarPlugin == null) {
            return;
        }

        int playersChecked = 0;
        int playersWithVL = 0;

        try {
            Object violationManager = getViolationManagerMethod.invoke(pulsarPlugin);

            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    UUID uuid = player.getUniqueId();

                    // Get player violation data using reflection
                    Method getPlayerDataMethod = violationManager.getClass().getMethod("getOrCreateData", Player.class);
                    Object playerData = getPlayerDataMethod.invoke(violationManager, player);

                    if (playerData == null) {
                        continue;
                    }

                    // Get Snap VL (Baritone-like detection)
                    Method getSnapVLMethod = playerData.getClass().getMethod("getSnapVL");
                    double snapVL = (double) getSnapVLMethod.invoke(playerData);

                    // Get Linearity VL (robotic movement)
                    Method getLinearityVLMethod = playerData.getClass().getMethod("getLinearityVL");
                    double linearityVL = (double) getLinearityVLMethod.invoke(playerData);

                    // Total VL = Snap + Linearity
                    double totalVL = snapVL + linearityVL;

                    playersChecked++;

                    if (totalVL > 0) {
                        playersWithVL++;
                        plugin.getLogger().info(String.format("%s has VL: Snap=%.1f, Linearity=%.1f, Total=%.1f",
                            player.getName(), snapVL, linearityVL, totalVL));
                    }

                    Double lastVL = lastBaritoneVL.get(uuid);

                    if (lastVL == null) {
                        lastBaritoneVL.put(uuid, totalVL);
                        // Only add to /sus if VL is above threshold (3.0)
                        if (totalVL >= 3.0) {
                            String reason = "Baritone detected (Snap + Linearity)";
                            plugin.getLogger().info(String.format(
                                ">>> ADDING TO /SUS: %s | reason: %s | VL: %.1f",
                                player.getName(), reason, totalVL
                            ));
                            plugin.getSusManager().addFlag(player, reason, totalVL);
                            plugin.getLogger().info(">>> Successfully added to /sus");
                        }
                        continue;
                    }

                    // If VL increased and reached threshold (3.0), add to /sus
                    if (totalVL > lastVL && totalVL >= 3.0) {
                        double vlIncrement = totalVL - lastVL;
                        String reason = "Baritone detected (Snap + Linearity)";

                        plugin.getLogger().info(String.format(
                            ">>> ADDING TO /SUS: %s | reason: %s | VL: +%.1f (Total: %.1f)",
                            player.getName(), reason, vlIncrement, totalVL
                        ));

                        // Add to /sus using SusManager
                        plugin.getSusManager().addFlag(player, reason, vlIncrement);

                        plugin.getLogger().info(">>> Successfully added to /sus");
                    }

                    lastBaritoneVL.put(uuid, totalVL);

                } catch (Exception e) {
                    plugin.getLogger().warning("Error checking player " + player.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if (playersChecked > 0 || playersWithVL > 0) {
                plugin.getLogger().info(String.format("[Pulsar Check] %d players checked, %d with VL",
                    playersChecked, playersWithVL));
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error in checkAllPlayers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        lastBaritoneVL.remove(event.getPlayer().getUniqueId());
    }

    public boolean isRegistered() {
        return registered;
    }
}

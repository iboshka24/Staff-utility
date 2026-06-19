package com.staffutility;

import com.github.retrooper.packetevents.PacketEvents;
import com.staffutility.commands.*;
import com.staffutility.listeners.PlayerListener;
import com.staffutility.listeners.GrimListener;
import com.staffutility.listeners.PulsarListener;
import com.staffutility.listeners.SpectatorHideListener;
import com.staffutility.listeners.SusGUIListener;
import com.staffutility.managers.StaffManager;
import com.staffutility.managers.ReportManager;
import com.staffutility.managers.DiscordWebhookManager;
import com.staffutility.managers.SusManager;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class StaffUtility extends JavaPlugin {

    private static StaffUtility instance;
    private StaffManager staffManager;
    private ReportManager reportManager;
    private DiscordWebhookManager discordWebhookManager;
    private SusManager susManager;
    private Logger logger;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
            .reEncodeByDefault(false)
            .checkForUpdates(false)
            .bStats(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        saveDefaultConfig();

        PacketEvents.getAPI().init();
        PacketEvents.getAPI().getEventManager().registerListener(new SpectatorHideListener(this));

        staffManager = new StaffManager(this);
        reportManager = new ReportManager(this);
        discordWebhookManager = new DiscordWebhookManager(this);
        susManager = new SusManager(this);

        registerCommands();
        registerListeners();

        // Register GrimAC integration after a delay to ensure GrimAC is fully loaded
        Plugin grimPlugin = Bukkit.getPluginManager().getPlugin("GrimAC");
        if (grimPlugin != null && grimPlugin.isEnabled()) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                try {
                    GrimListener grimListener = new GrimListener(this);
                    grimListener.register();
                    if (grimListener.isRegistered()) {
                        logger.info("GrimAC detected! Enabling integration with /sus");
                    } else {
                        logger.warning("GrimAC found but failed to register listener");
                    }
                } catch (Exception e) {
                    logger.severe("Failed to initialize GrimAC integration: " + e.getMessage());
                    e.printStackTrace();
                }
            }, 20L); // Wait 1 second for GrimAC to fully initialize
        } else {
            logger.warning("GrimAC not found. Grim integration disabled.");
        }

        // Register PulsarAntiCheat integration
        Plugin pulsarPlugin = Bukkit.getPluginManager().getPlugin("PulsarAntiCheat");
        if (pulsarPlugin != null && pulsarPlugin.isEnabled()) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                try {
                    PulsarListener pulsarListener = new PulsarListener(this);
                    pulsarListener.register();
                    if (pulsarListener.isRegistered()) {
                        logger.info("PulsarAntiCheat detected! Enabling Baritone detection integration with /sus");
                    } else {
                        logger.warning("PulsarAntiCheat found but failed to register listener");
                    }
                } catch (Exception e) {
                    logger.severe("Failed to initialize PulsarAntiCheat integration: " + e.getMessage());
                    e.printStackTrace();
                }
            }, 100L);
        } else {
            logger.warning("PulsarAntiCheat not found. Pulsar integration disabled.");
        }

        logger.info("StaffUtility has been enabled!");
        logger.info("Spectator mode is now hidden in TAB list");
    }

    @Override
    public void onDisable() {
        staffManager.disableAllStaffMode();
        PacketEvents.getAPI().terminate();
        logger.info("StaffUtility has been disabled!");
    }

    private void registerCommands() {
        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("sfmode").setExecutor(new StaffModeCommand(this));
        getCommand("offend").setExecutor(new OffendCommand(this));
        getCommand("nv").setExecutor(new NightVisionCommand(this));
        getCommand("sus").setExecutor(new SusCommand(this));
        getCommand("gmsp").setExecutor(new SpectatorCommand(this));
        getCommand("gtp").setExecutor(new TeleportCommand(this));
        getCommand("damage").setExecutor(new DamageCommand(this));
        getCommand("addstaff").setExecutor(new AddStaffCommand(this));
        getCommand("vanish").setExecutor(new VanishCommand(this));
        getCommand("ipban").setExecutor(new IpBanCommand(this));
        getCommand("homes").setExecutor(new HomesCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new SusGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new com.staffutility.listeners.ProfanityFilter(this), this);
    }

    public static StaffUtility getInstance() {
        return instance;
    }

    public StaffManager getStaffManager() {
        return staffManager;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public DiscordWebhookManager getDiscordWebhookManager() {
        return discordWebhookManager;
    }

    public SusManager getSusManager() {
        return susManager;
    }
}

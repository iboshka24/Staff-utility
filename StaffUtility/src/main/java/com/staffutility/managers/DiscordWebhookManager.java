package com.staffutility.managers;

import com.staffutility.StaffUtility;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class DiscordWebhookManager {

    private final StaffUtility plugin;
    private String webhookUrl;
    private boolean enabled;
    private int embedColor;
    private String embedTitle;
    private String embedFooter;

    public DiscordWebhookManager(StaffUtility plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
        embedColor = plugin.getConfig().getInt("discord.embed-color", 15158332);
        embedTitle = plugin.getConfig().getString("discord.embed-title", "Player Report");
        embedFooter = plugin.getConfig().getString("discord.embed-footer", "StaffUtility Report System");
    }

    public void sendReportNotification(String reportedPlayer, String reporter) {
        if (!enabled || webhookUrl.isEmpty() || webhookUrl.equals("https://discord.com/api/webhooks/YOUR_WEBHOOK_URL_HERE")) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String timestamp = Instant.now().toString();

                String json = String.format(
                    "{\"embeds\":[{\"title\":\"%s\",\"color\":%d,\"fields\":[{\"name\":\"Reported Player\",\"value\":\"%s\",\"inline\":true},{\"name\":\"Reporter\",\"value\":\"%s\",\"inline\":true}],\"footer\":{\"text\":\"%s\"},\"timestamp\":\"%s\"}]}",
                    embedTitle,
                    embedColor,
                    reportedPlayer,
                    reporter,
                    embedFooter,
                    timestamp
                );

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != 204 && responseCode != 200) {
                    plugin.getLogger().warning("Discord webhook returned response code: " + responseCode);
                }

                connection.disconnect();

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    public boolean isEnabled() {
        return enabled;
    }
}

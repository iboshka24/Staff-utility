package com.staffutility.managers;

import com.staffutility.StaffUtility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class ReportManager {

    private final StaffUtility plugin;
    private final Map<UUID, List<Report>> reports;

    public ReportManager(StaffUtility plugin) {
        this.plugin = plugin;
        this.reports = new HashMap<>();
    }

    public void addReport(UUID reportedPlayer, UUID reporter, String reason) {
        reports.computeIfAbsent(reportedPlayer, k -> new ArrayList<>()).add(new Report(reporter, reason, System.currentTimeMillis()));

        notifyStaff(reportedPlayer, reporter);
    }

    private void notifyStaff(UUID reportedPlayer, UUID reporter) {
        Player reported = Bukkit.getPlayer(reportedPlayer);
        Player reporterPlayer = Bukkit.getPlayer(reporter);

        String reportedName = reported != null ? reported.getName() : reportedPlayer.toString();
        String reporterName = reporterPlayer != null ? reporterPlayer.getName() : reporter.toString();

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (plugin.getStaffManager().isStaff(staff)) {
                staff.sendMessage("§c§l[REPORT] §e" + reporterName + " §freported §e" + reportedName);
            }
        }

        plugin.getDiscordWebhookManager().sendReportNotification(reportedName, reporterName);
    }

    public List<UUID> getReportedPlayers() {
        return new ArrayList<>(reports.keySet());
    }

    public List<Report> getReports(UUID player) {
        return reports.getOrDefault(player, new ArrayList<>());
    }

    public void clearReports(UUID player) {
        reports.remove(player);
    }

    public boolean hasReports(UUID player) {
        return reports.containsKey(player) && !reports.get(player).isEmpty();
    }

    public static class Report {
        private final UUID reporter;
        private final String reason;
        private final long timestamp;

        public Report(UUID reporter, String reason, long timestamp) {
            this.reporter = reporter;
            this.reason = reason;
            this.timestamp = timestamp;
        }

        public UUID getReporter() {
            return reporter;
        }

        public String getReason() {
            return reason;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}

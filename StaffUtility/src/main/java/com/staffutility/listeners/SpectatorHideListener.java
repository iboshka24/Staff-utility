package com.staffutility.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.staffutility.StaffUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpectatorHideListener extends PacketListenerAbstract {

    private final StaffUtility plugin;

    public SpectatorHideListener(StaffUtility plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
            handlePlayerInfoUpdate(event);
        } else if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO_REMOVE) {
            handlePlayerInfoRemove(event);
        }
    }

    private void handlePlayerInfoUpdate(PacketSendEvent event) {
        try {
            WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(event);
            Player receiver = (Player) event.getPlayer();

            // If receiver is null or not online, skip processing
            if (receiver == null || !receiver.isOnline()) {
                return;
            }

            boolean isReceiverStaff = plugin.getStaffManager().isStaff(receiver);

            List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> entries = packet.getEntries();
            List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> modifiedEntries = new ArrayList<>();
            boolean modified = false;

            for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry : entries) {
                Player target = plugin.getServer().getPlayer(entry.getGameProfile().getUUID());

                // If target is vanished and receiver is not staff, completely remove them from tab
                if (target != null && plugin.getStaffManager().isVanished(target) && !isReceiverStaff) {
                    // Skip this entry - don't add to modified list (removes from tab)
                    modified = true;
                    continue;
                }

                WrapperPlayServerPlayerInfoUpdate.PlayerInfo newEntry = entry;

                // Hide spectator mode for staff members in staff mode
                if (target != null && target.getGameMode() == org.bukkit.GameMode.SPECTATOR
                    && plugin.getStaffManager().isInStaffMode(target)) {

                    // Create a completely new display name - plain white text, no formatting
                    Component plainName = Component.text()
                        .content(entry.getGameProfile().getName())
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                        .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                        .decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                        .decoration(TextDecoration.UNDERLINED, TextDecoration.State.FALSE)
                        .decoration(TextDecoration.OBFUSCATED, TextDecoration.State.FALSE)
                        .build();

                    newEntry = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                        entry.getGameProfile(),
                        true, // listed = true (show in tab)
                        entry.getLatency(),
                        GameMode.SURVIVAL, // Show as survival mode instead of spectator
                        plainName,
                        entry.getChatSession()
                    );
                    modified = true;
                }

                modifiedEntries.add(newEntry);
            }

            if (modified) {
                packet.setEntries(modifiedEntries);
                event.markForReEncode(true);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error processing player info update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePlayerInfoRemove(PacketSendEvent event) {
        try {
            WrapperPlayServerPlayerInfoRemove packet = new WrapperPlayServerPlayerInfoRemove(event);
            Player receiver = (Player) event.getPlayer();

            if (receiver == null || !receiver.isOnline()) {
                return;
            }

            List<UUID> uuids = packet.getProfileIds();
            List<UUID> modifiedUuids = new ArrayList<>();
            boolean modified = false;

            for (UUID uuid : uuids) {
                Player target = plugin.getServer().getPlayer(uuid);

                // If target is vanished and receiver is not staff, allow removal
                if (target != null && plugin.getStaffManager().isVanished(target)
                    && !plugin.getStaffManager().isStaff(receiver)) {
                    modifiedUuids.add(uuid);
                    continue;
                }

                modifiedUuids.add(uuid);
            }

            if (modified) {
                packet.setProfileIds(modifiedUuids);
                event.markForReEncode(true);
            }

        } catch (Exception e) {
            // Silently ignore
        }
    }
}

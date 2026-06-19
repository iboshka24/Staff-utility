package com.staffutility.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.staffutility.StaffUtility;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Chat moderation: blocks profanity, slurs and racism (multi-language) and spam, hides the
 * offending message from chat, and flags the player in {@code /sus} via the SusManager. Heavier
 * categories (slurs/racism) add a higher violation level.
 *
 * <p>Runs at {@link EventPriority#LOWEST} so the message is cancelled before the chat formatter
 * renders it. Matching is root-based on a normalised form of the message (lower-cased, leetspeak
 * folded, separators stripped) so simple evasions like {@code f.u.c.k} or {@code х у й} are caught.</p>
 */
public class ProfanityFilter implements Listener {

    private final StaffUtility plugin;

    // Profanity / insults — flag + block (lower VL).
    private static final List<String> PROFANITY = Arrays.asList(
            // English
            "fuck", "shit", "bitch", "asshole", "dick", "pussy", "bastard", "motherfucker",
            "cock", "wank", "slut", "whore", "douche", "jackass",
            // Russian (roots)
            "хуй", "хуя", "хуе", "пизд", "бля", "ебан", "ебал", "ебат", "ебуч", "выеб", "заеб",
            "наеб", "поеб", "уеб", "сук", "сука", "гандон", "гондон", "мудак", "мудил", "долбоеб",
            "далбаеб", "хер", "херн", "залуп", "дроч", "очк", "говн", "сран", "чмо",
            // Spanish / common
            "puta", "mierda", "joder", "cabron", "coño", "pendejo",
            // German / French
            "scheisse", "arschloch", "merde", "putain", "connard"
    );

    // Slurs / racism / hate — flag + block (higher VL).
    private static final List<String> SLURS = Arrays.asList(
            "nigger", "nigga", "negro", "faggot", "fag", "retard", "kike", "chink", "spic",
            "tranny", "coon", "wetback", "raghead",
            // Russian slurs
            "пидор", "пидар", "педик", "хохол", "жид", "ниггер", "негр", "даун", "дебил",
            "пидорас", "пидрила"
    );

    // Leetspeak folding so "f4gg0t" / "х у й" still match.
    private static final Map<Character, Character> LEET = new HashMap<>();
    static {
        LEET.put('0', 'o');
        LEET.put('1', 'i');
        LEET.put('3', 'e');
        LEET.put('4', 'a');
        LEET.put('5', 's');
        LEET.put('7', 't');
        LEET.put('@', 'a');
        LEET.put('$', 's');
    }

    // Simple anti-spam: last message + timestamp per player.
    private final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();

    public ProfanityFilter(StaffUtility plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("staffutility.chat.bypass")) {
            return;
        }
        String raw = PlainTextComponentSerializer.plainText().serialize(event.message());
        String normalized = normalize(raw);

        // Slurs / racism → block + high VL.
        if (containsAny(normalized, SLURS)) {
            event.setCancelled(true);
            flag(player, "Racism / Hate Speech in chat", 5.0);
            player.sendMessage("§c§lYour message was blocked. §7Hate speech is not tolerated.");
            return;
        }
        // Profanity → block + moderate VL.
        if (containsAny(normalized, PROFANITY)) {
            event.setCancelled(true);
            flag(player, "Excessive profanity in chat", 2.0);
            player.sendMessage("§cYour message was blocked for inappropriate language.");
            return;
        }
        // Spam: identical message within 2s, or very fast repeats.
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        String prev = lastMessage.get(id);
        Long prevTime = lastMessageTime.get(id);
        lastMessage.put(id, normalized);
        lastMessageTime.put(id, now);
        if (prev != null && prevTime != null && (now - prevTime) < 2000L && prev.equals(normalized)) {
            event.setCancelled(true);
            flag(player, "Chat spam", 1.0);
            player.sendMessage("§cPlease don't spam the chat.");
        }
    }

    private void flag(Player player, String reason, double vl) {
        try {
            // SusManager API touches Bukkit collections; hop to the player's region thread.
            player.getScheduler().run(plugin, task ->
                    plugin.getSusManager().addFlag(player, reason, vl), null);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to flag " + player.getName() + ": " + t.getMessage());
        }
    }

    private boolean containsAny(String normalized, List<String> words) {
        for (String w : words) {
            if (normalized.contains(w)) {
                return true;
            }
        }
        return false;
    }

    /** Lower-case, fold leetspeak, and strip non-letter separators so spaced/obfuscated text matches. */
    private String normalize(String input) {
        String lower = input.toLowerCase();
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            Character folded = LEET.get(c);
            if (folded != null) {
                sb.append(folded);
            } else if (Character.isLetter(c)) {
                sb.append(c);
            }
            // separators (spaces, dots, etc.) are dropped
        }
        return sb.toString();
    }
}

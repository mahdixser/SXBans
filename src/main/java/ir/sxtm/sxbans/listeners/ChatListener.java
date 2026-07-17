package ir.sxtm.sxbans.listeners;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {
    private final SXBans plugin;

    public ChatListener(SXBans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String ip = player.getAddress().getAddress().getHostAddress();

        // Check for active mutes
        if (plugin.getPunishmentManager().isPlayerMuted(uuid)) {
            Punishment mute = plugin.getPunishmentManager().getActiveMute(uuid);
            if (mute != null) {
                player.sendMessage(plugin.getMessagesManager().getMuteMessage(mute));
                event.setCancelled(true);
                return;
            }
        }

        // Check for IP mutes
        if (plugin.getPunishmentManager().isIpMuted(ip)) {
            player.sendMessage(plugin.getMessagesManager().getColoredMessage("punishment.ipmute.message",
                    Map.of("reason", "Your IP address has been muted")));
            event.setCancelled(true);
            return;
        }

        // Check for muted by name (wildcard)
        if (plugin.getConfigManager().getBoolean("mute.wildcard-enabled", false)) {
            String mutedName = plugin.getConfigManager().getString("mute.wildcard-name");
            if (mutedName != null && player.getName().toLowerCase().contains(mutedName.toLowerCase())) {
                player.sendMessage(plugin.getMessagesManager().getColoredMessage("punishment.mute.wildcard"));
                event.setCancelled(true);
                return;
            }
        }

        // Check for muted words
        if (plugin.getConfigManager().getBoolean("mute.filter-enabled", true)) {
            String message = event.getMessage();
            String[] mutedWords = plugin.getConfigManager().getString("mute.filter-words", "").split(",");
            for (String word : mutedWords) {
                if (message.toLowerCase().contains(word.toLowerCase().trim())) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getMessagesManager().getColoredMessage("punishment.mute.filter"));

                    // Log the mute
                    plugin.getLogger().info("Player " + player.getName() + " was muted for using filtered word");
                    return;
                }
            }
        }
    }
}
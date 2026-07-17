package ir.sxtm.sxbans.listeners;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

public class ConnectionListener implements Listener {
    private final SXBans plugin;

    public ConnectionListener(SXBans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String ip = event.getAddress().getHostAddress();
        String playerName = event.getPlayer().getName();

        // Check for active bans
        if (plugin.getPunishmentManager().isPlayerBanned(uuid)) {
            Punishment ban = plugin.getPunishmentManager().getActiveBan(uuid);
            if (ban != null) {
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                        plugin.getMessagesManager().getBanMessage(ban));
                return;
            }
        }

        // Check for IP bans
        if (plugin.getPunishmentManager().isIpBanned(ip)) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                    plugin.getMessagesManager().getColoredMessage("punishment.ipban.message",
                            Map.of("reason", "Your IP address has been banned")));
            return;
        }

        // Check for blacklisted players
        if (plugin.getConfigManager().getBoolean("blacklist.enabled", true)) {
            String blacklistFile = plugin.getConfigManager().getString("blacklist.file", "blacklist.txt");
            // Check if player is in blacklist
            // This would require file reading implementation
        }

        // Check for connection limit per IP
        int maxConnections = plugin.getConfigManager().getInt("connection.max-per-ip", 3);
        int currentConnections = (int) plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.getAddress() != null &&
                        p.getAddress().getAddress().getHostAddress().equals(ip))
                .count();

        if (currentConnections >= maxConnections) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    plugin.getMessagesManager().getColoredMessage("connection.limit",
                            Map.of("max", String.valueOf(maxConnections))));
        }
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        // Add custom MOTD for banned players (optional)
        // This would require checking if the player is banned
        InetAddress address = event.getAddress();
        if (address != null) {
            String ip = address.getHostAddress();
            if (plugin.getPunishmentManager().isIpBanned(ip)) {
                // Could add a special MOTD for banned IPs
            }
        }
    }
}
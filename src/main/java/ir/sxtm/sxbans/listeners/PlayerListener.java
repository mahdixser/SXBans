package ir.sxtm.sxbans.listeners;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import ir.sxtm.sxbans.punishment.AltAccountDetector;
import ir.sxtm.sxbans.punishment.IPTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final SXBans plugin;
    private final IPTracker ipTracker;
    private final AltAccountDetector altDetector;

    public PlayerListener(SXBans plugin) {
        this.plugin = plugin;
        this.ipTracker = plugin.getIPTracker();
        this.altDetector = plugin.getAltAccountDetector();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String ip = player.getAddress().getAddress().getHostAddress();

        // Track IP
        ipTracker.trackIP(ip, uuid, player.getName());

        // Check for active bans
        if (plugin.getPunishmentManager().isPlayerBanned(uuid)) {
            Punishment ban = plugin.getPunishmentManager().getActiveBan(uuid);
            if (ban != null) {
                player.kickPlayer(plugin.getMessagesManager().getBanMessage(ban));
                return;
            }
        }

        // Check for IP bans
        if (plugin.getPunishmentManager().isIpBanned(ip)) {
            player.kickPlayer(plugin.getMessagesManager().getColoredMessage("punishment.ipban.message",
                    Map.of("reason", "Your IP address has been banned")));
            return;
        }

        // Check for alt accounts
        if (plugin.getConfigManager().getBoolean("alt-detection.enabled", true)) {
            Set<UUID> alts = altDetector.detectAlts(player);
            if (!alts.isEmpty()) {
                // Notify staff
                String message = plugin.getMessagesManager().getColoredMessage("alt-detection.found",
                        Map.of("player", player.getName(), "count", String.valueOf(alts.size())));

                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("sxbans.alts.notify"))
                        .forEach(p -> p.sendMessage(message));
            }
        }

        // Check for pending scheduled punishments
        if (plugin.getScheduledPunishmentManager() != null) {
            var scheduled = plugin.getScheduledPunishmentManager()
                    .getScheduledPunishmentsForPlayer(uuid);
            for (var sp : scheduled) {
                if (sp.getScheduledTime() <= System.currentTimeMillis()) {
                    // Execute immediately
                    plugin.getScheduledPunishmentManager().executeScheduledPunishment(sp);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Update last seen in IP data
        String ip = player.getAddress().getAddress().getHostAddress();
        var ipData = ipTracker.getIPData(ip);
        if (ipData != null) {
            ipData.setLastSeen(System.currentTimeMillis());
            plugin.getPunishmentStorage().saveIPData(ipData);
        }
    }
}
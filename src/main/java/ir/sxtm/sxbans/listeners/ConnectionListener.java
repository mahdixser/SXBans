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
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionListener implements Listener {
    private final SXBans plugin;

    private final Map<UUID, Long> loginAttemptCache;
    private static final long CACHE_DURATION = 10000;

    public ConnectionListener(SXBans plugin) {
        this.plugin = plugin;
        this.loginAttemptCache = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String ip = event.getAddress().getHostAddress();
        String playerName = event.getPlayer().getName();

        if (plugin.getServer().getBanList(org.bukkit.BanList.Type.IP).isBanned(ip)) {
            String kickMessage = plugin.getMessagesManager().getIpBanMessage(null);
            if (kickMessage == null || kickMessage.isEmpty()) {
                kickMessage = "&cYour IP address has been banned from this server!";
            }

            if (!isRecentlyLogged(uuid)) {
                plugin.getSXBansLogger().info("Blocked login attempt from server-banned IP: " + ip + " (" + playerName + ")");
                loginAttemptCache.put(uuid, System.currentTimeMillis());
            }

            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, plugin.getMessagesManager().colorize(kickMessage));
            return;
        }

        if (plugin.getPunishmentManager().isIpBanned(ip)) {

            Punishment ipBan = null;
            for (Punishment p : plugin.getPunishmentManager().getAllActivePunishments()) {
                if (p.getType() == Punishment.PunishmentType.IP_BAN &&
                        ip.equals(p.getIpAddress())) {
                    ipBan = p;
                    break;
                }
            }

            String kickMessage;
            if (ipBan != null) {
                kickMessage = plugin.getMessagesManager().getIpBanMessage(ipBan);
            } else {
                kickMessage = plugin.getMessagesManager().getIpBanMessage(null);
            }

            if (kickMessage == null || kickMessage.isEmpty()) {
                kickMessage = "&cYour IP address has been banned from this server!";
            }

            if (!isRecentlyLogged(uuid)) {
                plugin.getSXBansLogger().info("Blocked login attempt from plugin-banned IP: " + ip + " (" + playerName + ")");
                loginAttemptCache.put(uuid, System.currentTimeMillis());
            }

            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, plugin.getMessagesManager().colorize(kickMessage));
            return;
        }

        if (plugin.getPunishmentManager().isPlayerBanned(uuid)) {
            Punishment ban = plugin.getPunishmentManager().getActiveBan(uuid);
            if (ban != null) {
                String kickMessage;
                if (ban.getType() == Punishment.PunishmentType.TEMP_BAN) {
                    kickMessage = plugin.getMessagesManager().getTempBanMessage(ban);
                } else {
                    kickMessage = plugin.getMessagesManager().getBanMessage(ban);
                }
                if (kickMessage == null || kickMessage.isEmpty()) {
                    kickMessage = "&cYou are banned from this server!\n&7Reason: " + ban.getReason();
                }

                if (!isRecentlyLogged(uuid)) {
                    plugin.getSXBansLogger().info("Blocked login attempt from banned player: " + playerName + " (" + ip + ")");
                    loginAttemptCache.put(uuid, System.currentTimeMillis());
                }

                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, plugin.getMessagesManager().colorize(kickMessage));
                return;
            }
        }

        if (plugin.getConfigManager().getBoolean("blacklist.enabled", true)) {

        }

        int maxConnections = plugin.getConfigManager().getInt("connection.max-per-ip", 3);
        long currentConnections = plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.getAddress() != null &&
                        p.getAddress().getAddress().getHostAddress().equals(ip))
                .count();

        if (currentConnections >= maxConnections) {
            String kickMsg = plugin.getMessagesManager().getColoredMessage("connection.limit",
                    Map.of("max", String.valueOf(maxConnections)));
            if (kickMsg == null || kickMsg.isEmpty()) {
                kickMsg = "&cToo many connections from your IP address! (Max: " + maxConnections + ")";
            }

            loginAttemptCache.put(uuid, System.currentTimeMillis());
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, plugin.getMessagesManager().colorize(kickMsg));
        }
    }

    private boolean isRecentlyLogged(UUID uuid) {
        Long lastAttempt = loginAttemptCache.get(uuid);
        if (lastAttempt == null) {
            return false;
        }
        return (System.currentTimeMillis() - lastAttempt) < CACHE_DURATION;
    }

    public void cleanupCache() {
        long now = System.currentTimeMillis();
        loginAttemptCache.entrySet().removeIf(entry ->
                (now - entry.getValue()) > CACHE_DURATION * 2
        );
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        InetAddress address = event.getAddress();
        if (address != null) {
            String ip = address.getHostAddress();
            if (plugin.getPunishmentManager().isIpBanned(ip)) {

            }
        }
    }
}
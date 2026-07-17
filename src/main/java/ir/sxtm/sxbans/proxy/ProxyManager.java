package ir.sxtm.sxbans.proxy;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manager for proxy communication.
 * Handles communication between multiple servers in a network.
 */
public class ProxyManager {
    private final SXBans plugin;
    private RedisProxy redisProxy;
    private BungeeProxy bungeeProxy;
    private boolean initialized;

    public ProxyManager(SXBans plugin) {
        this.plugin = plugin;
        this.initialized = false;
    }

    /**
     * Initialize proxy communication.
     */
    public void initialize() {
        // Initialize Redis proxy
        if (plugin.getConfigManager().isRedisEnabled()) {
            redisProxy = new RedisProxy(plugin);
            redisProxy.initialize();
            plugin.getSXBansLogger().info("Redis proxy initialized");
        }

        // Initialize Bungee proxy (if running on BungeeCord)
        if (Bukkit.getServer().getName().equals("BungeeCord") ||
                Bukkit.getServer().getName().equals("Waterfall")) {
            bungeeProxy = new BungeeProxy(plugin);
            bungeeProxy.initialize();
            plugin.getSXBansLogger().info("Bungee proxy initialized");
        }

        initialized = true;
    }

    /**
     * Check if proxy is initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Check if Redis proxy is enabled.
     *
     * @return true if enabled
     */
    public boolean isRedisEnabled() {
        return redisProxy != null && redisProxy.isEnabled();
    }

    /**
     * Check if Bungee proxy is enabled.
     *
     * @return true if enabled
     */
    public boolean isBungeeEnabled() {
        return bungeeProxy != null && bungeeProxy.isEnabled();
    }

    /**
     * Broadcast a punishment across the network.
     *
     * @param punishment The punishment
     */
    public void broadcastPunishment(Punishment punishment) {
        if (isRedisEnabled()) {
            redisProxy.publishPunishment(punishment);
        }

        if (isBungeeEnabled()) {
            bungeeProxy.sendPunishment(punishment);
        }
    }

    /**
     * Broadcast a punishment removal across the network.
     *
     * @param punishment The punishment
     */
    public void broadcastPunishmentRemoval(Punishment punishment) {
        if (isRedisEnabled()) {
            redisProxy.publishPunishmentRemoval(punishment);
        }

        if (isBungeeEnabled()) {
            bungeeProxy.sendPunishmentRemoval(punishment);
        }
    }

    /**
     * Broadcast a ban wave across the network.
     *
     * @param waveId The wave ID
     * @param playerName The player name
     * @param executor The executor name
     */
    public void broadcastBanWave(String waveId, String playerName, String executor) {
        if (isRedisEnabled()) {
            redisProxy.publishBanWave(waveId, playerName, executor);
        }

        if (isBungeeEnabled()) {
            bungeeProxy.sendBanWave(waveId, playerName, executor);
        }
    }

    /**
     * Get player information from the network.
     *
     * @param playerName The player name
     * @return CompletableFuture with player info
     */
    public CompletableFuture<ProxyPlayerInfo> getPlayerInfo(String playerName) {
        if (isRedisEnabled()) {
            return redisProxy.getPlayerInfo(playerName);
        }

        if (isBungeeEnabled()) {
            return bungeeProxy.getPlayerInfo(playerName);
        }

        // Local fallback
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            ProxyPlayerInfo info = new ProxyPlayerInfo();
            info.setName(player.getName());
            info.setUuid(player.getUniqueId());
            // Fix: استفاده از getName() به جای getServerName()
            info.setServer(Bukkit.getServer().getName());
            info.setOnline(true);
            return CompletableFuture.completedFuture(info);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get all players on the network.
     *
     * @return CompletableFuture with list of players
     */
    public CompletableFuture<java.util.List<ProxyPlayerInfo>> getNetworkPlayers() {
        if (isRedisEnabled()) {
            return redisProxy.getNetworkPlayers();
        }

        if (isBungeeEnabled()) {
            return bungeeProxy.getNetworkPlayers();
        }

        // Local fallback
        java.util.List<ProxyPlayerInfo> players = new java.util.ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            ProxyPlayerInfo info = new ProxyPlayerInfo();
            info.setName(player.getName());
            info.setUuid(player.getUniqueId());
            // Fix: استفاده از getName() به جای getServerName()
            info.setServer(Bukkit.getServer().getName());
            info.setOnline(true);
            players.add(info);
        }
        return CompletableFuture.completedFuture(players);
    }

    /**
     * Kick a player from the network.
     *
     * @param playerName The player name
     * @param reason The reason
     */
    public void kickPlayerNetwork(String playerName, String reason) {
        if (isBungeeEnabled()) {
            bungeeProxy.kickPlayer(playerName, reason);
        } else {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.kickPlayer(reason);
            }
        }
    }

    /**
     * Get network status.
     *
     * @return Map of status information
     */
    public java.util.Map<String, Object> getNetworkStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();

        status.put("redis", isRedisEnabled());
        status.put("bungee", isBungeeEnabled());

        if (isRedisEnabled()) {
            status.put("redis_status", redisProxy.getStatus());
        }

        if (isBungeeEnabled()) {
            status.put("bungee_status", bungeeProxy.getStatus());
        }

        return status;
    }

    /**
     * Shutdown proxy connections.
     */
    public void shutdown() {
        if (redisProxy != null) {
            redisProxy.shutdown();
        }

        if (bungeeProxy != null) {
            bungeeProxy.shutdown();
        }

        initialized = false;
    }
}
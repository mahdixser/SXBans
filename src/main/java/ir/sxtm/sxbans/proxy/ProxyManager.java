package ir.sxtm.sxbans.proxy;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProxyManager {
    private final SXBans plugin;
    private RedisProxy redisProxy;
    private BungeeProxy bungeeProxy;
    private boolean initialized;

    public ProxyManager(SXBans plugin) {
        this.plugin = plugin;
        this.initialized = false;
    }

    public void initialize() {

        if (plugin.getConfigManager().isRedisEnabled()) {
            redisProxy = new RedisProxy(plugin);
            redisProxy.initialize();
            plugin.getSXBansLogger().info("Redis proxy initialized (server-name: " +
                    plugin.getConfigManager().getServerName() + ")");
        }

        if (plugin.getConfigManager().isBungeeMessagingEnabled()) {
            bungeeProxy = new BungeeProxy(plugin);
            bungeeProxy.initialize();
            plugin.getSXBansLogger().info("Bungee messaging proxy initialized");
        }

        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isRedisEnabled() {
        return redisProxy != null && redisProxy.isEnabled();
    }

    public boolean isBungeeEnabled() {
        return bungeeProxy != null && bungeeProxy.isEnabled();
    }

    public void broadcastPunishment(Punishment punishment) {
        if (isRedisEnabled()) {
            redisProxy.publishPunishment(punishment);
        }

        if (isBungeeEnabled()) {
            bungeeProxy.sendPunishment(punishment);
        }
    }

    public void broadcastPunishmentRemoval(Punishment punishment) {
        if (isRedisEnabled()) {
            redisProxy.publishPunishmentRemoval(punishment);
        }

        if (isBungeeEnabled()) {
            bungeeProxy.sendPunishmentRemoval(punishment);
        }
    }

    public void broadcastBanWave(String waveId, String playerName, String executor) {
        if (isRedisEnabled()) {
            redisProxy.publishBanWave(waveId, playerName, executor);
        }

        if (isBungeeEnabled()) {
            bungeeProxy.sendBanWave(waveId, playerName, executor);
        }
    }

    public CompletableFuture<ProxyPlayerInfo> getPlayerInfo(String playerName) {
        if (isRedisEnabled()) {
            return redisProxy.getPlayerInfo(playerName);
        }

        if (isBungeeEnabled()) {
            return bungeeProxy.getPlayerInfo(playerName);
        }

        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            ProxyPlayerInfo info = new ProxyPlayerInfo();
            info.setName(player.getName());
            info.setUuid(player.getUniqueId());

            info.setServer(plugin.getConfigManager().getServerName());
            info.setOnline(true);
            return CompletableFuture.completedFuture(info);
        }

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<java.util.List<ProxyPlayerInfo>> getNetworkPlayers() {
        if (isRedisEnabled()) {
            return redisProxy.getNetworkPlayers();
        }

        if (isBungeeEnabled()) {
            return bungeeProxy.getNetworkPlayers();
        }

        java.util.List<ProxyPlayerInfo> players = new java.util.ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            ProxyPlayerInfo info = new ProxyPlayerInfo();
            info.setName(player.getName());
            info.setUuid(player.getUniqueId());

            info.setServer(plugin.getConfigManager().getServerName());
            info.setOnline(true);
            players.add(info);
        }
        return CompletableFuture.completedFuture(players);
    }

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
package ir.sxtm.sxbans.proxy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.database.RedisManager;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-based proxy for cross-server communication.
 */
public class RedisProxy {
    private final SXBans plugin;
    private final RedisManager redisManager;
    private final Gson gson;
    private final Map<String, Long> lastSync;
    private boolean enabled;
    private JedisPubSub subscriber;

    public RedisProxy(SXBans plugin) {
        this.plugin = plugin;
        this.redisManager = plugin.getRedisManager();
        this.gson = new Gson();
        this.lastSync = new ConcurrentHashMap<>();
        this.enabled = false;
    }

    /**
     * Initialize Redis proxy.
     */
    public void initialize() {
        if (redisManager == null || !redisManager.isEnabled()) {
            plugin.getSXBansLogger().warning("Redis is not enabled, proxy disabled");
            return;
        }

        enabled = true;

        // Start subscriber
        startSubscriber();

        // Sync data
        syncData();

        plugin.getSXBansLogger().info("Redis proxy initialized");
    }

    /**
     * Start Redis subscriber for real-time updates.
     */
    private void startSubscriber() {
        if (!enabled) return;

        // Run in separate thread to avoid blocking
        new Thread(() -> {
            try (Jedis jedis = redisManager.getJedis()) {
                if (jedis == null) return;

                subscriber = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        handleMessage(channel, message);
                    }
                };

                jedis.subscribe(subscriber, "sxbans:punishments", "sxbans:banwave", "sxbans:sync");

            } catch (Exception e) {
                plugin.getSXBansLogger().warning("Redis subscriber error: " + e.getMessage());
                // Attempt to reconnect
                if (enabled) {
                    try {
                        Thread.sleep(5000);
                        startSubscriber();
                    } catch (InterruptedException ignored) {}
                }
            }
        }, "Redis-Subscriber").start();
    }

    /**
     * Handle incoming Redis messages.
     */
    private void handleMessage(String channel, String message) {
        try {
            JsonObject data = gson.fromJson(message, JsonObject.class);

            switch (channel) {
                case "sxbans:punishments":
                    handlePunishmentMessage(data);
                    break;
                case "sxbans:banwave":
                    handleBanWaveMessage(data);
                    break;
                case "sxbans:sync":
                    handleSyncMessage(data);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to handle Redis message: " + e.getMessage());
        }
    }

    /**
     * Handle punishment message from Redis.
     */
    private void handlePunishmentMessage(JsonObject data) {
        String action = data.get("action").getAsString();
        String punishmentJson = data.get("punishment").getAsString();

        try {
            Punishment punishment = gson.fromJson(punishmentJson, Punishment.class);

            if ("apply".equals(action)) {
                // Apply punishment on this server
                plugin.getPunishmentManager().applyPunishment(
                        punishment.getPlayerUUID(),
                        punishment.getPlayerName(),
                        punishment.getType(),
                        punishment.getReason(),
                        punishment.getDuration(),
                        punishment.getExecutorUUID(),
                        punishment.getExecutorName()
                );
            } else if ("remove".equals(action)) {
                // Remove punishment on this server
                plugin.getPunishmentManager().removePunishment(
                        punishment.getId(),
                        punishment.getRemoverUUID(),
                        punishment.getRemoverName(),
                        punishment.getRemoveReason()
                );
            }

            // Update cache
            lastSync.put("punishment_" + punishment.getId().toString(), System.currentTimeMillis());

        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to handle punishment message: " + e.getMessage());
        }
    }

    /**
     * Handle ban wave message from Redis.
     */
    private void handleBanWaveMessage(JsonObject data) {
        String waveId = data.get("waveId").getAsString();
        String playerName = data.get("player").getAsString();
        String executor = data.get("executor").getAsString();

        // Log ban wave on this server
        plugin.getSXBansLogger().info("Ban wave [" + waveId + "] affected: " + playerName + " by " + executor);

        // Could trigger local actions if needed
    }

    /**
     * Handle sync message from Redis.
     */
    private void handleSyncMessage(JsonObject data) {
        String type = data.get("type").getAsString();

        if ("request".equals(type)) {
            // Send data to requesting server
            sendSyncData(data.get("server").getAsString());
        } else if ("response".equals(type)) {
            // Handle sync response
            handleSyncResponse(data);
        }
    }

    /**
     * Send sync data to another server.
     */
    private void sendSyncData(String targetServer) {
        if (!enabled) return;

        try {
            List<Punishment> punishments = plugin.getPunishmentStorage().getAllPunishments();

            JsonObject data = new JsonObject();
            data.addProperty("type", "response");
            // Fix: استفاده از getName() به جای getServerName()
            data.addProperty("server", Bukkit.getServer().getName());
            data.add("punishments", gson.toJsonTree(punishments));

            try (Jedis jedis = redisManager.getJedis()) {
                if (jedis != null) {
                    jedis.publish("sxbans:sync:" + targetServer, gson.toJson(data));
                }
            }

        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to send sync data: " + e.getMessage());
        }
    }

    /**
     * Handle sync response.
     */
    private void handleSyncResponse(JsonObject data) {
        try {
            List<Punishment> punishments = new ArrayList<>();
            punishments = gson.fromJson(data.get("punishments"),
                    new com.google.gson.reflect.TypeToken<List<Punishment>>(){}.getType());

            // Merge punishments
            for (Punishment p : punishments) {
                // Fix: استفاده از getPunishment و بررسی null به جای isPresent
                Punishment existing = plugin.getPunishmentStorage().getPunishment(p.getId());
                if (existing == null) {
                    plugin.getPunishmentStorage().savePunishment(p);
                    // Fix: استفاده از متد عمومی addPunishmentToCache
                    plugin.getPunishmentManager().addPunishmentToCache(p);
                }
            }

            lastSync.put("sync_" + data.get("server").getAsString(), System.currentTimeMillis());

        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to handle sync response: " + e.getMessage());
        }
    }

    /**
     * Sync data with all servers.
     */
    private void syncData() {
        if (!enabled) return;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("type", "request");
            // Fix: استفاده از getName() به جای getServerName()
            data.addProperty("server", Bukkit.getServer().getName());

            try (Jedis jedis = redisManager.getJedis()) {
                if (jedis != null) {
                    jedis.publish("sxbans:sync", gson.toJson(data));
                }
            }

        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to sync data: " + e.getMessage());
        }
    }

    /**
     * Publish a punishment to Redis.
     *
     * @param punishment The punishment
     */
    public void publishPunishment(Punishment punishment) {
        if (!enabled) return;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("action", "apply");
            data.add("punishment", gson.toJsonTree(punishment));

            try (Jedis jedis = redisManager.getJedis()) {
                if (jedis != null) {
                    jedis.publish("sxbans:punishments", gson.toJson(data));
                }
            }

        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to publish punishment: " + e.getMessage());
        }
    }

    /**
     * Publish a punishment removal to Redis.
     *
     * @param punishment The punishment
     */
    public void publishPunishmentRemoval(Punishment punishment) {
        if (!enabled) return;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("action", "remove");
            data.add("punishment", gson.toJsonTree(punishment));

            try (Jedis jedis = redisManager.getJedis()) {
                if (jedis != null) {
                    jedis.publish("sxbans:punishments", gson.toJson(data));
                }
            }

        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to publish punishment removal: " + e.getMessage());
        }
    }

    /**
     * Publish a ban wave to Redis.
     *
     * @param waveId The wave ID
     * @param playerName The player name
     * @param executor The executor name
     */
    public void publishBanWave(String waveId, String playerName, String executor) {
        if (!enabled) return;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("waveId", waveId);
            data.addProperty("player", playerName);
            data.addProperty("executor", executor);
            data.addProperty("timestamp", System.currentTimeMillis());

            try (Jedis jedis = redisManager.getJedis()) {
                if (jedis != null) {
                    jedis.publish("sxbans:banwave", gson.toJson(data));
                }
            }

        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to publish ban wave: " + e.getMessage());
        }
    }

    /**
     * Get player info from Redis cache.
     *
     * @param playerName The player name
     * @return CompletableFuture with player info
     */
    public CompletableFuture<ProxyPlayerInfo> getPlayerInfo(String playerName) {
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = redisManager.getJedis()) {
                if (jedis == null) return null;

                String key = "sxbans:player:" + playerName.toLowerCase();
                String data = jedis.get(key);
                if (data == null) return null;

                return gson.fromJson(data, ProxyPlayerInfo.class);

            } catch (Exception e) {
                plugin.getSXBansLogger().warning("Failed to get player info: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Get all network players from Redis.
     *
     * @return CompletableFuture with list of players
     */
    public CompletableFuture<List<ProxyPlayerInfo>> getNetworkPlayers() {
        if (!enabled) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        return CompletableFuture.supplyAsync(() -> {
            List<ProxyPlayerInfo> players = new ArrayList<>();
            try (Jedis jedis = redisManager.getJedis()) {
                if (jedis == null) return players;

                Set<String> keys = jedis.keys("sxbans:player:*");
                for (String key : keys) {
                    String data = jedis.get(key);
                    if (data != null) {
                        players.add(gson.fromJson(data, ProxyPlayerInfo.class));
                    }
                }
                return players;

            } catch (Exception e) {
                plugin.getSXBansLogger().warning("Failed to get network players: " + e.getMessage());
                return players;
            }
        });
    }

    /**
     * Check if Redis proxy is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get status information.
     *
     * @return Status map
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", enabled);
        status.put("last_sync", lastSync);
        if (redisManager != null) {
            status.put("redis_connected", redisManager.isEnabled());
        }
        return status;
    }

    /**
     * Shutdown Redis proxy.
     */
    public void shutdown() {
        enabled = false;
        if (subscriber != null) {
            try {
                subscriber.unsubscribe();
            } catch (Exception ignored) {}
        }
    }
}
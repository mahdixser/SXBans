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

    public void initialize() {
        if (redisManager == null || !redisManager.isEnabled()) {
            plugin.getSXBansLogger().warning("Redis is not enabled, proxy disabled");
            return;
        }

        enabled = true;

        startSubscriber();

        syncData();

        plugin.getSXBansLogger().info("Redis proxy initialized");
    }

    private void startSubscriber() {
        if (!enabled) return;

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

                if (enabled) {
                    try {
                        Thread.sleep(5000);
                        startSubscriber();
                    } catch (InterruptedException ignored) {}
                }
            }
        }, "Redis-Subscriber").start();
    }

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

    private void handlePunishmentMessage(JsonObject data) {
        String action = data.get("action").getAsString();
        String punishmentJson = data.get("punishment").getAsString();

        try {
            Punishment punishment = gson.fromJson(punishmentJson, Punishment.class);

            if (plugin.getConfigManager().getServerName().equals(punishment.getServerName())) {
                return;
            }

            if ("apply".equals(action)) {

                plugin.getPunishmentManager().applyPunishmentFromNetwork(
                        punishment.getPlayerUUID(),
                        punishment.getPlayerName(),
                        punishment.getType(),
                        punishment.getReason(),
                        punishment.getDuration(),
                        punishment.getExecutorUUID(),
                        punishment.getExecutorName(),
                        punishment.getIpAddress()
                );
            } else if ("remove".equals(action)) {

                plugin.getPunishmentManager().removePunishmentFromNetwork(
                        punishment.getId(),
                        punishment.getRemoverUUID(),
                        punishment.getRemoverName(),
                        punishment.getRemoveReason()
                );
            }

            lastSync.put("punishment_" + punishment.getId().toString(), System.currentTimeMillis());

        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to handle punishment message: " + e.getMessage());
        }
    }

    private void handleBanWaveMessage(JsonObject data) {
        String waveId = data.get("waveId").getAsString();
        String playerName = data.get("player").getAsString();
        String executor = data.get("executor").getAsString();

        plugin.getSXBansLogger().info("Ban wave [" + waveId + "] affected: " + playerName + " by " + executor);

    }

    private void handleSyncMessage(JsonObject data) {
        String type = data.get("type").getAsString();

        if ("request".equals(type)) {

            sendSyncData(data.get("server").getAsString());
        } else if ("response".equals(type)) {

            handleSyncResponse(data);
        }
    }

    private void sendSyncData(String targetServer) {
        if (!enabled) return;

        try {
            List<Punishment> punishments = plugin.getPunishmentStorage().getAllPunishments();

            JsonObject data = new JsonObject();
            data.addProperty("type", "response");

            data.addProperty("server", plugin.getConfigManager().getServerName());
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

    private void handleSyncResponse(JsonObject data) {
        try {
            List<Punishment> punishments = new ArrayList<>();
            punishments = gson.fromJson(data.get("punishments"),
                    new com.google.gson.reflect.TypeToken<List<Punishment>>(){}.getType());

            for (Punishment p : punishments) {

                Punishment existing = plugin.getPunishmentStorage().getPunishment(p.getId());
                if (existing == null) {
                    plugin.getPunishmentStorage().savePunishment(p);

                    plugin.getPunishmentManager().addPunishmentToCache(p);
                }
            }

            lastSync.put("sync_" + data.get("server").getAsString(), System.currentTimeMillis());

        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to handle sync response: " + e.getMessage());
        }
    }

    private void syncData() {
        if (!enabled) return;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("type", "request");

            data.addProperty("server", plugin.getConfigManager().getServerName());

            try (Jedis jedis = redisManager.getJedis()) {
                if (jedis != null) {
                    jedis.publish("sxbans:sync", gson.toJson(data));
                }
            }

        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to sync data: " + e.getMessage());
        }
    }

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

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", enabled);
        status.put("last_sync", lastSync);
        if (redisManager != null) {
            status.put("redis_connected", redisManager.isEnabled());
        }
        return status;
    }

    public void shutdown() {
        enabled = false;
        if (subscriber != null) {
            try {
                subscriber.unsubscribe();
            } catch (Exception ignored) {}
        }
    }
}
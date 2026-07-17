package ir.sxtm.sxbans.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RedisManager {
    private final SXBans plugin;
    private JedisPool jedisPool;
    private boolean isEnabled;
    private final ObjectMapper objectMapper;

    public RedisManager(SXBans plugin) {
        this.plugin = plugin;
        this.isEnabled = false;
        this.objectMapper = new ObjectMapper();
    }

    public void initialize() {
        if (!plugin.getConfigManager().isRedisEnabled()) {
            plugin.getSXBansLogger().info("Redis is disabled");
            return;
        }

        try {
            String host = plugin.getConfigManager().getString("redis.host", "localhost");
            int port = plugin.getConfigManager().getInt("redis.port", 6379);
            String password = plugin.getConfigManager().getString("redis.password", "");
            int database = plugin.getConfigManager().getInt("redis.database", 0);

            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);

            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, password, database);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, null, database);
            }

            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                isEnabled = true;
                plugin.getSXBansLogger().info("Redis connected successfully to " + host + ":" + port);
            }

        } catch (Exception e) {
            isEnabled = false;
            plugin.getSXBansLogger().severe("Failed to connect to Redis: " + e.getMessage());
        }
    }

    public void publishPunishment(Punishment punishment) {
        if (!isEnabled) return;

        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                Map<String, String> data = new HashMap<>();
                data.put("id", punishment.getId().toString());
                data.put("player_uuid", punishment.getPlayerUUID().toString());
                data.put("player_name", punishment.getPlayerName());
                data.put("type", punishment.getType().name());
                data.put("reason", punishment.getReason());
                data.put("duration", String.valueOf(punishment.getDuration()));
                data.put("executor_name", punishment.getExecutorName());
                data.put("timestamp", String.valueOf(Instant.now().toEpochMilli()));

                jedis.publish("sxbans:punishments", data.toString());
                plugin.getSXBansLogger().info("Published punishment to Redis");
            } catch (Exception e) {
                plugin.getSXBansLogger().warning("Failed to publish punishment to Redis: " + e.getMessage());
            }
        });
    }

    public void publishBanWave(String playerName, String executor) {
        if (!isEnabled) return;

        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                Map<String, String> data = new HashMap<>();
                data.put("type", "banwave");
                data.put("player", playerName);
                data.put("executor", executor);
                data.put("timestamp", String.valueOf(Instant.now().toEpochMilli()));

                jedis.publish("sxbans:banwave", data.toString());
            } catch (Exception e) {
                plugin.getSXBansLogger().warning("Failed to publish ban wave to Redis: " + e.getMessage());
            }
        });
    }

    public void cachePunishment(String key, Punishment punishment) {
        if (!isEnabled) return;

        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(punishment);
            jedis.setex("sxbans:punishment:" + key, 3600, json);
        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to cache punishment: " + e.getMessage());
        }
    }

    public Punishment getCachedPunishment(String key) {
        if (!isEnabled) return null;

        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("sxbans:punishment:" + key);
            if (json != null) {
                return objectMapper.readValue(json, Punishment.class);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    public void invalidateCache(String key) {
        if (!isEnabled) return;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("sxbans:punishment:" + key);
        } catch (Exception e) {
            // Ignore
        }
    }

    public void set(String key, String value) {
        if (!isEnabled) return;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to set Redis key: " + e.getMessage());
        }
    }

    public void setex(String key, int seconds, String value) {
        if (!isEnabled) return;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, seconds, value);
        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to setex Redis key: " + e.getMessage());
        }
    }

    public String get(String key) {
        if (!isEnabled) return null;

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    public void delete(String key) {
        if (!isEnabled) return;

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            // Ignore
        }
    }

    public Set<String> getKeys(String pattern) {
        if (!isEnabled) return Collections.emptySet();

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.keys(pattern);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            isEnabled = false;
            plugin.getSXBansLogger().info("Redis connection closed");
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Jedis getJedis() {
        if (!isEnabled) return null;
        return jedisPool.getResource();
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
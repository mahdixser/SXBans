package ir.sxtm.sxbans.utils;

import ir.sxtm.sxbans.SXBans;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter for preventing spam.
 */
public class RateLimiter {
    private final SXBans plugin;
    private final Map<String, RateLimit> limits;
    private final long defaultWindow;
    private final int defaultMaxRequests;

    public RateLimiter(SXBans plugin) {
        this.plugin = plugin;
        this.limits = new ConcurrentHashMap<>();
        this.defaultWindow = 60000; // 1 minute
        this.defaultMaxRequests = 5;
    }

    /**
     * Check if a key is rate limited.
     *
     * @param key The key to check
     * @return true if rate limited
     */
    public boolean isRateLimited(String key) {
        return isRateLimited(key, defaultWindow, defaultMaxRequests);
    }

    /**
     * Check if a key is rate limited with custom settings.
     *
     * @param key The key to check
     * @param window The time window in milliseconds
     * @param maxRequests The maximum requests in the window
     * @return true if rate limited
     */
    public boolean isRateLimited(String key, long window, int maxRequests) {
        RateLimit limit = limits.computeIfAbsent(key, k -> new RateLimit());

        long now = System.currentTimeMillis();
        long windowStart = now - window;

        // Clean old requests
        limit.requests.removeIf(time -> time < windowStart);

        if (limit.requests.size() >= maxRequests) {
            return true;
        }

        limit.requests.add(now);
        return false;
    }

    /**
     * Check if a player is rate limited.
     *
     * @param playerName The player name
     * @return true if rate limited
     */
    public boolean isPlayerRateLimited(String playerName) {
        return isRateLimited("player:" + playerName);
    }

    /**
     * Check if a player is rate limited for a specific action.
     *
     * @param playerName The player name
     * @param action The action
     * @return true if rate limited
     */
    public boolean isPlayerRateLimited(String playerName, String action) {
        return isRateLimited("player:" + playerName + ":" + action);
    }

    /**
     * Check if a player is rate limited for a specific action with custom settings.
     *
     * @param playerName The player name
     * @param action The action
     * @param window The time window in milliseconds
     * @param maxRequests The maximum requests in the window
     * @return true if rate limited
     */
    public boolean isPlayerRateLimited(String playerName, String action, long window, int maxRequests) {
        return isRateLimited("player:" + playerName + ":" + action, window, maxRequests);
    }

    /**
     * Check if an IP is rate limited.
     *
     * @param ip The IP address
     * @return true if rate limited
     */
    public boolean isIpRateLimited(String ip) {
        return isRateLimited("ip:" + ip);
    }

    /**
     * Check if an IP is rate limited for a specific action.
     *
     * @param ip The IP address
     * @param action The action
     * @return true if rate limited
     */
    public boolean isIpRateLimited(String ip, String action) {
        return isRateLimited("ip:" + ip + ":" + action);
    }

    /**
     * Reset rate limit for a key.
     *
     * @param key The key
     */
    public void resetRateLimit(String key) {
        limits.remove(key);
    }

    /**
     * Reset rate limit for a player.
     *
     * @param playerName The player name
     */
    public void resetPlayerRateLimit(String playerName) {
        limits.keySet().removeIf(key -> key.startsWith("player:" + playerName));
    }

    /**
     * Reset rate limit for an IP.
     *
     * @param ip The IP address
     */
    public void resetIpRateLimit(String ip) {
        limits.keySet().removeIf(key -> key.startsWith("ip:" + ip));
    }

    /**
     * Clear all rate limits.
     */
    public void clearAll() {
        limits.clear();
    }

    /**
     * Get the number of requests for a key.
     *
     * @param key The key
     * @return Number of requests
     */
    public int getRequestCount(String key) {
        RateLimit limit = limits.get(key);
        if (limit == null) return 0;

        long now = System.currentTimeMillis();
        long windowStart = now - defaultWindow;
        limit.requests.removeIf(time -> time < windowStart);

        return limit.requests.size();
    }

    /**
     * Get rate limit statistics.
     *
     * @return Map of statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalKeys", limits.size());
        stats.put("defaultWindow", defaultWindow);
        stats.put("defaultMaxRequests", defaultMaxRequests);
        return stats;
    }

    private static class RateLimit {
        private final java.util.List<Long> requests;

        private RateLimit() {
            this.requests = new java.util.ArrayList<>();
        }
    }
}
package ir.sxtm.sxbans.utils;

import ir.sxtm.sxbans.SXBans;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private final SXBans plugin;
    private final Map<String, RateLimit> limits;
    private final long defaultWindow;
    private final int defaultMaxRequests;

    public RateLimiter(SXBans plugin) {
        this.plugin = plugin;
        this.limits = new ConcurrentHashMap<>();
        this.defaultWindow = 60000;
        this.defaultMaxRequests = 5;
    }

    public boolean isRateLimited(String key) {
        return isRateLimited(key, defaultWindow, defaultMaxRequests);
    }

    public boolean isRateLimited(String key, long window, int maxRequests) {
        RateLimit limit = limits.computeIfAbsent(key, k -> new RateLimit());

        long now = System.currentTimeMillis();
        long windowStart = now - window;

        limit.requests.removeIf(time -> time < windowStart);

        if (limit.requests.size() >= maxRequests) {
            return true;
        }

        limit.requests.add(now);
        return false;
    }

    public boolean isPlayerRateLimited(String playerName) {
        return isRateLimited("player:" + playerName);
    }

    public boolean isPlayerRateLimited(String playerName, String action) {
        return isRateLimited("player:" + playerName + ":" + action);
    }

    public boolean isPlayerRateLimited(String playerName, String action, long window, int maxRequests) {
        return isRateLimited("player:" + playerName + ":" + action, window, maxRequests);
    }

    public boolean isIpRateLimited(String ip) {
        return isRateLimited("ip:" + ip);
    }

    public boolean isIpRateLimited(String ip, String action) {
        return isRateLimited("ip:" + ip + ":" + action);
    }

    public void resetRateLimit(String key) {
        limits.remove(key);
    }

    public void resetPlayerRateLimit(String playerName) {
        limits.keySet().removeIf(key -> key.startsWith("player:" + playerName));
    }

    public void resetIpRateLimit(String ip) {
        limits.keySet().removeIf(key -> key.startsWith("ip:" + ip));
    }

    public void clearAll() {
        limits.clear();
    }

    public int getRequestCount(String key) {
        RateLimit limit = limits.get(key);
        if (limit == null) return 0;

        long now = System.currentTimeMillis();
        long windowStart = now - defaultWindow;
        limit.requests.removeIf(time -> time < windowStart);

        return limit.requests.size();
    }

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
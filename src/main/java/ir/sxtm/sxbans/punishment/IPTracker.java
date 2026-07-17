package ir.sxtm.sxbans.punishment;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.IPData;
import ir.sxtm.sxbans.models.Punishment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class IPTracker {
    private final SXBans plugin;
    private final Map<String, IPData> ipDataCache;
    private final Map<String, Set<UUID>> ipPlayerMapping;
    private final Map<UUID, Set<String>> playerIPHistory;

    public IPTracker(SXBans plugin) {
        this.plugin = plugin;
        this.ipDataCache = new ConcurrentHashMap<>();
        this.ipPlayerMapping = new ConcurrentHashMap<>();
        this.playerIPHistory = new ConcurrentHashMap<>();
        loadAllIPData();
    }

    private void loadAllIPData() {
        // getAllIPData doesn't exist, so we'll load from cache or initialize empty
        // The IP data will be loaded as players join
        plugin.getSXBansLogger().info("IP tracker initialized");
    }

    public void trackIP(String ip, UUID playerUUID, String playerName) {
        IPData data = ipDataCache.computeIfAbsent(ip, k -> new IPData(ip));
        data.addPlayer(playerUUID, playerName);
        data.setLastSeen(System.currentTimeMillis());
        data.incrementLogins();

        ipPlayerMapping.computeIfAbsent(ip, k -> new HashSet<>()).add(playerUUID);
        playerIPHistory.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(ip);

        // Save to storage
        plugin.getPunishmentStorage().saveIPData(data);
    }

    public IPData getIPData(String ip) {
        return ipDataCache.get(ip);
    }

    public List<UUID> getPlayersOnIP(String ip) {
        Set<UUID> players = ipPlayerMapping.get(ip);
        return players != null ? new ArrayList<>(players) : Collections.emptyList();
    }

    public Set<String> getIPsForPlayer(UUID playerUUID) {
        return playerIPHistory.getOrDefault(playerUUID, Collections.emptySet());
    }

    public List<UUID> findAltAccounts(UUID playerUUID) {
        Set<String> ips = getIPsForPlayer(playerUUID);
        Set<UUID> alts = new HashSet<>();

        for (String ip : ips) {
            alts.addAll(getPlayersOnIP(ip));
        }

        alts.remove(playerUUID);
        return new ArrayList<>(alts);
    }

    public boolean isBlacklistedIP(String ip) {
        IPData data = ipDataCache.get(ip);
        return data != null && data.isBlacklisted();
    }

    public void blacklistIP(String ip) {
        IPData data = ipDataCache.computeIfAbsent(ip, k -> new IPData(ip));
        data.setBlacklisted(true);
        plugin.getPunishmentStorage().saveIPData(data);
    }

    public void unblacklistIP(String ip) {
        IPData data = ipDataCache.get(ip);
        if (data != null) {
            data.setBlacklisted(false);
            plugin.getPunishmentStorage().saveIPData(data);
        }
    }

    public Map<String, Integer> getIPUsageStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (Map.Entry<String, IPData> entry : ipDataCache.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().getPlayerCount());
        }
        return stats;
    }

    public List<IPData> getSuspiciousIPs(int threshold) {
        return ipDataCache.values().stream()
                .filter(data -> data.getPlayerCount() > threshold)
                .sorted((a, b) -> Integer.compare(b.getPlayerCount(), a.getPlayerCount()))
                .collect(Collectors.toList());
    }

    public String getGeoInfo(String ip) {
        // This would call an external API for geo-location
        // Placeholder implementation
        try {
            InetAddress address = InetAddress.getByName(ip);
            return "Location: " + address.getHostAddress();
        } catch (UnknownHostException e) {
            return "Unknown";
        }
    }

    public void cleanup() {
        // Remove old data (30 days)
        long cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        ipDataCache.values().removeIf(data -> data.getLastSeen() < cutoff);
    }

    public Map<String, IPData> getIpDataCache() {
        return ipDataCache;
    }
}
package ir.sxtm.sxbans.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Cache manager using Caffeine for high-performance caching.
 */
public class CacheManager {
    private final SXBans plugin;

    // Caches
    private final Cache<UUID, Boolean> bannedCache;
    private final Cache<UUID, Boolean> mutedCache;
    private final Cache<String, Boolean> ipBannedCache;
    private final Cache<String, Boolean> ipMutedCache;
    private final Cache<UUID, Integer> warningCache;
    private final Cache<UUID, Punishment> activeBanCache;
    private final Cache<UUID, Punishment> activeMuteCache;
    private final Cache<String, Punishment> activeIpBanCache;
    private final Cache<String, Punishment> activeIpMuteCache;
    private final Cache<UUID, java.util.List<Punishment>> playerPunishmentsCache;

    public CacheManager(SXBans plugin) {
        this.plugin = plugin;

        // Initialize caches with appropriate sizes and expiry
        this.bannedCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();

        this.mutedCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();

        this.ipBannedCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();

        this.ipMutedCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();

        this.warningCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();

        this.activeBanCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();

        this.activeMuteCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();

        this.activeIpBanCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();

        this.activeIpMuteCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();

        this.playerPunishmentsCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }

    // Banned cache methods
    public boolean isBanned(UUID uuid) {
        Boolean cached = bannedCache.getIfPresent(uuid);
        if (cached != null) {
            return cached;
        }
        boolean banned = plugin.getPunishmentManager().isPlayerBanned(uuid);
        bannedCache.put(uuid, banned);
        return banned;
    }

    public void invalidateBanned(UUID uuid) {
        bannedCache.invalidate(uuid);
    }

    // Muted cache methods
    public boolean isMuted(UUID uuid) {
        Boolean cached = mutedCache.getIfPresent(uuid);
        if (cached != null) {
            return cached;
        }
        boolean muted = plugin.getPunishmentManager().isPlayerMuted(uuid);
        mutedCache.put(uuid, muted);
        return muted;
    }

    public void invalidateMuted(UUID uuid) {
        mutedCache.invalidate(uuid);
    }

    // IP Banned cache methods
    public boolean isIpBanned(String ip) {
        Boolean cached = ipBannedCache.getIfPresent(ip);
        if (cached != null) {
            return cached;
        }
        boolean banned = plugin.getPunishmentManager().isIpBanned(ip);
        ipBannedCache.put(ip, banned);
        return banned;
    }

    public void invalidateIpBanned(String ip) {
        ipBannedCache.invalidate(ip);
    }

    // IP Muted cache methods
    public boolean isIpMuted(String ip) {
        Boolean cached = ipMutedCache.getIfPresent(ip);
        if (cached != null) {
            return cached;
        }
        boolean muted = plugin.getPunishmentManager().isIpMuted(ip);
        ipMutedCache.put(ip, muted);
        return muted;
    }

    public void invalidateIpMuted(String ip) {
        ipMutedCache.invalidate(ip);
    }

    // Warning cache methods
    public int getWarnings(UUID uuid) {
        Integer cached = warningCache.getIfPresent(uuid);
        if (cached != null) {
            return cached;
        }
        int warnings = plugin.getPunishmentManager().getWarningCount(uuid);
        warningCache.put(uuid, warnings);
        return warnings;
    }

    public void invalidateWarnings(UUID uuid) {
        warningCache.invalidate(uuid);
    }

    // Active ban cache methods
    public Punishment getActiveBan(UUID uuid) {
        Punishment cached = activeBanCache.getIfPresent(uuid);
        if (cached != null) {
            return cached;
        }
        Punishment ban = plugin.getPunishmentManager().getActiveBan(uuid);
        if (ban != null) {
            activeBanCache.put(uuid, ban);
        }
        return ban;
    }

    public void invalidateActiveBan(UUID uuid) {
        activeBanCache.invalidate(uuid);
    }

    // Active mute cache methods
    public Punishment getActiveMute(UUID uuid) {
        Punishment cached = activeMuteCache.getIfPresent(uuid);
        if (cached != null) {
            return cached;
        }
        Punishment mute = plugin.getPunishmentManager().getActiveMute(uuid);
        if (mute != null) {
            activeMuteCache.put(uuid, mute);
        }
        return mute;
    }

    public void invalidateActiveMute(UUID uuid) {
        activeMuteCache.invalidate(uuid);
    }

    // Active IP ban cache methods
    public Punishment getActiveIpBan(String ip) {
        Punishment cached = activeIpBanCache.getIfPresent(ip);
        if (cached != null) {
            return cached;
        }
        // Need to find active IP ban
        for (Punishment p : plugin.getPunishmentManager().getAllActivePunishments()) {
            if (p.getType() == Punishment.PunishmentType.IP_BAN && ip.equals(p.getIpAddress())) {
                activeIpBanCache.put(ip, p);
                return p;
            }
        }
        return null;
    }

    public void invalidateActiveIpBan(String ip) {
        activeIpBanCache.invalidate(ip);
    }

    // Active IP mute cache methods
    public Punishment getActiveIpMute(String ip) {
        Punishment cached = activeIpMuteCache.getIfPresent(ip);
        if (cached != null) {
            return cached;
        }
        for (Punishment p : plugin.getPunishmentManager().getAllActivePunishments()) {
            if (p.getType() == Punishment.PunishmentType.IP_MUTE && ip.equals(p.getIpAddress())) {
                activeIpMuteCache.put(ip, p);
                return p;
            }
        }
        return null;
    }

    public void invalidateActiveIpMute(String ip) {
        activeIpMuteCache.invalidate(ip);
    }

    // Player punishments cache
    public java.util.List<Punishment> getPlayerPunishments(UUID uuid) {
        java.util.List<Punishment> cached = playerPunishmentsCache.getIfPresent(uuid);
        if (cached != null) {
            return cached;
        }
        java.util.List<Punishment> punishments = plugin.getPunishmentManager().getPlayerPunishments(uuid);
        playerPunishmentsCache.put(uuid, punishments);
        return punishments;
    }

    public void invalidatePlayerPunishments(UUID uuid) {
        playerPunishmentsCache.invalidate(uuid);
    }

    /**
     * Invalidate all caches for a player.
     *
     * @param uuid The player UUID
     */
    public void invalidatePlayer(UUID uuid) {
        invalidateBanned(uuid);
        invalidateMuted(uuid);
        invalidateWarnings(uuid);
        invalidateActiveBan(uuid);
        invalidateActiveMute(uuid);
        invalidatePlayerPunishments(uuid);
    }

    /**
     * Clear all caches.
     */
    public void clearAll() {
        bannedCache.invalidateAll();
        mutedCache.invalidateAll();
        ipBannedCache.invalidateAll();
        ipMutedCache.invalidateAll();
        warningCache.invalidateAll();
        activeBanCache.invalidateAll();
        activeMuteCache.invalidateAll();
        activeIpBanCache.invalidateAll();
        activeIpMuteCache.invalidateAll();
        playerPunishmentsCache.invalidateAll();
    }

    /**
     * Get cache statistics.
     *
     * @return Map of cache statistics
     */
    public java.util.Map<String, Object> getStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("bannedCacheSize", bannedCache.estimatedSize());
        stats.put("mutedCacheSize", mutedCache.estimatedSize());
        stats.put("ipBannedCacheSize", ipBannedCache.estimatedSize());
        stats.put("ipMutedCacheSize", ipMutedCache.estimatedSize());
        stats.put("warningCacheSize", warningCache.estimatedSize());
        stats.put("activeBanCacheSize", activeBanCache.estimatedSize());
        stats.put("activeMuteCacheSize", activeMuteCache.estimatedSize());
        stats.put("activeIpBanCacheSize", activeIpBanCache.estimatedSize());
        stats.put("activeIpMuteCacheSize", activeIpMuteCache.estimatedSize());
        stats.put("playerPunishmentsCacheSize", playerPunishmentsCache.estimatedSize());
        return stats;
    }
}
package ir.sxtm.sxbans.punishment;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.database.PunishmentStorage;
import ir.sxtm.sxbans.database.RedisManager;
import ir.sxtm.sxbans.models.HistoryEntry;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentStatus;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import ir.sxtm.sxbans.utils.CacheManager;
import ir.sxtm.sxbans.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PunishmentManager {
    private final SXBans plugin;
    private final PunishmentStorage storage;
    private final RedisManager redisManager;
    private final CacheManager cacheManager;
    private final MessageUtils messageUtils;

    private final Map<UUID, List<Punishment>> playerPunishmentsCache;
    private final Map<UUID, Punishment> activeBansCache;
    private final Map<UUID, Punishment> activeMutesCache;
    private final Map<String, Punishment> activeIpBansCache;
    private final Map<String, Punishment> activeIpMutesCache;
    private final Map<String, Integer> warningCountsCache;

    private boolean initialized;

    public PunishmentManager(SXBans plugin) {
        this.plugin = plugin;
        this.storage = plugin.getPunishmentStorage();
        this.redisManager = plugin.getRedisManager();
        this.cacheManager = plugin.getCacheManager();
        this.messageUtils = new MessageUtils(plugin);

        this.playerPunishmentsCache = new ConcurrentHashMap<>();
        this.activeBansCache = new ConcurrentHashMap<>();
        this.activeMutesCache = new ConcurrentHashMap<>();
        this.activeIpBansCache = new ConcurrentHashMap<>();
        this.activeIpMutesCache = new ConcurrentHashMap<>();
        this.warningCountsCache = new ConcurrentHashMap<>();
        this.initialized = false;
    }

    public void initialize() {
        if (initialized) return;
        loadAllPunishments();
        startExpiryChecker();
        startAutoUnbanChecker();
        startWarningExpiryChecker();
        initialized = true;
        plugin.getSXBansLogger().info("Punishment manager initialized");
    }

    private void loadAllPunishments() {
        List<Punishment> allPunishments = storage.getAllPunishments();
        for (Punishment p : allPunishments) {
            addToCache(p);
        }
        plugin.getSXBansLogger().info("Loaded " + allPunishments.size() + " punishments into cache");
    }

    private void addToCache(Punishment punishment) {
        UUID playerId = punishment.getPlayerUUID();
        playerPunishmentsCache.computeIfAbsent(playerId, k -> new ArrayList<>()).add(punishment);

        if (punishment.isActive()) {
            switch (punishment.getType()) {
                case BAN:
                case TEMP_BAN:
                    activeBansCache.put(playerId, punishment);
                    break;
                case MUTE:
                case TEMP_MUTE:
                    activeMutesCache.put(playerId, punishment);
                    break;
                case IP_BAN:
                    if (punishment.getIpAddress() != null) {
                        activeIpBansCache.put(punishment.getIpAddress(), punishment);
                    }
                    break;
                case IP_MUTE:
                    if (punishment.getIpAddress() != null) {
                        activeIpMutesCache.put(punishment.getIpAddress(), punishment);
                    }
                    break;
                case WARN:
                    warningCountsCache.merge(playerId.toString(), 1, Integer::sum);
                    break;
            }
        }

        if (redisManager != null && redisManager.isEnabled()) {
            redisManager.cachePunishment(punishment.getId().toString(), punishment);
        }
    }

    public Punishment applyPunishment(UUID playerUUID, String playerName, PunishmentType type,
                                      String reason, long duration, UUID executorUUID, String executorName) {
        if (playerUUID == null || executorUUID == null) {
            throw new IllegalArgumentException("Player and executor UUIDs cannot be null");
        }

        if (isPunished(playerUUID, type)) {
            return null;
        }

        Punishment punishment = new Punishment(
                UUID.randomUUID(),
                playerUUID,
                playerName,
                type,
                reason,
                duration,
                executorUUID,
                executorName
        );

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            punishment.setIpAddress(player.getAddress().getAddress().getHostAddress());
            punishment.setWorldName(player.getWorld().getName());
        }

        String serverName = "Unknown";
        try {
            serverName = Bukkit.getServer().getName();
        } catch (Exception e) {
            try {
                serverName = Bukkit.getServer().getIp();
                if (serverName == null || serverName.isEmpty()) {
                    serverName = "Unknown";
                }
            } catch (Exception ex) {
                serverName = "Unknown";
            }
        }
        punishment.setServerName(serverName);

        storage.savePunishment(punishment);
        addToCache(punishment);
        addHistory(playerUUID, playerName, "PUNISHMENT",
                type.name() + ": " + reason + " by " + executorName, executorUUID, executorName);

        // ===== FIX: اجرای اثرات مجازات در Thread اصلی =====
        applyPunishmentEffectsSync(punishment);

        broadcastPunishment(punishment);

        if (redisManager != null && redisManager.isEnabled()) {
            redisManager.publishPunishment(punishment);
        }

        if (type == PunishmentType.WARN) {
            checkAutoBan(punishment);
        }

        return punishment;
    }

    // ===== FIX: متد جدید برای اجرای اثرات مجازات در Thread اصلی =====
    private void applyPunishmentEffectsSync(Punishment punishment) {
        Player player = Bukkit.getPlayer(punishment.getPlayerUUID());
        if (player == null) return;

        // اجرا در Thread اصلی سرور
        if (Bukkit.isPrimaryThread()) {
            applyPunishmentEffects(punishment);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> applyPunishmentEffects(punishment));
        }
    }

    private void applyPunishmentEffects(Punishment punishment) {
        Player player = Bukkit.getPlayer(punishment.getPlayerUUID());
        if (player == null) return;

        switch (punishment.getType()) {
            case BAN:
            case TEMP_BAN:
            case IP_BAN:
                player.kickPlayer(plugin.getMessagesManager().getBanMessage(punishment));
                break;
            case KICK:
            case IP_KICK:
                player.kickPlayer(plugin.getMessagesManager().getKickMessage(punishment));
                break;
            case MUTE:
            case TEMP_MUTE:
            case IP_MUTE:
                player.sendMessage(plugin.getMessagesManager().getMuteMessage(punishment));
                break;
            case WARN:
                player.sendMessage(plugin.getMessagesManager().getWarnMessage(punishment));
                break;
        }
    }

    private boolean isPunished(UUID playerUUID, PunishmentType type) {
        switch (type) {
            case BAN:
            case TEMP_BAN:
                return activeBansCache.containsKey(playerUUID);
            case MUTE:
            case TEMP_MUTE:
                return activeMutesCache.containsKey(playerUUID);
            default:
                return false;
        }
    }

    public boolean removePunishment(UUID punishmentId, UUID removerUUID, String removerName, String reason) {
        Punishment punishment = storage.getPunishment(punishmentId);
        if (punishment == null) {
            return false;
        }

        if (punishment.getStatus() != PunishmentStatus.ACTIVE) {
            return false;
        }

        punishment.setStatus(PunishmentStatus.REMOVED);
        punishment.setRemoverUUID(removerUUID);
        punishment.setRemoverName(removerName);
        punishment.setRemovedTime(Instant.now().toEpochMilli());
        punishment.setRemoveReason(reason);
        punishment.setUpdatedAt(Instant.now().toEpochMilli());

        try {
            storage.updatePunishment(punishment);
        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Failed to update punishment: " + e.getMessage());
            return false;
        }

        removeFromCache(punishment);
        addHistory(punishment.getPlayerUUID(), punishment.getPlayerName(), "REMOVE_PUNISHMENT",
                punishment.getType().name() + " removed by " + removerName + ": " + reason,
                removerUUID, removerName);

        broadcastRemoval(punishment);

        return true;
    }

    private void removeFromCache(Punishment punishment) {
        UUID playerId = punishment.getPlayerUUID();

        switch (punishment.getType()) {
            case BAN:
            case TEMP_BAN:
                activeBansCache.remove(playerId);
                break;
            case MUTE:
            case TEMP_MUTE:
                activeMutesCache.remove(playerId);
                break;
            case IP_BAN:
                if (punishment.getIpAddress() != null) {
                    activeIpBansCache.remove(punishment.getIpAddress());
                }
                break;
            case IP_MUTE:
                if (punishment.getIpAddress() != null) {
                    activeIpMutesCache.remove(punishment.getIpAddress());
                }
                break;
            case WARN:
                warningCountsCache.merge(playerId.toString(), -1, Integer::sum);
                if (warningCountsCache.getOrDefault(playerId.toString(), 0) <= 0) {
                    warningCountsCache.remove(playerId.toString());
                }
                break;
        }
    }

    private void broadcastPunishment(Punishment punishment) {
        String basePath = "broadcast." + punishment.getType().name().toLowerCase();
        Map<String, String> placeholders = messageUtils.formatPunishmentPlaceholders(punishment);
        messageUtils.sendBroadcastWithPermission(basePath, placeholders, punishment);
    }

    private void broadcastRemoval(Punishment punishment) {
        String basePath = "broadcast." + punishment.getType().name().toLowerCase().replace("_", "");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", punishment.getPlayerName());
        placeholders.put("executor", punishment.getRemoverName() != null ? punishment.getRemoverName() : "Console");
        placeholders.put("reason", punishment.getRemoveReason() != null ? punishment.getRemoveReason() : "No reason provided");
        placeholders.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis())));
        placeholders.put("server", punishment.getServerName() != null ? punishment.getServerName() : "Global");

        String unbanPath = "broadcast.unban";
        if (punishment.getType() == PunishmentType.MUTE || punishment.getType() == PunishmentType.TEMP_MUTE) {
            unbanPath = "broadcast.unmute";
        }
        if (punishment.getType() == PunishmentType.IP_BAN) {
            unbanPath = "broadcast.ipunban";
        }
        if (punishment.getType() == PunishmentType.IP_MUTE) {
            unbanPath = "broadcast.ipunmute";
        }

        messageUtils.sendBroadcastWithPermission(unbanPath, placeholders, punishment);
    }

    private void addHistory(UUID playerUUID, String playerName, String action,
                            String details, UUID executorUUID, String executorName) {
        HistoryEntry entry = new HistoryEntry(
                UUID.randomUUID(),
                playerUUID,
                playerName,
                action,
                details,
                executorUUID,
                executorName
        );
        storage.saveHistory(entry);
    }

    private void checkAutoBan(Punishment punishment) {
        int warnings = getWarningCount(punishment.getPlayerUUID());
        int maxWarnings = plugin.getConfigManager().getMaxWarnings();

        if (warnings >= maxWarnings && plugin.getConfigManager().isAutoBanEnabled()) {
            long duration = plugin.getConfigManager().getAutoBanDuration();
            applyPunishment(
                    punishment.getPlayerUUID(),
                    punishment.getPlayerName(),
                    PunishmentType.TEMP_BAN,
                    "Auto-banned for reaching " + warnings + " warnings",
                    duration,
                    UUID.fromString("00000000-0000-0000-0000-000000000000"),
                    "System"
            );
        }
    }

    // ===== Query Methods =====
    public boolean isPlayerBanned(UUID playerUUID) {
        Punishment ban = activeBansCache.get(playerUUID);
        return ban != null && ban.isActive();
    }

    public boolean isPlayerMuted(UUID playerUUID) {
        Punishment mute = activeMutesCache.get(playerUUID);
        return mute != null && mute.isActive();
    }

    public boolean isIpBanned(String ip) {
        Punishment ban = activeIpBansCache.get(ip);
        return ban != null && ban.isActive();
    }

    public boolean isIpMuted(String ip) {
        Punishment mute = activeIpMutesCache.get(ip);
        return mute != null && mute.isActive();
    }

    public Punishment getActiveBan(UUID playerUUID) {
        return activeBansCache.get(playerUUID);
    }

    public Punishment getActiveMute(UUID playerUUID) {
        return activeMutesCache.get(playerUUID);
    }

    public List<Punishment> getPlayerPunishments(UUID playerUUID) {
        return playerPunishmentsCache.getOrDefault(playerUUID, Collections.emptyList())
                .stream()
                .sorted((a, b) -> Long.compare(b.getStartTime(), a.getStartTime()))
                .collect(Collectors.toList());
    }

    public List<Punishment> getActivePunishments(UUID playerUUID) {
        return getPlayerPunishments(playerUUID)
                .stream()
                .filter(Punishment::isActive)
                .collect(Collectors.toList());
    }

    public int getWarningCount(UUID playerUUID) {
        return warningCountsCache.getOrDefault(playerUUID.toString(), 0);
    }

    public void resetWarnings(UUID playerUUID) {
        List<Punishment> punishments = playerPunishmentsCache.get(playerUUID);
        if (punishments != null) {
            punishments.removeIf(p -> p.getType() == PunishmentType.WARN);
        }
        warningCountsCache.remove(playerUUID.toString());
    }

    // ===== Background Tasks =====
    private void startExpiryChecker() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = Instant.now().toEpochMilli();
            for (Punishment p : getAllActivePunishments()) {
                if (!p.isPermanent() && p.getEndTime() <= now) {
                    p.setStatus(PunishmentStatus.EXPIRED);
                    p.setUpdatedAt(now);
                    storage.updatePunishment(p);
                    removeFromCache(p);

                    Player player = Bukkit.getPlayer(p.getPlayerUUID());
                    if (player != null) {
                        player.sendMessage(plugin.getMessagesManager().getColoredMessage("punishment.expired",
                                Map.of("type", p.getType().getDisplayName())));
                    }
                }
            }
        }, 0, 20 * 60);
    }

    private void startAutoUnbanChecker() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = Instant.now().toEpochMilli();

            for (Punishment p : new ArrayList<>(activeBansCache.values())) {
                if (!p.isPermanent() && p.getEndTime() <= now) {
                    p.setStatus(PunishmentStatus.EXPIRED);
                    storage.updatePunishment(p);
                    activeBansCache.remove(p.getPlayerUUID());

                    Player player = Bukkit.getPlayer(p.getPlayerUUID());
                    if (player != null) {
                        player.sendMessage(plugin.getMessagesManager().getColoredMessage("punishment.auto-unban"));
                    }
                }
            }

            for (Punishment p : new ArrayList<>(activeMutesCache.values())) {
                if (!p.isPermanent() && p.getEndTime() <= now) {
                    p.setStatus(PunishmentStatus.EXPIRED);
                    storage.updatePunishment(p);
                    activeMutesCache.remove(p.getPlayerUUID());

                    Player player = Bukkit.getPlayer(p.getPlayerUUID());
                    if (player != null) {
                        player.sendMessage(plugin.getMessagesManager().getColoredMessage("punishment.auto-unmute"));
                    }
                }
            }
        }, 0, 20 * 5);
    }

    private void startWarningExpiryChecker() {
        long expiry = plugin.getConfigManager().getWarningExpiry();
        if (expiry <= 0) return;

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long cutoff = Instant.now().toEpochMilli() - expiry;

            for (List<Punishment> punishments : playerPunishmentsCache.values()) {
                punishments.removeIf(p -> {
                    if (p.getType() == PunishmentType.WARN && p.getStartTime() < cutoff) {
                        warningCountsCache.merge(p.getPlayerUUID().toString(), -1, Integer::sum);
                        if (warningCountsCache.getOrDefault(p.getPlayerUUID().toString(), 0) <= 0) {
                            warningCountsCache.remove(p.getPlayerUUID().toString());
                        }
                        return true;
                    }
                    return false;
                });
            }
        }, 0, 20 * 60 * 60);
    }

    public List<Punishment> getAllActivePunishments() {
        List<Punishment> active = new ArrayList<>();
        active.addAll(activeBansCache.values());
        active.addAll(activeMutesCache.values());
        active.addAll(activeIpBansCache.values());
        active.addAll(activeIpMutesCache.values());
        return active;
    }

    public void saveAll() {
        storage.saveAll();
    }

    public void addPunishmentToCache(Punishment punishment) {
        addToCache(punishment);
    }

    public boolean isInitialized() {
        return initialized;
    }
}
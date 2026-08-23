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

                        plugin.getServer().banIP(punishment.getIpAddress());
                        plugin.getSXBansLogger().info("IP " + punishment.getIpAddress() + " added to server ban list");
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
        return applyPunishment(playerUUID, playerName, type, reason, duration, executorUUID, executorName, null);
    }

    public Punishment applyPunishment(UUID playerUUID, String playerName, PunishmentType type,
                                      String reason, long duration, UUID executorUUID, String executorName,
                                      String explicitIp) {
        return applyPunishmentInternal(playerUUID, playerName, type, reason, duration, executorUUID, executorName, explicitIp, false);
    }

    public Punishment applyPunishmentFromNetwork(UUID playerUUID, String playerName, PunishmentType type,
                                                 String reason, long duration, UUID executorUUID, String executorName,
                                                 String explicitIp) {
        return applyPunishmentInternal(playerUUID, playerName, type, reason, duration, executorUUID, executorName, explicitIp, true);
    }

    private Punishment applyPunishmentInternal(UUID playerUUID, String playerName, PunishmentType type,
                                      String reason, long duration, UUID executorUUID, String executorName,
                                      String explicitIp, boolean fromNetwork) {
        if (playerUUID == null || executorUUID == null) {
            throw new IllegalArgumentException("Player and executor UUIDs cannot be null");
        }

        if (isPunished(playerUUID, type)) {
            return null;
        }

        if ((type == PunishmentType.IP_BAN && isIpBanned(explicitIp)) ||
                (type == PunishmentType.IP_MUTE && isIpMuted(explicitIp))) {
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
        String ip = explicitIp;
        if (ip == null && player != null) {
            ip = player.getAddress().getAddress().getHostAddress();
        }
        if (ip != null && !ip.isEmpty()) {
            punishment.setIpAddress(ip);
        }
        if (player != null) {
            punishment.setWorldName(player.getWorld().getName());
        }

        punishment.setServerName(plugin.getConfigManager().getServerName());

        storage.savePunishment(punishment);
        addToCache(punishment);
        addHistory(playerUUID, playerName, "PUNISHMENT",
                type.name() + ": " + reason + " by " + executorName, executorUUID, executorName);

        applyPunishmentEffectsSync(punishment);

        broadcastPunishment(punishment);

        if (!fromNetwork && plugin.getConfigManager().isNetworkSyncEnabled()) {
            plugin.getProxyManager().broadcastPunishment(punishment);
        }

        if (type == PunishmentType.WARN) {
            checkAutoBan(punishment);
        }

        return punishment;
    }

    private void applyPunishmentEffectsSync(Punishment punishment) {
        Player player = Bukkit.getPlayer(punishment.getPlayerUUID());

        if (Bukkit.isPrimaryThread()) {
            applyPunishmentEffects(punishment);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> applyPunishmentEffects(punishment));
        }
    }

    private void applyPunishmentEffects(Punishment punishment) {
        Player player = Bukkit.getPlayer(punishment.getPlayerUUID());
        String message = getPunishmentMessage(punishment);
        if (message == null || message.isEmpty()) {
            message = "&cYou have been " + punishment.getType().getDisplayName() + "!";
        }

        switch (punishment.getType()) {
            case BAN:
            case TEMP_BAN:
                if (player != null) {
                    player.kickPlayer(plugin.getMessagesManager().colorize(message));
                }
                break;

            case IP_BAN:

                String ip = punishment.getIpAddress();
                if (ip != null && !ip.isEmpty()) {

                    plugin.getServer().banIP(ip);
                    plugin.getSXBansLogger().info("IP " + ip + " has been banned by " + punishment.getExecutorName());
                }

                if (player != null) {
                    player.kickPlayer(plugin.getMessagesManager().colorize(message));
                }

                if (ip != null && !ip.isEmpty()) {
                    for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                        if (onlinePlayer.getAddress() != null &&
                                onlinePlayer.getAddress().getAddress().getHostAddress().equals(ip) &&
                                !onlinePlayer.equals(player)) {
                            onlinePlayer.kickPlayer(plugin.getMessagesManager().colorize(
                                    "&cYour IP address has been banned!"
                            ));
                        }
                    }
                }
                break;

            case KICK:
            case IP_KICK:
                if (player != null) {
                    player.kickPlayer(plugin.getMessagesManager().colorize(message));
                }
                break;

            case MUTE:
            case TEMP_MUTE:
                if (player != null) {
                    player.sendMessage(plugin.getMessagesManager().colorize(message));
                }
                break;

            case IP_MUTE:

                if (player != null) {
                    player.sendMessage(plugin.getMessagesManager().colorize(message));
                }

                String muteIp = punishment.getIpAddress();
                if (muteIp != null && !muteIp.isEmpty()) {
                    for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                        if (onlinePlayer.getAddress() != null &&
                                onlinePlayer.getAddress().getAddress().getHostAddress().equals(muteIp) &&
                                !onlinePlayer.equals(player)) {
                            onlinePlayer.sendMessage(plugin.getMessagesManager().colorize(
                                    "&cYour IP address has been muted!"
                            ));
                        }
                    }
                }
                break;

            case WARN:
                if (player != null) {
                    player.sendMessage(plugin.getMessagesManager().colorize(message));
                }
                break;
        }
    }

    public String getPunishmentMessage(Punishment punishment) {
        if (punishment == null) return null;

        switch (punishment.getType()) {
            case BAN:
                return plugin.getMessagesManager().getBanMessage(punishment);
            case TEMP_BAN:
                return plugin.getMessagesManager().getTempBanMessage(punishment);
            case MUTE:
                return plugin.getMessagesManager().getMuteMessage(punishment);
            case TEMP_MUTE:
                return plugin.getMessagesManager().getTempMuteMessage(punishment);
            case KICK:
                return plugin.getMessagesManager().getKickMessage(punishment);
            case IP_KICK:
                return plugin.getMessagesManager().getIpKickMessage(punishment);
            case IP_BAN:
                return plugin.getMessagesManager().getIpBanMessage(punishment);
            case IP_MUTE:
                return plugin.getMessagesManager().getIpMuteMessage(punishment);
            case WARN:
                return plugin.getMessagesManager().getWarnMessage(punishment);
            default:
                return plugin.getMessagesManager().getColoredMessage("punishment." +
                        punishment.getType().name().toLowerCase().replace("_", "") + ".message", null);
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
        return removePunishmentInternal(punishmentId, removerUUID, removerName, reason, false);
    }

    public boolean removePunishmentFromNetwork(UUID punishmentId, UUID removerUUID, String removerName, String reason) {
        return removePunishmentInternal(punishmentId, removerUUID, removerName, reason, true);
    }

    private boolean removePunishmentInternal(UUID punishmentId, UUID removerUUID, String removerName, String reason, boolean fromNetwork) {
        Punishment punishment = storage.getPunishment(punishmentId);
        if (punishment == null) {
            return false;
        }

        if (punishment.getStatus() != PunishmentStatus.ACTIVE) {
            return false;
        }

        if (punishment.getType() == PunishmentType.IP_BAN) {
            String ip = punishment.getIpAddress();
            if (ip != null && !ip.isEmpty()) {

                plugin.getServer().unbanIP(ip);
                plugin.getSXBansLogger().info("IP " + ip + " has been unbanned by " + removerName);
            }
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

        if (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMP_BAN ||
                punishment.getType() == PunishmentType.MUTE || punishment.getType() == PunishmentType.TEMP_MUTE) {
            List<Punishment> playerPunishments = playerPunishmentsCache.getOrDefault(punishment.getPlayerUUID(), Collections.emptyList());
            for (Punishment other : new ArrayList<>(playerPunishments)) {
                if (other.getId().equals(punishment.getId())) continue;
                if (other.getStatus() != PunishmentStatus.ACTIVE) continue;
                boolean sameCategory = (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMP_BAN)
                        ? (other.getType() == PunishmentType.BAN || other.getType() == PunishmentType.TEMP_BAN)
                        : (other.getType() == PunishmentType.MUTE || other.getType() == PunishmentType.TEMP_MUTE);
                if (!sameCategory) continue;

                plugin.getSXBansLogger().warning("Found an extra ACTIVE " + other.getType() +
                        " record (" + other.getId() + ") for " + punishment.getPlayerName() +
                        " while removing " + punishment.getId() + " — cleaning it up too.");
                other.setStatus(PunishmentStatus.REMOVED);
                other.setRemoverUUID(removerUUID);
                other.setRemoverName(removerName);
                other.setRemovedTime(Instant.now().toEpochMilli());
                other.setRemoveReason(reason);
                other.setUpdatedAt(Instant.now().toEpochMilli());
                storage.updatePunishment(other);
                removeFromCache(other);
            }
        }

        addHistory(punishment.getPlayerUUID(), punishment.getPlayerName(), "REMOVE_PUNISHMENT",
                punishment.getType().name() + " removed by " + removerName + ": " + reason,
                removerUUID, removerName);

        broadcastRemoval(punishment);

        if (!fromNetwork && plugin.getConfigManager().isNetworkSyncEnabled()) {
            plugin.getProxyManager().broadcastPunishmentRemoval(punishment);
        }

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

        String basePath = "broadcast." + punishment.getType().name().toLowerCase().replace("_", "");
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

            PunishmentType autoBanType = duration > 0 ? PunishmentType.TEMP_BAN : PunishmentType.BAN;
            applyPunishment(
                    punishment.getPlayerUUID(),
                    punishment.getPlayerName(),
                    autoBanType,
                    "Auto-banned for reaching " + warnings + " warnings",
                    duration,
                    UUID.fromString("00000000-0000-0000-0000-000000000000"),
                    "System"
            );
        }
    }

    public boolean isPlayerBanned(UUID playerUUID) {
        Punishment ban = activeBansCache.get(playerUUID);
        return ban != null && ban.isActive();
    }

    public boolean isPlayerMuted(UUID playerUUID) {
        Punishment mute = activeMutesCache.get(playerUUID);
        return mute != null && mute.isActive();
    }

    public boolean isIpBanned(String ip) {

        if (ip == null || ip.isEmpty()) return false;

        Punishment ban = activeIpBansCache.get(ip);
        if (ban != null && ban.isActive()) {
            return true;
        }

        if (plugin.getServer().getBanList(org.bukkit.BanList.Type.IP).isBanned(ip)) {
            return true;
        }

        return false;
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

    private void startExpiryChecker() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = Instant.now().toEpochMilli();
            for (Punishment p : getAllActivePunishments()) {
                if (!p.isPermanent() && p.getEndTime() <= now) {
                    expirePunishment(p, now);
                }
            }
        }, 0, 20 * 5);
    }

    private void expirePunishment(Punishment p, long now) {
        p.setStatus(PunishmentStatus.EXPIRED);
        p.setUpdatedAt(now);
        storage.updatePunishment(p);
        removeFromCache(p);

        if (p.getType() == PunishmentType.IP_BAN && p.getIpAddress() != null) {
            plugin.getServer().unbanIP(p.getIpAddress());
            plugin.getSXBansLogger().info("Expired IP ban removed from server ban list: " + p.getIpAddress());
        }

        Player player = Bukkit.getPlayer(p.getPlayerUUID());
        if (player != null) {
            String message = plugin.getMessagesManager().getExpiredMessage(p.getType().getDisplayName());
            player.sendMessage(plugin.getMessagesManager().colorize(message));
        }
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

    public int clearPlayerData(UUID playerUUID) {
        List<Punishment> punishments = new ArrayList<>(getPlayerPunishments(playerUUID));
        for (Punishment p : punishments) {
            if (p.getStatus() == PunishmentStatus.ACTIVE) {
                if (p.getType() == PunishmentType.IP_BAN && p.getIpAddress() != null) {
                    plugin.getServer().unbanIP(p.getIpAddress());
                }
                removeFromCache(p);
            }
            storage.deletePunishment(p.getId());
        }
        playerPunishmentsCache.remove(playerUUID);
        warningCountsCache.remove(playerUUID.toString());
        return punishments.size();
    }

    public int clearAllData() {
        List<Punishment> all = storage.getAllPunishments();

        for (Punishment p : activeIpBansCache.values()) {
            if (p.getIpAddress() != null) {
                plugin.getServer().unbanIP(p.getIpAddress());
            }
        }

        for (Punishment p : all) {
            storage.deletePunishment(p.getId());
        }

        playerPunishmentsCache.clear();
        activeBansCache.clear();
        activeMutesCache.clear();
        activeIpBansCache.clear();
        activeIpMutesCache.clear();
        warningCountsCache.clear();

        return all.size();
    }

    public void addPunishmentToCache(Punishment punishment) {
        addToCache(punishment);
    }

    public boolean isInitialized() {
        return initialized;
    }
}
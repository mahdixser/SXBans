package ir.sxtm.sxbans.punishment;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutoBanManager {
    private final SXBans plugin;
    private final Map<UUID, Integer> warningCache;
    private final Map<UUID, Long> lastWarningTime;
    private boolean enabled;

    public AutoBanManager(SXBans plugin) {
        this.plugin = plugin;
        this.warningCache = new ConcurrentHashMap<>();
        this.lastWarningTime = new ConcurrentHashMap<>();
        this.enabled = true;
    }

    public void checkAutoBan(Punishment warning) {
        if (!enabled || warning.getType() != PunishmentType.WARN) return;

        UUID playerUUID = warning.getPlayerUUID();
        int warnings = plugin.getPunishmentManager().getWarningCount(playerUUID);
        int maxWarnings = plugin.getConfigManager().getMaxWarnings();

        if (warnings >= maxWarnings) {
            autoBanPlayer(playerUUID);
        }
    }

    private void autoBanPlayer(UUID playerUUID) {
        String playerName = getPlayerName(playerUUID);
        if (playerName == null) return;

        long duration = plugin.getConfigManager().getAutoBanDuration();
        PunishmentType type = duration > 0 ? PunishmentType.TEMP_BAN : PunishmentType.BAN;
        String reason = "Auto-banned for reaching " + plugin.getConfigManager().getMaxWarnings() + " warnings";

        plugin.getPunishmentManager().applyPunishment(
                playerUUID,
                playerName,
                type,
                reason,
                duration,
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                "System"
        );
    }

    public void resetWarnings(UUID playerUUID) {
        warningCache.remove(playerUUID);
        lastWarningTime.remove(playerUUID);
        plugin.getPunishmentManager().resetWarnings(playerUUID);
    }

    private String getPlayerName(UUID uuid) {
        return plugin.getServer().getOfflinePlayer(uuid).getName();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWarningCount(UUID playerUUID) {
        return warningCache.getOrDefault(playerUUID, 0);
    }

    public long getLastWarningTime(UUID playerUUID) {
        return lastWarningTime.getOrDefault(playerUUID, 0L);
    }
}
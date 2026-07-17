package ir.sxtm.sxbans.api;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import ir.sxtm.sxbans.models.HistoryEntry;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * API for managing punishments.
 * Provides methods to apply, remove, and query punishments.
 */
public class PunishmentAPI {
    private final SXBans plugin;

    public PunishmentAPI(SXBans plugin) {
        this.plugin = plugin;
    }

    /**
     * Apply a ban to a player.
     *
     * @param player The player to ban
     * @param reason The reason for the ban
     * @param executor The player executing the ban
     * @return The created punishment, or null if failed
     */
    public Punishment banPlayer(Player player, String reason, Player executor) {
        return banPlayer(player.getUniqueId(), player.getName(), reason, executor.getUniqueId(), executor.getName());
    }

    /**
     * Apply a ban to a player.
     *
     * @param playerUUID The UUID of the player to ban
     * @param playerName The name of the player to ban
     * @param reason The reason for the ban
     * @param executorUUID The UUID of the executor
     * @param executorName The name of the executor
     * @return The created punishment, or null if failed
     */
    public Punishment banPlayer(UUID playerUUID, String playerName, String reason, UUID executorUUID, String executorName) {
        return plugin.getPunishmentManager().applyPunishment(
                playerUUID,
                playerName,
                PunishmentType.BAN,
                reason,
                -1,
                executorUUID,
                executorName
        );
    }

    /**
     * Apply a temporary ban to a player.
     *
     * @param player The player to ban
     * @param reason The reason for the ban
     * @param duration The duration in milliseconds
     * @param executor The player executing the ban
     * @return The created punishment, or null if failed
     */
    public Punishment tempBanPlayer(Player player, String reason, long duration, Player executor) {
        return tempBanPlayer(player.getUniqueId(), player.getName(), reason, duration, executor.getUniqueId(), executor.getName());
    }

    /**
     * Apply a temporary ban to a player.
     *
     * @param playerUUID The UUID of the player to ban
     * @param playerName The name of the player to ban
     * @param reason The reason for the ban
     * @param duration The duration in milliseconds
     * @param executorUUID The UUID of the executor
     * @param executorName The name of the executor
     * @return The created punishment, or null if failed
     */
    public Punishment tempBanPlayer(UUID playerUUID, String playerName, String reason, long duration, UUID executorUUID, String executorName) {
        return plugin.getPunishmentManager().applyPunishment(
                playerUUID,
                playerName,
                PunishmentType.TEMP_BAN,
                reason,
                duration,
                executorUUID,
                executorName
        );
    }

    /**
     * Unban a player.
     *
     * @param player The player to unban
     * @param executor The player executing the unban
     * @param reason The reason for the unban
     * @return true if successful
     */
    public boolean unbanPlayer(Player player, Player executor, String reason) {
        return unbanPlayer(player.getUniqueId(), executor.getUniqueId(), executor.getName(), reason);
    }

    /**
     * Unban a player.
     *
     * @param playerUUID The UUID of the player to unban
     * @param executorUUID The UUID of the executor
     * @param executorName The name of the executor
     * @param reason The reason for the unban
     * @return true if successful
     */
    public boolean unbanPlayer(UUID playerUUID, UUID executorUUID, String executorName, String reason) {
        Punishment ban = plugin.getPunishmentManager().getActiveBan(playerUUID);
        if (ban == null) return false;

        return plugin.getPunishmentManager().removePunishment(
                ban.getId(),
                executorUUID,
                executorName,
                reason
        );
    }

    /**
     * Apply a mute to a player.
     *
     * @param player The player to mute
     * @param reason The reason for the mute
     * @param executor The player executing the mute
     * @return The created punishment, or null if failed
     */
    public Punishment mutePlayer(Player player, String reason, Player executor) {
        return mutePlayer(player.getUniqueId(), player.getName(), reason, executor.getUniqueId(), executor.getName());
    }

    /**
     * Apply a mute to a player.
     *
     * @param playerUUID The UUID of the player to mute
     * @param playerName The name of the player to mute
     * @param reason The reason for the mute
     * @param executorUUID The UUID of the executor
     * @param executorName The name of the executor
     * @return The created punishment, or null if failed
     */
    public Punishment mutePlayer(UUID playerUUID, String playerName, String reason, UUID executorUUID, String executorName) {
        return plugin.getPunishmentManager().applyPunishment(
                playerUUID,
                playerName,
                PunishmentType.MUTE,
                reason,
                -1,
                executorUUID,
                executorName
        );
    }

    /**
     * Apply a temporary mute to a player.
     *
     * @param player The player to mute
     * @param reason The reason for the mute
     * @param duration The duration in milliseconds
     * @param executor The player executing the mute
     * @return The created punishment, or null if failed
     */
    public Punishment tempMutePlayer(Player player, String reason, long duration, Player executor) {
        return tempMutePlayer(player.getUniqueId(), player.getName(), reason, duration, executor.getUniqueId(), executor.getName());
    }

    /**
     * Apply a temporary mute to a player.
     *
     * @param playerUUID The UUID of the player to mute
     * @param playerName The name of the player to mute
     * @param reason The reason for the mute
     * @param duration The duration in milliseconds
     * @param executorUUID The UUID of the executor
     * @param executorName The name of the executor
     * @return The created punishment, or null if failed
     */
    public Punishment tempMutePlayer(UUID playerUUID, String playerName, String reason, long duration, UUID executorUUID, String executorName) {
        return plugin.getPunishmentManager().applyPunishment(
                playerUUID,
                playerName,
                PunishmentType.TEMP_MUTE,
                reason,
                duration,
                executorUUID,
                executorName
        );
    }

    /**
     * Unmute a player.
     *
     * @param player The player to unmute
     * @param executor The player executing the unmute
     * @param reason The reason for the unmute
     * @return true if successful
     */
    public boolean unmutePlayer(Player player, Player executor, String reason) {
        return unmutePlayer(player.getUniqueId(), executor.getUniqueId(), executor.getName(), reason);
    }

    /**
     * Unmute a player.
     *
     * @param playerUUID The UUID of the player to unmute
     * @param executorUUID The UUID of the executor
     * @param executorName The name of the executor
     * @param reason The reason for the unmute
     * @return true if successful
     */
    public boolean unmutePlayer(UUID playerUUID, UUID executorUUID, String executorName, String reason) {
        Punishment mute = plugin.getPunishmentManager().getActiveMute(playerUUID);
        if (mute == null) return false;

        return plugin.getPunishmentManager().removePunishment(
                mute.getId(),
                executorUUID,
                executorName,
                reason
        );
    }

    /**
     * Kick a player.
     *
     * @param player The player to kick
     * @param reason The reason for the kick
     * @param executor The player executing the kick
     * @return The created punishment, or null if failed
     */
    public Punishment kickPlayer(Player player, String reason, Player executor) {
        return kickPlayer(player.getUniqueId(), player.getName(), reason, executor.getUniqueId(), executor.getName());
    }

    /**
     * Kick a player.
     *
     * @param playerUUID The UUID of the player to kick
     * @param playerName The name of the player to kick
     * @param reason The reason for the kick
     * @param executorUUID The UUID of the executor
     * @param executorName The name of the executor
     * @return The created punishment, or null if failed
     */
    public Punishment kickPlayer(UUID playerUUID, String playerName, String reason, UUID executorUUID, String executorName) {
        return plugin.getPunishmentManager().applyPunishment(
                playerUUID,
                playerName,
                PunishmentType.KICK,
                reason,
                -1,
                executorUUID,
                executorName
        );
    }

    /**
     * Warn a player.
     *
     * @param player The player to warn
     * @param reason The reason for the warning
     * @param executor The player executing the warning
     * @return The created punishment, or null if failed
     */
    public Punishment warnPlayer(Player player, String reason, Player executor) {
        return warnPlayer(player.getUniqueId(), player.getName(), reason, executor.getUniqueId(), executor.getName());
    }

    /**
     * Warn a player.
     *
     * @param playerUUID The UUID of the player to warn
     * @param playerName The name of the player to warn
     * @param reason The reason for the warning
     * @param executorUUID The UUID of the executor
     * @param executorName The name of the executor
     * @return The created punishment, or null if failed
     */
    public Punishment warnPlayer(UUID playerUUID, String playerName, String reason, UUID executorUUID, String executorName) {
        return plugin.getPunishmentManager().applyPunishment(
                playerUUID,
                playerName,
                PunishmentType.WARN,
                reason,
                -1,
                executorUUID,
                executorName
        );
    }

    /**
     * Remove a punishment by ID.
     *
     * @param punishmentId The ID of the punishment to remove
     * @param executor The player executing the removal
     * @param reason The reason for the removal
     * @return true if successful
     */
    public boolean removePunishment(UUID punishmentId, Player executor, String reason) {
        return removePunishment(punishmentId, executor.getUniqueId(), executor.getName(), reason);
    }

    /**
     * Remove a punishment by ID.
     *
     * @param punishmentId The ID of the punishment to remove
     * @param executorUUID The UUID of the executor
     * @param executorName The name of the executor
     * @param reason The reason for the removal
     * @return true if successful
     */
    public boolean removePunishment(UUID punishmentId, UUID executorUUID, String executorName, String reason) {
        return plugin.getPunishmentManager().removePunishment(
                punishmentId,
                executorUUID,
                executorName,
                reason
        );
    }

    /**
     * Get a punishment by ID.
     *
     * @param id The ID of the punishment
     * @return The punishment, or null if not found
     */
    public Punishment getPunishment(UUID id) {
        return plugin.getPunishmentStorage().getPunishment(id);
    }

    /**
     * Get all punishments for a player.
     *
     * @param player The player
     * @return List of punishments
     */
    public List<Punishment> getPlayerPunishments(Player player) {
        return getPlayerPunishments(player.getUniqueId());
    }

    /**
     * Get all punishments for a player.
     *
     * @param playerUUID The UUID of the player
     * @return List of punishments
     */
    public List<Punishment> getPlayerPunishments(UUID playerUUID) {
        return plugin.getPunishmentManager().getPlayerPunishments(playerUUID);
    }

    /**
     * Get active punishments for a player.
     *
     * @param player The player
     * @return List of active punishments
     */
    public List<Punishment> getActivePunishments(Player player) {
        return getActivePunishments(player.getUniqueId());
    }

    /**
     * Get active punishments for a player.
     *
     * @param playerUUID The UUID of the player
     * @return List of active punishments
     */
    public List<Punishment> getActivePunishments(UUID playerUUID) {
        return plugin.getPunishmentManager().getActivePunishments(playerUUID);
    }

    /**
     * Get all punishments in the system.
     *
     * @return List of all punishments
     */
    public List<Punishment> getAllPunishments() {
        return plugin.getPunishmentStorage().getAllPunishments();
    }

    /**
     * Get punishment history for a player.
     *
     * @param player The player
     * @return List of history entries
     */
    public List<HistoryEntry> getPlayerHistory(Player player) {
        return getPlayerHistory(player.getUniqueId());
    }

    /**
     * Get punishment history for a player.
     *
     * @param playerUUID The UUID of the player
     * @return List of history entries
     */
    public List<HistoryEntry> getPlayerHistory(UUID playerUUID) {
        return plugin.getPunishmentStorage().getPlayerHistory(playerUUID);
    }

    /**
     * Reset warnings for a player.
     *
     * @param player The player
     */
    public void resetWarnings(Player player) {
        resetWarnings(player.getUniqueId());
    }

    /**
     * Reset warnings for a player.
     *
     * @param playerUUID The UUID of the player
     */
    public void resetWarnings(UUID playerUUID) {
        plugin.getPunishmentManager().resetWarnings(playerUUID);
    }

    /**
     * Apply an IP ban.
     *
     * @param ip The IP address to ban
     * @param reason The reason for the ban
     * @param executor The player executing the ban
     * @return The created punishment, or null if failed
     */
    public Punishment banIP(String ip, String reason, Player executor) {
        return banIP(ip, reason, executor.getUniqueId(), executor.getName());
    }

    /**
     * Apply an IP ban.
     *
     * @param ip The IP address to ban
     * @param reason The reason for the ban
     * @param executorUUID The UUID of the executor
     * @param executorName The name of the executor
     * @return The created punishment, or null if failed
     */
    public Punishment banIP(String ip, String reason, UUID executorUUID, String executorName) {
        Punishment punishment = new Punishment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "IP: " + ip,
                PunishmentType.IP_BAN,
                reason,
                -1,
                executorUUID,
                executorName
        );
        punishment.setIpAddress(ip);
        plugin.getPunishmentStorage().savePunishment(punishment);
        return punishment;
    }

    /**
     * Unban an IP.
     *
     * @param ip The IP address to unban
     * @param executor The player executing the unban
     * @param reason The reason for the unban
     * @return true if successful
     */
    public boolean unbanIP(String ip, Player executor, String reason) {
        return unbanIP(ip, executor.getUniqueId(), executor.getName(), reason);
    }

    /**
     * Unban an IP.
     *
     * @param ip The IP address to unban
     * @param executorUUID The UUID of the executor
     * @param executorName The name of the executor
     * @param reason The reason for the unban
     * @return true if successful
     */
    public boolean unbanIP(String ip, UUID executorUUID, String executorName, String reason) {
        // Find the IP ban
        for (Punishment p : plugin.getPunishmentManager().getAllActivePunishments()) {
            if (p.getType() == PunishmentType.IP_BAN && ip.equals(p.getIpAddress())) {
                return plugin.getPunishmentManager().removePunishment(
                        p.getId(),
                        executorUUID,
                        executorName,
                        reason
                );
            }
        }
        return false;
    }

    /**
     * Check if a player is exempt from punishments.
     *
     * @param player The player to check
     * @param type The punishment type
     * @return true if exempt
     */
    public boolean isExempt(Player player, PunishmentType type) {
        String bypassPermission = "sxbans.bypass." + type.name().toLowerCase().replace("_", "");
        return player.hasPermission(bypassPermission) || player.hasPermission("sxbans.bypass.*");
    }
}
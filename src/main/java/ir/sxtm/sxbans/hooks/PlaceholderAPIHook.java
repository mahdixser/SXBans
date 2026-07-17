package ir.sxtm.sxbans.hooks;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * PlaceholderAPI integration for SX Bans.
 * Provides placeholders for punishment information.
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final SXBans plugin;

    // Placeholders:
    // %sxbans_banned% - Returns "true" or "false"
    // %sxbans_muted% - Returns "true" or "false"
    // %sxbans_warnings% - Returns number of warnings
    // %sxbans_ban_reason% - Returns ban reason
    // %sxbans_mute_reason% - Returns mute reason
    // %sxbans_ban_duration% - Returns ban duration
    // %sxbans_mute_duration% - Returns mute duration
    // %sxbans_ban_executor% - Returns ban executor name
    // %sxbans_mute_executor% - Returns mute executor name
    // %sxbans_ban_time% - Returns ban time
    // %sxbans_mute_time% - Returns mute time
    // %sxbans_total_punishments% - Returns total punishments
    // %sxbans_active_punishments% - Returns active punishments

    public PlaceholderAPIHook(SXBans plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "sxbans";
    }

    @Override
    public String getAuthor() {
        return "xsergod";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }

        UUID uuid = player.getUniqueId();

        switch (params.toLowerCase()) {
            case "banned":
                return String.valueOf(plugin.getPunishmentManager().isPlayerBanned(uuid));

            case "muted":
                return String.valueOf(plugin.getPunishmentManager().isPlayerMuted(uuid));

            case "warnings":
                return String.valueOf(plugin.getPunishmentManager().getWarningCount(uuid));

            case "ban_reason": {
                Punishment ban = plugin.getPunishmentManager().getActiveBan(uuid);
                return ban != null ? ban.getReason() : "Not banned";
            }

            case "mute_reason": {
                Punishment mute = plugin.getPunishmentManager().getActiveMute(uuid);
                return mute != null ? mute.getReason() : "Not muted";
            }

            case "ban_duration": {
                Punishment ban = plugin.getPunishmentManager().getActiveBan(uuid);
                return ban != null ? ban.getFormattedTimeRemaining() : "N/A";
            }

            case "mute_duration": {
                Punishment mute = plugin.getPunishmentManager().getActiveMute(uuid);
                return mute != null ? mute.getFormattedTimeRemaining() : "N/A";
            }

            case "ban_executor": {
                Punishment ban = plugin.getPunishmentManager().getActiveBan(uuid);
                return ban != null ? ban.getExecutorName() : "N/A";
            }

            case "mute_executor": {
                Punishment mute = plugin.getPunishmentManager().getActiveMute(uuid);
                return mute != null ? mute.getExecutorName() : "N/A";
            }

            case "ban_time": {
                Punishment ban = plugin.getPunishmentManager().getActiveBan(uuid);
                return ban != null ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date(ban.getStartTime())) : "N/A";
            }

            case "mute_time": {
                Punishment mute = plugin.getPunishmentManager().getActiveMute(uuid);
                return mute != null ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date(mute.getStartTime())) : "N/A";
            }

            case "total_punishments":
                return String.valueOf(plugin.getPunishmentManager().getPlayerPunishments(uuid).size());

            case "active_punishments":
                return String.valueOf(plugin.getPunishmentManager().getActivePunishments(uuid).size());

            default:
                return null;
        }
    }

    /**
     * Handle placeholder requests for offline players.
     * Note: This method may not exist in all versions of PlaceholderAPI.
     * Remove @Override if this method doesn't exist in the parent class.
     */
    public String onPlaceholderRequest(OfflinePlayer player, String params) {
        if (player == null || !player.hasPlayedBefore()) {
            return "";
        }

        UUID uuid = player.getUniqueId();

        switch (params.toLowerCase()) {
            case "banned":
                return String.valueOf(plugin.getPunishmentManager().isPlayerBanned(uuid));

            case "muted":
                return String.valueOf(plugin.getPunishmentManager().isPlayerMuted(uuid));

            case "warnings":
                return String.valueOf(plugin.getPunishmentManager().getWarningCount(uuid));

            case "total_punishments":
                return String.valueOf(plugin.getPunishmentManager().getPlayerPunishments(uuid).size());

            default:
                return null;
        }
    }

    /**
     * Register the expansion with PlaceholderAPI.
     */
    public void registerExpansion() {
        if (this.register()) {
            plugin.getSXBansLogger().info("PlaceholderAPI expansion registered");
        } else {
            plugin.getSXBansLogger().warning("Failed to register PlaceholderAPI expansion");
        }
    }

    /**
     * Format a message with placeholders.
     *
     * @param player The player
     * @param message The message to format
     * @return The formatted message
     */
    public String formatPlaceholders(Player player, String message) {
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message);
    }
}
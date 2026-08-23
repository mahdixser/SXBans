package ir.sxtm.sxbans.hooks;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final SXBans plugin;

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

    public void registerExpansion() {
        if (this.register()) {
            plugin.getSXBansLogger().info("PlaceholderAPI expansion registered");
        } else {
            plugin.getSXBansLogger().warning("Failed to register PlaceholderAPI expansion");
        }
    }

    public String formatPlaceholders(Player player, String message) {
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message);
    }
}
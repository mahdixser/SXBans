package ir.sxtm.sxbans.api;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import ir.sxtm.sxbans.models.HistoryEntry;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PunishmentAPI {
    private final SXBans plugin;

    public PunishmentAPI(SXBans plugin) {
        this.plugin = plugin;
    }

    public Punishment banPlayer(Player player, String reason, Player executor) {
        return banPlayer(player.getUniqueId(), player.getName(), reason, executor.getUniqueId(), executor.getName());
    }

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

    public Punishment tempBanPlayer(Player player, String reason, long duration, Player executor) {
        return tempBanPlayer(player.getUniqueId(), player.getName(), reason, duration, executor.getUniqueId(), executor.getName());
    }

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

    public boolean unbanPlayer(Player player, Player executor, String reason) {
        return unbanPlayer(player.getUniqueId(), executor.getUniqueId(), executor.getName(), reason);
    }

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

    public Punishment mutePlayer(Player player, String reason, Player executor) {
        return mutePlayer(player.getUniqueId(), player.getName(), reason, executor.getUniqueId(), executor.getName());
    }

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

    public Punishment tempMutePlayer(Player player, String reason, long duration, Player executor) {
        return tempMutePlayer(player.getUniqueId(), player.getName(), reason, duration, executor.getUniqueId(), executor.getName());
    }

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

    public boolean unmutePlayer(Player player, Player executor, String reason) {
        return unmutePlayer(player.getUniqueId(), executor.getUniqueId(), executor.getName(), reason);
    }

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

    public Punishment kickPlayer(Player player, String reason, Player executor) {
        return kickPlayer(player.getUniqueId(), player.getName(), reason, executor.getUniqueId(), executor.getName());
    }

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

    public Punishment warnPlayer(Player player, String reason, Player executor) {
        return warnPlayer(player.getUniqueId(), player.getName(), reason, executor.getUniqueId(), executor.getName());
    }

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

    public boolean removePunishment(UUID punishmentId, Player executor, String reason) {
        return removePunishment(punishmentId, executor.getUniqueId(), executor.getName(), reason);
    }

    public boolean removePunishment(UUID punishmentId, UUID executorUUID, String executorName, String reason) {
        return plugin.getPunishmentManager().removePunishment(
                punishmentId,
                executorUUID,
                executorName,
                reason
        );
    }

    public Punishment getPunishment(UUID id) {
        return plugin.getPunishmentStorage().getPunishment(id);
    }

    public List<Punishment> getPlayerPunishments(Player player) {
        return getPlayerPunishments(player.getUniqueId());
    }

    public List<Punishment> getPlayerPunishments(UUID playerUUID) {
        return plugin.getPunishmentManager().getPlayerPunishments(playerUUID);
    }

    public List<Punishment> getActivePunishments(Player player) {
        return getActivePunishments(player.getUniqueId());
    }

    public List<Punishment> getActivePunishments(UUID playerUUID) {
        return plugin.getPunishmentManager().getActivePunishments(playerUUID);
    }

    public List<Punishment> getAllPunishments() {
        return plugin.getPunishmentStorage().getAllPunishments();
    }

    public List<HistoryEntry> getPlayerHistory(Player player) {
        return getPlayerHistory(player.getUniqueId());
    }

    public List<HistoryEntry> getPlayerHistory(UUID playerUUID) {
        return plugin.getPunishmentStorage().getPlayerHistory(playerUUID);
    }

    public void resetWarnings(Player player) {
        resetWarnings(player.getUniqueId());
    }

    public void resetWarnings(UUID playerUUID) {
        plugin.getPunishmentManager().resetWarnings(playerUUID);
    }

    public Punishment banIP(String ip, String reason, Player executor) {
        return banIP(ip, reason, executor.getUniqueId(), executor.getName());
    }

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

    public boolean unbanIP(String ip, Player executor, String reason) {
        return unbanIP(ip, executor.getUniqueId(), executor.getName(), reason);
    }

    public boolean unbanIP(String ip, UUID executorUUID, String executorName, String reason) {

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

    public boolean isExempt(Player player, PunishmentType type) {
        String bypassPermission = "sxbans.bypass." + type.name().toLowerCase().replace("_", "");
        return player.hasPermission(bypassPermission) || player.hasPermission("sxbans.bypass.*");
    }
}
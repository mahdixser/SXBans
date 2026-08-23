package ir.sxtm.sxbans.punishment;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import ir.sxtm.sxbans.utils.TimeUtils;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PunishmentFactory {
    private final SXBans plugin;

    public PunishmentFactory(SXBans plugin) {
        this.plugin = plugin;
    }

    public Punishment createBan(Player target, String reason, UUID executorUUID, String executorName) {
        return createPunishment(target, PunishmentType.BAN, reason, -1, executorUUID, executorName);
    }

    public Punishment createTempBan(Player target, String reason, long duration, UUID executorUUID, String executorName) {
        return createPunishment(target, PunishmentType.TEMP_BAN, reason, duration, executorUUID, executorName);
    }

    public Punishment createMute(Player target, String reason, UUID executorUUID, String executorName) {
        return createPunishment(target, PunishmentType.MUTE, reason, -1, executorUUID, executorName);
    }

    public Punishment createTempMute(Player target, String reason, long duration, UUID executorUUID, String executorName) {
        return createPunishment(target, PunishmentType.TEMP_MUTE, reason, duration, executorUUID, executorName);
    }

    public Punishment createKick(Player target, String reason, UUID executorUUID, String executorName) {
        return createPunishment(target, PunishmentType.KICK, reason, -1, executorUUID, executorName);
    }

    public Punishment createWarn(Player target, String reason, UUID executorUUID, String executorName) {
        return createPunishment(target, PunishmentType.WARN, reason, -1, executorUUID, executorName);
    }

    public Punishment createIPBan(String ip, String playerName, String reason, UUID executorUUID, String executorName) {
        Punishment punishment = new Punishment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                playerName != null ? playerName : "Unknown",
                PunishmentType.IP_BAN,
                reason,
                -1,
                executorUUID,
                executorName
        );
        punishment.setIpAddress(ip);
        return punishment;
    }

    public Punishment createIPMute(String ip, String playerName, String reason, UUID executorUUID, String executorName) {
        Punishment punishment = new Punishment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                playerName != null ? playerName : "Unknown",
                PunishmentType.IP_MUTE,
                reason,
                -1,
                executorUUID,
                executorName
        );
        punishment.setIpAddress(ip);
        return punishment;
    }

    private Punishment createPunishment(Player target, PunishmentType type, String reason,
                                        long duration, UUID executorUUID, String executorName) {
        Punishment punishment = new Punishment(
                UUID.randomUUID(),
                target.getUniqueId(),
                target.getName(),
                type,
                reason,
                duration,
                executorUUID,
                executorName
        );

        punishment.setIpAddress(target.getAddress().getAddress().getHostAddress());
        punishment.setWorldName(target.getWorld().getName());
        punishment.setServerName(plugin.getServer().getName());
        punishment.setClientVersion(getClientVersion(target));
        punishment.setClientBrand(getClientBrand(target));

        return punishment;
    }

    private String getClientVersion(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object client = handle.getClass().getField("client").get(handle);
            return client.getClass().getSimpleName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String getClientBrand(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object client = handle.getClass().getField("client").get(handle);
            String brand = client.getClass().getMethod("getBrand").invoke(client).toString();
            return brand;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public Punishment createFromTemplate(String templateName, Player target, UUID executorUUID, String executorName) {

        return createBan(target, "Template: " + templateName, executorUUID, executorName);
    }
}
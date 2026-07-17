package ir.sxtm.sxbans.web.services;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import ir.sxtm.sxbans.models.Punishment.PunishmentStatus;
import ir.sxtm.sxbans.utils.TimeUtils;
import ir.sxtm.sxbans.web.models.WebPunishment;
import ir.sxtm.sxbans.web.models.WebResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.stream.Collectors;

public class PunishmentService {
    private final SXBans plugin;

    public PunishmentService(SXBans plugin) {
        this.plugin = plugin;
    }

    public WebResponse getPunishments(int page, int limit, String type, String status, String player, String sort, String order) {
        List<Punishment> punishments = plugin.getPunishmentStorage().getAllPunishments();

        // Filter
        if (type != null && !type.isEmpty()) {
            try {
                PunishmentType filterType = PunishmentType.valueOf(type.toUpperCase());
                punishments = punishments.stream()
                        .filter(p -> p.getType() == filterType)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        if (status != null && !status.isEmpty()) {
            try {
                PunishmentStatus filterStatus = PunishmentStatus.valueOf(status.toUpperCase());
                punishments = punishments.stream()
                        .filter(p -> p.getStatus() == filterStatus)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        if (player != null && !player.isEmpty()) {
            punishments = punishments.stream()
                    .filter(p -> p.getPlayerName().toLowerCase().contains(player.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Sort
        if (sort != null) {
            boolean ascending = "asc".equalsIgnoreCase(order);
            punishments.sort((a, b) -> {
                int comparison = 0;
                switch (sort.toLowerCase()) {
                    case "player":
                        comparison = a.getPlayerName().compareTo(b.getPlayerName());
                        break;
                    case "type":
                        comparison = a.getType().name().compareTo(b.getType().name());
                        break;
                    case "reason":
                        comparison = a.getReason().compareTo(b.getReason());
                        break;
                    case "executor":
                        comparison = a.getExecutorName().compareTo(b.getExecutorName());
                        break;
                    case "duration":
                        comparison = Long.compare(a.getDuration(), b.getDuration());
                        break;
                    case "date":
                    default:
                        comparison = Long.compare(a.getCreatedAt(), b.getCreatedAt());
                        break;
                }
                return ascending ? comparison : -comparison;
            });
        } else {
            punishments.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        }

        // Paginate
        int total = punishments.size();
        int start = (page - 1) * limit;
        int end = Math.min(start + limit, total);
        List<Punishment> paged = start < total ? punishments.subList(start, end) : new ArrayList<>();

        List<WebPunishment> webPunishments = paged.stream()
                .map(WebPunishment::new)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("punishments", webPunishments);
        response.put("total", total);
        response.put("page", page);
        response.put("totalPages", (int) Math.ceil(total / (double) limit));

        return new WebResponse(true, "Success", response);
    }

    public WebResponse getPunishment(UUID id) {
        Punishment punishment = plugin.getPunishmentStorage().getPunishment(id);
        if (punishment == null) {
            return new WebResponse(false, "Punishment not found");
        }

        WebPunishment webPunishment = new WebPunishment(punishment);
        return new WebResponse(true, "Success", webPunishment);
    }

    public WebResponse createPunishment(String playerName, String typeStr, String reason, String durationStr, String executorName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            return new WebResponse(false, "Player not found");
        }

        UUID targetUUID = target.getUniqueId();
        String realName = target.getName() != null ? target.getName() : playerName;

        try {
            PunishmentType type = PunishmentType.valueOf(typeStr.toUpperCase());

            if (type == PunishmentType.BAN || type == PunishmentType.TEMP_BAN) {
                if (plugin.getPunishmentManager().isPlayerBanned(targetUUID)) {
                    return new WebResponse(false, "Player is already banned");
                }
            }

            if (type == PunishmentType.MUTE || type == PunishmentType.TEMP_MUTE) {
                if (plugin.getPunishmentManager().isPlayerMuted(targetUUID)) {
                    return new WebResponse(false, "Player is already muted");
                }
            }
        } catch (IllegalArgumentException e) {
            return new WebResponse(false, "Invalid punishment type");
        }

        long duration = -1;
        if (durationStr != null && !durationStr.isEmpty() &&
                !durationStr.equals("-1") && !durationStr.equalsIgnoreCase("permanent")) {
            duration = TimeUtils.parseTime(durationStr);
            if (duration <= 0) {
                return new WebResponse(false, "Invalid duration format");
            }
        }

        try {
            PunishmentType type = PunishmentType.valueOf(typeStr.toUpperCase());
            UUID executorUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

            Punishment punishment = plugin.getPunishmentManager().applyPunishment(
                    targetUUID,
                    realName,
                    type,
                    reason,
                    duration,
                    executorUUID,
                    executorName != null ? executorName : "WebPanel"
            );

            if (punishment != null) {
                // WebSocket غیرفعال است، فقط لاگ می‌زنیم
                if (plugin.getWebServer() != null && plugin.getWebServer().isRunning()) {
                    plugin.getSXBansLogger().info("Punishment created via WebPanel: " + punishment.getId());
                }

                return new WebResponse(true, "Punishment applied successfully", new WebPunishment(punishment));
            } else {
                return new WebResponse(false, "Failed to apply punishment");
            }

        } catch (Exception e) {
            return new WebResponse(false, "Error: " + e.getMessage());
        }
    }

    public WebResponse removePunishment(UUID id, String removerName, String reason) {
        Punishment punishment = plugin.getPunishmentStorage().getPunishment(id);
        if (punishment == null) {
            return new WebResponse(false, "Punishment not found");
        }

        if (punishment.getStatus() != PunishmentStatus.ACTIVE) {
            return new WebResponse(false, "Punishment is not active");
        }

        UUID removerUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

        boolean success = plugin.getPunishmentManager().removePunishment(
                id,
                removerUUID,
                removerName != null ? removerName : "WebPanel",
                reason != null ? reason : "Removed via web panel"
        );

        if (success) {
            if (plugin.getWebServer() != null && plugin.getWebServer().isRunning()) {
                plugin.getSXBansLogger().info("Punishment removed via WebPanel: " + punishment.getId());
            }

            return new WebResponse(true, "Punishment removed successfully");
        } else {
            return new WebResponse(false, "Failed to remove punishment");
        }
    }

    public WebResponse expirePunishment(UUID id) {
        Punishment punishment = plugin.getPunishmentStorage().getPunishment(id);
        if (punishment == null) {
            return new WebResponse(false, "Punishment not found");
        }

        if (punishment.getStatus() != PunishmentStatus.ACTIVE) {
            return new WebResponse(false, "Punishment is not active");
        }

        if (punishment.isPermanent()) {
            return new WebResponse(false, "Cannot expire permanent punishment");
        }

        punishment.setEndTime(System.currentTimeMillis());
        punishment.setStatus(PunishmentStatus.EXPIRED);
        punishment.setUpdatedAt(System.currentTimeMillis());

        plugin.getPunishmentStorage().updatePunishment(punishment);

        return new WebResponse(true, "Punishment expired successfully");
    }

    public WebResponse reduceDuration(UUID id, long amount) {
        Punishment punishment = plugin.getPunishmentStorage().getPunishment(id);
        if (punishment == null) {
            return new WebResponse(false, "Punishment not found");
        }

        if (punishment.isPermanent()) {
            return new WebResponse(false, "Cannot reduce duration of permanent punishment");
        }

        long newEndTime = punishment.getEndTime() - amount;
        if (newEndTime <= System.currentTimeMillis()) {
            newEndTime = System.currentTimeMillis() + 1000;
        }

        punishment.setEndTime(newEndTime);
        punishment.setUpdatedAt(System.currentTimeMillis());

        plugin.getPunishmentStorage().updatePunishment(punishment);

        Map<String, Object> response = new HashMap<>();
        response.put("newDuration", punishment.getFormattedTimeRemaining());
        response.put("newEndTime", punishment.getEndTime());

        return new WebResponse(true, "Duration reduced successfully", response);
    }

    public WebResponse increaseDuration(UUID id, long amount) {
        Punishment punishment = plugin.getPunishmentStorage().getPunishment(id);
        if (punishment == null) {
            return new WebResponse(false, "Punishment not found");
        }

        if (punishment.isPermanent()) {
            return new WebResponse(false, "Cannot increase duration of permanent punishment");
        }

        long newEndTime = punishment.getEndTime() + amount;
        punishment.setEndTime(newEndTime);
        punishment.setUpdatedAt(System.currentTimeMillis());

        plugin.getPunishmentStorage().updatePunishment(punishment);

        Map<String, Object> response = new HashMap<>();
        response.put("newDuration", punishment.getFormattedTimeRemaining());
        response.put("newEndTime", punishment.getEndTime());

        return new WebResponse(true, "Duration increased successfully", response);
    }

    public WebResponse getStats() {
        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allPunishments.size());

        long active = allPunishments.stream().filter(Punishment::isActive).count();
        stats.put("active", active);

        long expired = allPunishments.stream()
                .filter(p -> p.getStatus() == PunishmentStatus.EXPIRED)
                .count();
        stats.put("expired", expired);

        long removed = allPunishments.stream()
                .filter(p -> p.getStatus() == PunishmentStatus.REMOVED)
                .count();
        stats.put("removed", removed);

        Map<String, Long> byType = allPunishments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getType().name(),
                        Collectors.counting()
                ));
        stats.put("byType", byType);

        long today = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        long todayCount = allPunishments.stream()
                .filter(p -> p.getCreatedAt() > today)
                .count();
        stats.put("today", todayCount);

        long week = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        long weekCount = allPunishments.stream()
                .filter(p -> p.getCreatedAt() > week)
                .count();
        stats.put("thisWeek", weekCount);

        return new WebResponse(true, "Success", stats);
    }
}
package ir.sxtm.sxbans.punishment;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BanAppealManager {
    private final SXBans plugin;
    private final Map<UUID, AppealRequest> appeals;
    private final Map<UUID, List<AppealRequest>> playerAppeals;

    public BanAppealManager(SXBans plugin) {
        this.plugin = plugin;
        this.appeals = new ConcurrentHashMap<>();
        this.playerAppeals = new ConcurrentHashMap<>();
    }

    public void submitAppeal(UUID playerUUID, String playerName, UUID punishmentId, String reason) {
        AppealRequest request = new AppealRequest(
                UUID.randomUUID(),
                playerUUID,
                playerName,
                punishmentId,
                reason,
                System.currentTimeMillis(),
                AppealStatus.PENDING
        );

        appeals.put(request.getId(), request);
        playerAppeals.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(request);

        notifyStaff(request);
    }

    public boolean reviewAppeal(UUID appealId, UUID reviewerUUID, String reviewerName,
                                AppealStatus status, String response) {
        AppealRequest request = appeals.get(appealId);
        if (request == null) return false;

        request.setStatus(status);
        request.setReviewerUUID(reviewerUUID);
        request.setReviewerName(reviewerName);
        request.setResponse(response);
        request.setReviewedAt(System.currentTimeMillis());

        if (status == AppealStatus.APPROVED) {

            Punishment punishment = plugin.getPunishmentStorage().getPunishment(request.getPunishmentId());
            if (punishment != null) {
                plugin.getPunishmentManager().removePunishment(
                        punishment.getId(),
                        reviewerUUID,
                        reviewerName,
                        "Appeal approved: " + response
                );
            }
        }

        notifyPlayer(request);

        return true;
    }

    public List<AppealRequest> getPlayerAppeals(UUID playerUUID) {
        return playerAppeals.getOrDefault(playerUUID, Collections.emptyList());
    }

    public List<AppealRequest> getPendingAppeals() {
        List<AppealRequest> pending = new ArrayList<>();
        for (AppealRequest request : appeals.values()) {
            if (request.getStatus() == AppealStatus.PENDING) {
                pending.add(request);
            }
        }
        return pending;
    }

    public AppealRequest getAppeal(UUID id) {
        return appeals.get(id);
    }

    private void notifyStaff(AppealRequest request) {

        String message = plugin.getMessagesManager().getColoredMessage("appeal.submitted",
                Map.of("player", request.getPlayerName(), "reason", request.getReason()));

        plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("sxbans.appeals.view"))
                .forEach(p -> p.sendMessage(message));
    }

    private void notifyPlayer(AppealRequest request) {

        String message = plugin.getMessagesManager().getColoredMessage("appeal." +
                        request.getStatus().name().toLowerCase(),
                Map.of("response", request.getResponse() != null ? request.getResponse() : ""));

        Player player = plugin.getServer().getPlayer(request.getPlayerUUID());
        if (player != null) {
            player.sendMessage(message);
        }
    }

    public enum AppealStatus {
        PENDING,
        APPROVED,
        DENIED,
        CLOSED
    }

    public static class AppealRequest {
        private final UUID id;
        private final UUID playerUUID;
        private final String playerName;
        private final UUID punishmentId;
        private final String reason;
        private final long submittedAt;
        private AppealStatus status;
        private UUID reviewerUUID;
        private String reviewerName;
        private String response;
        private long reviewedAt;

        public AppealRequest(UUID id, UUID playerUUID, String playerName, UUID punishmentId,
                             String reason, long submittedAt, AppealStatus status) {
            this.id = id;
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.punishmentId = punishmentId;
            this.reason = reason;
            this.submittedAt = submittedAt;
            this.status = status;
        }

        public UUID getId() { return id; }
        public UUID getPlayerUUID() { return playerUUID; }
        public String getPlayerName() { return playerName; }
        public UUID getPunishmentId() { return punishmentId; }
        public String getReason() { return reason; }
        public long getSubmittedAt() { return submittedAt; }
        public AppealStatus getStatus() { return status; }
        public void setStatus(AppealStatus status) { this.status = status; }
        public UUID getReviewerUUID() { return reviewerUUID; }
        public void setReviewerUUID(UUID reviewerUUID) { this.reviewerUUID = reviewerUUID; }
        public String getReviewerName() { return reviewerName; }
        public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        public long getReviewedAt() { return reviewedAt; }
        public void setReviewedAt(long reviewedAt) { this.reviewedAt = reviewedAt; }
    }
}
package ir.sxtm.sxbans.web.models;

import ir.sxtm.sxbans.models.Punishment;

import java.util.UUID;

public class WebPunishment {
    private String id;
    private String playerName;
    private String playerUuid;
    private String ipAddress;
    private String type;
    private String reason;
    private String duration;
    private String startTime;
    private String endTime;
    private String status;
    private String executorName;
    private String executorUuid;
    private String removerName;
    private String removeReason;
    private String timeRemaining;
    private boolean isActive;
    private boolean isPermanent;
    private boolean isExpired;

    public WebPunishment(Punishment punishment) {
        this.id = punishment.getId().toString();
        this.playerName = punishment.getPlayerName();
        this.playerUuid = punishment.getPlayerUUID().toString();
        this.ipAddress = punishment.getIpAddress();
        this.type = punishment.getType().getDisplayName();
        this.reason = punishment.getReason();
        this.duration = punishment.isPermanent() ? "Permanent" : punishment.getFormattedTimeRemaining();
        this.startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(punishment.getStartTime()));
        this.endTime = punishment.isPermanent() ? "Never" :
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date(punishment.getEndTime()));
        this.status = punishment.getStatus().getDisplayName();
        this.executorName = punishment.getExecutorName();
        this.executorUuid = punishment.getExecutorUUID().toString();
        if (punishment.getRemoverName() != null) {
            this.removerName = punishment.getRemoverName();
        }
        if (punishment.getRemoveReason() != null) {
            this.removeReason = punishment.getRemoveReason();
        }
        this.timeRemaining = punishment.getFormattedTimeRemaining();
        this.isActive = punishment.isActive();
        this.isPermanent = punishment.isPermanent();
        this.isExpired = punishment.isExpired();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExecutorName() { return executorName; }
    public void setExecutorName(String executorName) { this.executorName = executorName; }
    public String getExecutorUuid() { return executorUuid; }
    public void setExecutorUuid(String executorUuid) { this.executorUuid = executorUuid; }
    public String getRemoverName() { return removerName; }
    public void setRemoverName(String removerName) { this.removerName = removerName; }
    public String getRemoveReason() { return removeReason; }
    public void setRemoveReason(String removeReason) { this.removeReason = removeReason; }
    public String getTimeRemaining() { return timeRemaining; }
    public void setTimeRemaining(String timeRemaining) { this.timeRemaining = timeRemaining; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public boolean isPermanent() { return isPermanent; }
    public void setPermanent(boolean permanent) { isPermanent = permanent; }
    public boolean isExpired() { return isExpired; }
    public void setExpired(boolean expired) { isExpired = expired; }
}
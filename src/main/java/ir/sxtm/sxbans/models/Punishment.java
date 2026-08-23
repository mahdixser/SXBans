package ir.sxtm.sxbans.models;

import java.time.Instant;
import java.util.UUID;

public class Punishment {
    private final UUID id;
    private final UUID playerUUID;
    private final String playerName;
    private String ipAddress;
    private final PunishmentType type;
    private final String reason;
    private final long duration;
    private final long startTime;
    private long endTime;
    private PunishmentStatus status;
    private final UUID executorUUID;
    private final String executorName;
    private UUID removerUUID;
    private String removerName;
    private long removedTime;
    private String removeReason;
    private final long createdAt;
    private long updatedAt;
    private String serverName;
    private String worldName;
    private String ipCountry;
    private String ipCity;
    private String clientVersion;
    private String clientBrand;

    public Punishment() {
        this.id = null;
        this.playerUUID = null;
        this.playerName = null;
        this.type = null;
        this.reason = null;
        this.duration = 0;
        this.startTime = 0;
        this.executorUUID = null;
        this.executorName = null;
        this.createdAt = 0;
    }

    public Punishment(UUID id, UUID playerUUID, String playerName, PunishmentType type,
                      String reason, long duration, UUID executorUUID, String executorName) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.type = type;
        this.reason = reason;
        this.duration = duration;
        this.executorUUID = executorUUID;
        this.executorName = executorName;
        this.startTime = Instant.now().toEpochMilli();
        this.status = PunishmentStatus.ACTIVE;
        this.createdAt = startTime;
        this.updatedAt = startTime;

        if (duration > 0) {
            this.endTime = startTime + duration;
        } else {
            this.endTime = -1;
        }
    }

    public UUID getId() { return id; }
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public PunishmentType getType() { return type; }
    public String getReason() { return reason; }
    public long getDuration() { return duration; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public PunishmentStatus getStatus() { return status; }
    public void setStatus(PunishmentStatus status) { this.status = status; }
    public UUID getExecutorUUID() { return executorUUID; }
    public String getExecutorName() { return executorName; }
    public UUID getRemoverUUID() { return removerUUID; }
    public void setRemoverUUID(UUID removerUUID) { this.removerUUID = removerUUID; }
    public String getRemoverName() { return removerName; }
    public void setRemoverName(String removerName) { this.removerName = removerName; }
    public long getRemovedTime() { return removedTime; }
    public void setRemovedTime(long removedTime) { this.removedTime = removedTime; }
    public String getRemoveReason() { return removeReason; }
    public void setRemoveReason(String removeReason) { this.removeReason = removeReason; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public String getIpCountry() { return ipCountry; }
    public void setIpCountry(String ipCountry) { this.ipCountry = ipCountry; }
    public String getIpCity() { return ipCity; }
    public void setIpCity(String ipCity) { this.ipCity = ipCity; }
    public String getClientVersion() { return clientVersion; }
    public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }
    public String getClientBrand() { return clientBrand; }
    public void setClientBrand(String clientBrand) { this.clientBrand = clientBrand; }

    public boolean isPermanent() {
        return duration <= 0;
    }

    public boolean isExpired() {
        if (isPermanent()) return false;
        return Instant.now().toEpochMilli() > endTime;
    }

    public boolean isActive() {
        return status == PunishmentStatus.ACTIVE && !isExpired();
    }

    public long getTimeRemaining() {
        if (isPermanent()) return -1;
        long remaining = endTime - Instant.now().toEpochMilli();
        return remaining > 0 ? remaining : 0;
    }

    public String getFormattedTimeRemaining() {
        if (isPermanent()) return "Permanent";
        long remaining = getTimeRemaining();
        if (remaining <= 0) return "Expired";
        return formatTime(remaining);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    public enum PunishmentType {
        BAN("Ban"),
        IP_BAN("IP Ban"),
        KICK("Kick"),
        IP_KICK("IP Kick"),
        MUTE("Mute"),
        IP_MUTE("IP Mute"),
        TEMP_BAN("Temporary Ban"),
        TEMP_MUTE("Temporary Mute"),
        WARN("Warning"),
        BLACKLIST("Blacklist");

        private final String displayName;

        PunishmentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum PunishmentStatus {
        ACTIVE("Active"),
        REMOVED("Removed"),
        EXPIRED("Expired");

        private final String displayName;

        PunishmentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return "Punishment{" +
                "id=" + id +
                ", playerName='" + playerName + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", duration=" + duration +
                '}';
    }
}
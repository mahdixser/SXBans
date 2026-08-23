package ir.sxtm.sxbans.models;

import java.time.Instant;
import java.util.UUID;

public class HistoryEntry {
    private final UUID id;
    private final UUID playerUUID;
    private final String playerName;
    private final String action;
    private final String details;
    private final UUID executorUUID;
    private final String executorName;
    private final long timestamp;
    private String ipAddress;
    private String serverName;
    private String worldName;

    public HistoryEntry() {
        this.id = null;
        this.playerUUID = null;
        this.playerName = null;
        this.action = null;
        this.details = null;
        this.executorUUID = null;
        this.executorName = null;
        this.timestamp = 0;
    }

    public HistoryEntry(UUID id, UUID playerUUID, String playerName, String action,
                        String details, UUID executorUUID, String executorName) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.action = action;
        this.details = details;
        this.executorUUID = executorUUID;
        this.executorName = executorName;
        this.timestamp = Instant.now().toEpochMilli();
    }

    public UUID getId() { return id; }
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public UUID getExecutorUUID() { return executorUUID; }
    public String getExecutorName() { return executorName; }
    public long getTimestamp() { return timestamp; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public String getFormattedTime() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(timestamp));
    }

    @Override
    public String toString() {
        return "HistoryEntry{" +
                "playerName='" + playerName + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + getFormattedTime() +
                '}';
    }
}
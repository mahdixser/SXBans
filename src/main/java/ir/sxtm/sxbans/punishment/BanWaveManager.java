package ir.sxtm.sxbans.punishment;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BanWaveManager {
    private final SXBans plugin;
    private final List<BanWave> activeWaves;
    private boolean isRunning;

    public BanWaveManager(SXBans plugin) {
        this.plugin = plugin;
        this.activeWaves = new ArrayList<>();
        this.isRunning = false;
    }

    public void startBanWave(String name, String reason, UUID executorUUID, String executorName,
                             List<UUID> targetUUIDs, long duration) {
        BanWave wave = new BanWave(
                UUID.randomUUID(),
                name,
                reason,
                executorUUID,
                executorName,
                targetUUIDs,
                duration,
                System.currentTimeMillis()
        );

        activeWaves.add(wave);

        CompletableFuture.runAsync(() -> {
            isRunning = true;

            for (UUID targetUUID : targetUUIDs) {
                String playerName = getPlayerName(targetUUID);
                if (playerName != null) {
                    Punishment punishment = plugin.getPunishmentManager().applyPunishment(
                            targetUUID,
                            playerName,
                            duration > 0 ? PunishmentType.TEMP_BAN : PunishmentType.BAN,
                            reason,
                            duration,
                            executorUUID,
                            executorName
                    );

                    if (punishment != null) {
                        wave.addAffectedPlayer(targetUUID);
                    }
                }
            }

            wave.setCompletedAt(System.currentTimeMillis());
            isRunning = false;

            plugin.getLogger().info("Ban wave '" + name + "' completed. Affected: " +
                    wave.getAffectedPlayers().size() + " players");
        });
    }

    public void stopBanWave(UUID waveId) {
        BanWave wave = getWave(waveId);
        if (wave != null) {
            wave.setStopped(true);
            activeWaves.remove(wave);
        }
    }

    public List<BanWave> getActiveWaves() {
        return new ArrayList<>(activeWaves);
    }

    public BanWave getWave(UUID id) {
        for (BanWave wave : activeWaves) {
            if (wave.getId().equals(id)) {
                return wave;
            }
        }
        return null;
    }

    public boolean isRunning() {
        return isRunning;
    }

    private String getPlayerName(UUID uuid) {

        return plugin.getServer().getOfflinePlayer(uuid).getName();
    }

    public static class BanWave {
        private final UUID id;
        private final String name;
        private final String reason;
        private final UUID executorUUID;
        private final String executorName;
        private final List<UUID> targetUUIDs;
        private final List<UUID> affectedPlayers;
        private final long duration;
        private final long startedAt;
        private long completedAt;
        private boolean stopped;

        public BanWave(UUID id, String name, String reason, UUID executorUUID, String executorName,
                       List<UUID> targetUUIDs, long duration, long startedAt) {
            this.id = id;
            this.name = name;
            this.reason = reason;
            this.executorUUID = executorUUID;
            this.executorName = executorName;
            this.targetUUIDs = targetUUIDs;
            this.affectedPlayers = new ArrayList<>();
            this.duration = duration;
            this.startedAt = startedAt;
            this.completedAt = -1;
            this.stopped = false;
        }

        public void addAffectedPlayer(UUID uuid) {
            affectedPlayers.add(uuid);
        }

        public UUID getId() { return id; }
        public String getName() { return name; }
        public String getReason() { return reason; }
        public UUID getExecutorUUID() { return executorUUID; }
        public String getExecutorName() { return executorName; }
        public List<UUID> getTargetUUIDs() { return targetUUIDs; }
        public List<UUID> getAffectedPlayers() { return affectedPlayers; }
        public long getDuration() { return duration; }
        public long getStartedAt() { return startedAt; }
        public long getCompletedAt() { return completedAt; }
        public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
        public boolean isStopped() { return stopped; }
        public void setStopped(boolean stopped) { this.stopped = stopped; }
        public boolean isComplete() { return completedAt > 0; }
        public int getTotalTargets() { return targetUUIDs.size(); }
        public int getAffectedCount() { return affectedPlayers.size(); }
    }
}
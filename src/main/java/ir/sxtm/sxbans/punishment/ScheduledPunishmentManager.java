package ir.sxtm.sxbans.punishment;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ScheduledPunishmentManager {
    private final SXBans plugin;
    private final Map<UUID, ScheduledPunishment> scheduledPunishments;
    private final Map<UUID, BukkitTask> activeTasks;
    private boolean isShutdown;

    public ScheduledPunishmentManager(SXBans plugin) {
        this.plugin = plugin;
        this.scheduledPunishments = new ConcurrentHashMap<>();
        this.activeTasks = new ConcurrentHashMap<>();
        this.isShutdown = false;

        loadScheduledPunishments();
    }

    private void loadScheduledPunishments() {

        plugin.getSXBansLogger().info("Scheduled punishment manager initialized");
    }

    public void schedulePunishment(UUID targetUUID, String targetName, PunishmentType type,
                                   String reason, long duration, UUID executorUUID,
                                   String executorName, long scheduledTime) {
        if (isShutdown) return;

        ScheduledPunishment scheduled = new ScheduledPunishment(
                UUID.randomUUID(),
                targetUUID,
                targetName,
                type,
                reason,
                duration,
                executorUUID,
                executorName,
                scheduledTime
        );

        scheduledPunishments.put(scheduled.getId(), scheduled);

        long delay = scheduledTime - System.currentTimeMillis();
        if (delay <= 0) {

            executeScheduledPunishment(scheduled);
        } else {

            long ticks = Math.max(1, delay / 50);
            BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,
                    () -> executeScheduledPunishment(scheduled),
                    ticks
            );
            activeTasks.put(scheduled.getId(), task);
        }

        plugin.getSXBansLogger().info("Scheduled punishment for " + targetName +
                " at " + new Date(scheduledTime));
    }

    public void executeScheduledPunishment(ScheduledPunishment scheduled) {
        if (scheduled.isExecuted()) return;

        Punishment punishment = plugin.getPunishmentManager().applyPunishment(
                scheduled.getTargetUUID(),
                scheduled.getTargetName(),
                scheduled.getType(),
                scheduled.getReason(),
                scheduled.getDuration(),
                scheduled.getExecutorUUID(),
                scheduled.getExecutorName()
        );

        if (punishment != null) {
            scheduled.setExecuted(true);
            scheduled.setExecutionTime(System.currentTimeMillis());
            scheduled.setPunishmentId(punishment.getId());

            BukkitTask task = activeTasks.remove(scheduled.getId());
            if (task != null) {
                task.cancel();
            }

            plugin.getSXBansLogger().info("Executed scheduled punishment for " +
                    scheduled.getTargetName());
        }
    }

    public void cancelScheduledPunishment(UUID id) {
        ScheduledPunishment scheduled = scheduledPunishments.get(id);
        if (scheduled == null || scheduled.isExecuted()) return;

        BukkitTask task = activeTasks.remove(id);
        if (task != null) {
            task.cancel();
        }

        scheduled.setCancelled(true);
        plugin.getSXBansLogger().info("Cancelled scheduled punishment for " +
                scheduled.getTargetName());
    }

    public List<ScheduledPunishment> getPendingPunishments() {
        List<ScheduledPunishment> pending = new ArrayList<>();
        for (ScheduledPunishment sp : scheduledPunishments.values()) {
            if (!sp.isExecuted() && !sp.isCancelled()) {
                pending.add(sp);
            }
        }
        return pending;
    }

    public List<ScheduledPunishment> getScheduledPunishmentsForPlayer(UUID playerUUID) {
        List<ScheduledPunishment> result = new ArrayList<>();
        for (ScheduledPunishment sp : scheduledPunishments.values()) {
            if (sp.getTargetUUID().equals(playerUUID) && !sp.isExecuted()) {
                result.add(sp);
            }
        }
        return result;
    }

    public ScheduledPunishment getScheduledPunishment(UUID id) {
        return scheduledPunishments.get(id);
    }

    public void shutdown() {
        isShutdown = true;

        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();

        plugin.getSXBansLogger().info("Scheduled punishment manager shut down");
    }

    public static class ScheduledPunishment {
        private final UUID id;
        private final UUID targetUUID;
        private final String targetName;
        private final PunishmentType type;
        private final String reason;
        private final long duration;
        private final UUID executorUUID;
        private final String executorName;
        private final long scheduledTime;
        private boolean executed;
        private long executionTime;
        private UUID punishmentId;
        private boolean cancelled;

        public ScheduledPunishment(UUID id, UUID targetUUID, String targetName,
                                   PunishmentType type, String reason, long duration,
                                   UUID executorUUID, String executorName, long scheduledTime) {
            this.id = id;
            this.targetUUID = targetUUID;
            this.targetName = targetName;
            this.type = type;
            this.reason = reason;
            this.duration = duration;
            this.executorUUID = executorUUID;
            this.executorName = executorName;
            this.scheduledTime = scheduledTime;
            this.executed = false;
            this.cancelled = false;
        }

        public UUID getId() { return id; }
        public UUID getTargetUUID() { return targetUUID; }
        public String getTargetName() { return targetName; }
        public PunishmentType getType() { return type; }
        public String getReason() { return reason; }
        public long getDuration() { return duration; }
        public UUID getExecutorUUID() { return executorUUID; }
        public String getExecutorName() { return executorName; }
        public long getScheduledTime() { return scheduledTime; }
        public boolean isExecuted() { return executed; }
        public void setExecuted(boolean executed) { this.executed = executed; }
        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
        public UUID getPunishmentId() { return punishmentId; }
        public void setPunishmentId(UUID punishmentId) { this.punishmentId = punishmentId; }
        public boolean isCancelled() { return cancelled; }
        public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

        public long getTimeRemaining() {
            return Math.max(0, scheduledTime - System.currentTimeMillis());
        }

        public String getFormattedScheduledTime() {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date(scheduledTime));
        }
    }
}
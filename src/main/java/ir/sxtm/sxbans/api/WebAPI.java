package ir.sxtm.sxbans.api;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.WebUser;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.HistoryEntry;

import java.util.List;
import java.util.UUID;

public class WebAPI {
    private final SXBans plugin;

    public WebAPI(SXBans plugin) {
        this.plugin = plugin;
    }

    public boolean isWebServerRunning() {
        return plugin.getWebServer() != null && plugin.getWebServer().isRunning();
    }

    public int getWebPort() {
        return plugin.getConfigManager().getWebPort();
    }

    public String getWebHost() {
        return plugin.getConfigManager().getWebHost();
    }

    public WebUser authenticateWebUser(String username, String password) {
        if (plugin.getWebUsersManager().authenticate(username, password)) {
            return plugin.getWebUsersManager().getUser(username);
        }
        return null;
    }

    public String createWebSession(String username) {
        return plugin.getWebUsersManager().createSession(username);
    }

    public boolean validateWebSession(String token) {
        return plugin.getWebServer().isValidSession(token);
    }

    public WebUser getWebUserBySession(String token) {
        if (!validateWebSession(token)) return null;
        String username = plugin.getWebServer().getUsernameFromSession(token);
        if (username == null) return null;
        return plugin.getWebUsersManager().getUser(username);
    }

    public List<WebUser> getAllWebUsers() {
        return plugin.getWebUsersManager().getAllUsers();
    }

    public boolean createWebUser(String username, String password, int level) {
        return plugin.getWebUsersManager().createUser(username, password, level);
    }

    public boolean deleteWebUser(String username) {
        return plugin.getWebUsersManager().deleteUser(username);
    }

    public boolean updateWebUserLevel(String username, int level) {
        return plugin.getWebUsersManager().updateUserLevel(username, level);
    }

    public boolean updateWebUserPassword(String username, String newPassword) {
        return plugin.getWebUsersManager().updateUserPassword(username, newPassword);
    }

    public java.util.Map<String, Object> getDashboardStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();
        stats.put("totalPunishments", allPunishments.size());

        long activePunishments = allPunishments.stream()
                .filter(Punishment::isActive)
                .count();
        stats.put("activePunishments", activePunishments);

        long activeBans = allPunishments.stream()
                .filter(p -> p.isActive() && (p.getType() == Punishment.PunishmentType.BAN ||
                        p.getType() == Punishment.PunishmentType.TEMP_BAN))
                .count();
        stats.put("activeBans", activeBans);

        long activeMutes = allPunishments.stream()
                .filter(p -> p.isActive() && (p.getType() == Punishment.PunishmentType.MUTE ||
                        p.getType() == Punishment.PunishmentType.TEMP_MUTE))
                .count();
        stats.put("activeMutes", activeMutes);

        long totalWarnings = allPunishments.stream()
                .filter(p -> p.getType() == Punishment.PunishmentType.WARN)
                .count();
        stats.put("totalWarnings", totalWarnings);

        long today = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        long todayPunishments = allPunishments.stream()
                .filter(p -> p.getCreatedAt() > today)
                .count();
        stats.put("todayPunishments", todayPunishments);

        java.util.Set<UUID> uniquePlayers = allPunishments.stream()
                .map(Punishment::getPlayerUUID)
                .collect(java.util.stream.Collectors.toSet());
        stats.put("uniquePlayers", uniquePlayers.size());

        return stats;
    }

    public java.util.Map<String, Long> getPunishmentTypeStats() {
        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();
        return allPunishments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.getType().name(),
                        java.util.stream.Collectors.counting()
                ));
    }

    public java.util.Map<String, Long> getPunishmentDailyStats() {
        java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();

        long now = System.currentTimeMillis();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

        for (int i = 29; i >= 0; i--) {
            String day = sdf.format(new java.util.Date(now - (i * 24 * 60 * 60 * 1000L)));
            long count = allPunishments.stream()
                    .filter(p -> day.equals(sdf.format(new java.util.Date(p.getCreatedAt()))))
                    .count();
            stats.put(day, count);
        }

        return stats;
    }

    public java.util.Map<String, Long> getTopPunishedPlayers(int limit) {
        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();
        return allPunishments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Punishment::getPlayerName,
                        java.util.stream.Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        java.util.Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));
    }

    public void broadcastWebMessage(String message) {
    }

    public boolean reloadConfig() {
        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getMessagesManager().reloadMessages();
            plugin.getWebUsersManager().loadUsers();
            plugin.getTemplateManager().reloadTemplates();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
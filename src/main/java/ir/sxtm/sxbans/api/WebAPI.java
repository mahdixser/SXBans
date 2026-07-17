package ir.sxtm.sxbans.api;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.WebUser;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.HistoryEntry;

import java.util.List;
import java.util.UUID;

/**
 * API for web panel integration.
 * Provides methods for web panel to interact with the plugin.
 */
public class WebAPI {
    private final SXBans plugin;

    public WebAPI(SXBans plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if web server is running.
     *
     * @return true if running
     */
    public boolean isWebServerRunning() {
        return plugin.getWebServer() != null && plugin.getWebServer().isRunning();
    }

    /**
     * Get web server port.
     *
     * @return The port number
     */
    public int getWebPort() {
        return plugin.getConfigManager().getWebPort();
    }

    /**
     * Get web server host.
     *
     * @return The host address
     */
    public String getWebHost() {
        return plugin.getConfigManager().getWebHost();
    }

    /**
     * Authenticate a web user.
     *
     * @param username The username
     * @param password The password
     * @return The WebUser if authenticated, null otherwise
     */
    public WebUser authenticateWebUser(String username, String password) {
        if (plugin.getWebUsersManager().authenticate(username, password)) {
            return plugin.getWebUsersManager().getUser(username);
        }
        return null;
    }

    /**
     * Create a web session token.
     *
     * @param username The username
     * @return The session token, or null if failed
     */
    public String createWebSession(String username) {
        return plugin.getWebUsersManager().createSession(username);
    }

    /**
     * Validate a web session token.
     *
     * @param token The session token
     * @return true if valid
     */
    public boolean validateWebSession(String token) {
        return plugin.getWebServer().isValidSession(token);
    }

    /**
     * Get web user by session token.
     *
     * @param token The session token
     * @return The WebUser, or null if invalid
     */
    public WebUser getWebUserBySession(String token) {
        if (!validateWebSession(token)) return null;
        String username = plugin.getWebServer().getUsernameFromSession(token);
        if (username == null) return null;
        return plugin.getWebUsersManager().getUser(username);
    }

    /**
     * Get all web users.
     *
     * @return List of web users
     */
    public List<WebUser> getAllWebUsers() {
        return plugin.getWebUsersManager().getAllUsers();
    }

    /**
     * Create a new web user.
     *
     * @param username The username
     * @param password The password
     * @param level The permission level
     * @return true if created
     */
    public boolean createWebUser(String username, String password, int level) {
        return plugin.getWebUsersManager().createUser(username, password, level);
    }

    /**
     * Delete a web user.
     *
     * @param username The username
     * @return true if deleted
     */
    public boolean deleteWebUser(String username) {
        return plugin.getWebUsersManager().deleteUser(username);
    }

    /**
     * Update web user level.
     *
     * @param username The username
     * @param level The new level
     * @return true if updated
     */
    public boolean updateWebUserLevel(String username, int level) {
        return plugin.getWebUsersManager().updateUserLevel(username, level);
    }

    /**
     * Update web user password.
     *
     * @param username The username
     * @param newPassword The new password
     * @return true if updated
     */
    public boolean updateWebUserPassword(String username, String newPassword) {
        return plugin.getWebUsersManager().updateUserPassword(username, newPassword);
    }

    /**
     * Get dashboard statistics.
     *
     * @return Map of statistics
     */
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

    /**
     * Get punishment statistics by type.
     *
     * @return Map of type to count
     */
    public java.util.Map<String, Long> getPunishmentTypeStats() {
        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();
        return allPunishments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.getType().name(),
                        java.util.stream.Collectors.counting()
                ));
    }

    /**
     * Get punishment statistics by day (last 30 days).
     *
     * @return Map of day to count
     */
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

    /**
     * Get top punished players.
     *
     * @param limit Maximum number of players
     * @return Map of player name to punishment count
     */
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

    /**
     * Broadcast a message to the web panel.
     *
     * @param message The message to broadcast
     */
    public void broadcastWebMessage(String message) {
    }

    /**
     * Reload configuration.
     *
     * @return true if successful
     */
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
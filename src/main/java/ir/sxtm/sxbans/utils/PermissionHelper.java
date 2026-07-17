package ir.sxtm.sxbans.utils;

import ir.sxtm.sxbans.SXBans;
import org.bukkit.entity.Player;

/**
 * Helper class for permission management.
 */
public class PermissionHelper {
    private final SXBans plugin;

    public PermissionHelper(SXBans plugin) {
        this.plugin = plugin;
    }

    /**
     * Get a player's permission level.
     * Checks from highest (100) to lowest (1).
     *
     * @param player The player
     * @return The permission level (0-100)
     */
    public int getPermissionLevel(Player player) {
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("sxbans.adminlvl." + i)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Check if a player has a specific permission level.
     *
     * @param player The player
     * @param level The level to check
     * @return true if the player has at least the specified level
     */
    public boolean hasPermissionLevel(Player player, int level) {
        return getPermissionLevel(player) >= level;
    }

    /**
     * Check if a player can punish another player.
     * The executor must have a higher permission level than the target.
     *
     * @param executor The executor
     * @param target The target
     * @return true if the executor can punish the target
     */
    public boolean canPunish(Player executor, Player target) {
        int executorLevel = getPermissionLevel(executor);
        int targetLevel = getPermissionLevel(target);
        return executorLevel > targetLevel;
    }

    /**
     * Check if a player can punish another player by name.
     *
     * @param executor The executor
     * @param targetName The target name
     * @return true if the executor can punish the target
     */
    public boolean canPunish(Player executor, String targetName) {
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) return true; // Offline players can be punished
        return canPunish(executor, target);
    }

    /**
     * Check if a player has admin permission.
     *
     * @param player The player
     * @return true if admin
     */
    public boolean isAdmin(Player player) {
        return player.hasPermission("sxbans.admin") || player.hasPermission("sxbans.*");
    }

    /**
     * Check if a player has a specific punishment permission.
     *
     * @param player The player
     * @param permission The permission node
     * @return true if has permission
     */
    public boolean hasPunishmentPermission(Player player, String permission) {
        return player.hasPermission(permission) || isAdmin(player);
    }

    /**
     * Check if a player can bypass punishments.
     *
     * @param player The player
     * @param type The punishment type
     * @return true if can bypass
     */
    public boolean canBypass(Player player, String type) {
        return player.hasPermission("sxbans.bypass." + type.toLowerCase()) ||
                player.hasPermission("sxbans.bypass.*") ||
                isAdmin(player);
    }

    /**
     * Check if a player can bypass bans.
     *
     * @param player The player
     * @return true if can bypass bans
     */
    public boolean canBypassBan(Player player) {
        return canBypass(player, "ban");
    }

    /**
     * Check if a player can bypass mutes.
     *
     * @param player The player
     * @return true if can bypass mutes
     */
    public boolean canBypassMute(Player player) {
        return canBypass(player, "mute");
    }

    /**
     * Check if a player can bypass kicks.
     *
     * @param player The player
     * @return true if can bypass kicks
     */
    public boolean canBypassKick(Player player) {
        return canBypass(player, "kick");
    }

    /**
     * Check if a player can bypass IP bans.
     *
     * @param player The player
     * @return true if can bypass IP bans
     */
    public boolean canBypassIpBan(Player player) {
        return canBypass(player, "ipban");
    }

    /**
     * Check if a player can bypass IP mutes.
     *
     * @param player The player
     * @return true if can bypass IP mutes
     */
    public boolean canBypassIpMute(Player player) {
        return canBypass(player, "ipmute");
    }

    /**
     * Get the highest permission level from a list of levels.
     *
     * @param levels Array of levels to check
     * @return The highest level found
     */
    public int getHighestLevel(int... levels) {
        int highest = 0;
        for (int level : levels) {
            if (level > highest) {
                highest = level;
            }
        }
        return highest;
    }

    /**
     * Check if a player has web access.
     *
     * @param player The player
     * @return true if has web access
     */
    public boolean hasWebAccess(Player player) {
        return player.hasPermission("sxbans.web.access") || isAdmin(player);
    }

    /**
     * Check if a player has web admin access.
     *
     * @param player The player
     * @return true if has web admin access
     */
    public boolean hasWebAdmin(Player player) {
        return player.hasPermission("sxbans.web.admin") || isAdmin(player);
    }
}
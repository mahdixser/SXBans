package ir.sxtm.sxbans.utils;

import ir.sxtm.sxbans.SXBans;
import org.bukkit.entity.Player;

public class PermissionHelper {
    private final SXBans plugin;

    public PermissionHelper(SXBans plugin) {
        this.plugin = plugin;
    }

    public int getPermissionLevel(Player player) {
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("sxbans.adminlvl." + i)) {
                return i;
            }
        }
        return 0;
    }

    public boolean hasPermissionLevel(Player player, int level) {
        return getPermissionLevel(player) >= level;
    }

    public boolean canPunish(Player executor, Player target) {
        int executorLevel = getPermissionLevel(executor);
        int targetLevel = getPermissionLevel(target);
        return executorLevel > targetLevel;
    }

    public boolean canPunish(Player executor, String targetName) {
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) return true;
        return canPunish(executor, target);
    }

    public boolean isAdmin(Player player) {
        return player.hasPermission("sxbans.admin") || player.hasPermission("sxbans.*");
    }

    public boolean hasPunishmentPermission(Player player, String permission) {
        return player.hasPermission(permission) || isAdmin(player);
    }

    public boolean canBypass(Player player, String type) {
        return player.hasPermission("sxbans.bypass." + type.toLowerCase()) ||
                player.hasPermission("sxbans.bypass.*") ||
                isAdmin(player);
    }

    public boolean canBypassBan(Player player) {
        return canBypass(player, "ban");
    }

    public boolean canBypassMute(Player player) {
        return canBypass(player, "mute");
    }

    public boolean canBypassKick(Player player) {
        return canBypass(player, "kick");
    }

    public boolean canBypassIpBan(Player player) {
        return canBypass(player, "ipban");
    }

    public boolean canBypassIpMute(Player player) {
        return canBypass(player, "ipmute");
    }

    public int getHighestLevel(int... levels) {
        int highest = 0;
        for (int level : levels) {
            if (level > highest) {
                highest = level;
            }
        }
        return highest;
    }

    public boolean hasWebAccess(Player player) {
        return player.hasPermission("sxbans.web.access") || isAdmin(player);
    }

    public boolean hasWebAdmin(Player player) {
        return player.hasPermission("sxbans.web.admin") || isAdmin(player);
    }
}
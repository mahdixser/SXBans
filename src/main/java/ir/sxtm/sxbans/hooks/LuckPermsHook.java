package ir.sxtm.sxbans.hooks;

import ir.sxtm.sxbans.SXBans;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * LuckPerms integration for SX Bans.
 * Provides advanced permission management.
 */
public class LuckPermsHook {
    private final SXBans plugin;
    private LuckPerms luckPerms;
    private boolean setup;

    public LuckPermsHook(SXBans plugin) {
        this.plugin = plugin;
        this.setup = false;
    }

    /**
     * Setup LuckPerms hook.
     *
     * @return true if successful
     */
    public boolean setup() {
        try {
            luckPerms = LuckPermsProvider.get();
            setup = true;
            return true;
        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to hook into LuckPerms: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if LuckPerms is set up.
     *
     * @return true if set up
     */
    public boolean isSetup() {
        return setup;
    }

    /**
     * Get the LuckPerms instance.
     *
     * @return The LuckPerms instance
     */
    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    /**
     * Get a user by UUID.
     *
     * @param uuid The UUID
     * @return The User, or null if not found
     */
    public User getUser(UUID uuid) {
        if (!setup || luckPerms == null) return null;
        return luckPerms.getUserManager().getUser(uuid);
    }

    /**
     * Get a user by name.
     *
     * @param playerName The player name
     * @return The User, or null if not found
     */
    public User getUser(String playerName) {
        if (!setup || luckPerms == null) return null;
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) return null;
        return getUser(player.getUniqueId());
    }

    /**
     * Get a user by player.
     *
     * @param player The player
     * @return The User, or null if not found
     */
    public User getUser(Player player) {
        if (!setup || luckPerms == null) return null;
        return getUser(player.getUniqueId());
    }

    /**
     * Check if a player has a permission.
     *
     * @param player The player
     * @param permission The permission node
     * @return true if has permission
     */
    public boolean hasPermission(Player player, String permission) {
        if (!setup || luckPerms == null) {
            return player.hasPermission(permission);
        }

        User user = getUser(player);
        if (user == null) return player.hasPermission(permission);

        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }

    /**
     * Add a permission to a player.
     *
     * @param player The player
     * @param permission The permission node
     * @return true if successful
     */
    public boolean addPermission(Player player, String permission) {
        if (!setup || luckPerms == null) return false;

        User user = getUser(player);
        if (user == null) return false;

        Node node = PermissionNode.builder(permission).build();
        user.data().add(node);
        luckPerms.getUserManager().saveUser(user);
        return true;
    }

    /**
     * Remove a permission from a player.
     *
     * @param player The player
     * @param permission The permission node
     * @return true if successful
     */
    public boolean removePermission(Player player, String permission) {
        if (!setup || luckPerms == null) return false;

        User user = getUser(player);
        if (user == null) return false;

        user.data().remove(Node.builder(permission).build());
        luckPerms.getUserManager().saveUser(user);
        return true;
    }

    /**
     * Get a player's permission level.
     *
     * @param playerName The player name
     * @return The permission level (0-100)
     */
    public int getPermissionLevel(String playerName) {
        if (!setup || luckPerms == null) return 0;

        User user = getUser(playerName);
        if (user == null) return 0;

        for (int i = 100; i >= 1; i--) {
            if (user.getCachedData().getPermissionData()
                    .checkPermission("sxbans.adminlvl." + i).asBoolean()) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Get a player's permission level.
     *
     * @param player The player
     * @return The permission level (0-100)
     */
    public int getPermissionLevel(Player player) {
        return getPermissionLevel(player.getName());
    }

    /**
     * Get a player's primary group.
     *
     * @param player The player
     * @return The primary group name
     */
    public String getPrimaryGroup(Player player) {
        if (!setup || luckPerms == null) return "";

        User user = getUser(player);
        if (user == null) return "";

        return user.getPrimaryGroup();
    }

    /**
     * Get all groups for a player.
     *
     * @param player The player
     * @return Array of group names
     */
    public String[] getGroups(Player player) {
        if (!setup || luckPerms == null) return new String[0];

        User user = getUser(player);
        if (user == null) return new String[0];

        return user.getInheritedGroups(user.getQueryOptions()).stream()
                .map(group -> group.getName())
                .toArray(String[]::new);
    }

    /**
     * Add a player to a group.
     *
     * @param player The player
     * @param group The group name
     * @return true if successful
     */
    public boolean addToGroup(Player player, String group) {
        if (!setup || luckPerms == null) return false;

        User user = getUser(player);
        if (user == null) return false;

        user.data().add(Node.builder("group." + group).build());
        luckPerms.getUserManager().saveUser(user);
        return true;
    }

    /**
     * Remove a player from a group.
     *
     * @param player The player
     * @param group The group name
     * @return true if successful
     */
    public boolean removeFromGroup(Player player, String group) {
        if (!setup || luckPerms == null) return false;

        User user = getUser(player);
        if (user == null) return false;

        user.data().remove(Node.builder("group." + group).build());
        luckPerms.getUserManager().saveUser(user);
        return true;
    }

    /**
     * Get a player's permission level asynchronously.
     *
     * @param playerName The player name
     * @return CompletableFuture with the permission level
     */
    public CompletableFuture<Integer> getPermissionLevelAsync(String playerName) {
        if (!setup || luckPerms == null) {
            return CompletableFuture.completedFuture(0);
        }

        // Fix: loadUser accepts UUID, not String
        // First get the UUID from player name
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return CompletableFuture.completedFuture(0);
        }

        return luckPerms.getUserManager().loadUser(player.getUniqueId())
                .thenApply(user -> {
                    if (user == null) return 0;
                    for (int i = 100; i >= 1; i--) {
                        if (user.getCachedData().getPermissionData()
                                .checkPermission("sxbans.adminlvl." + i).asBoolean()) {
                            return i;
                        }
                    }
                    return 0;
                });
    }
}
package ir.sxtm.sxbans.hooks;

import ir.sxtm.sxbans.SXBans;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault integration for SX Bans.
 * Provides permission and chat management.
 */
public class VaultHook {
    private final SXBans plugin;
    private Permission permission;
    private Chat chat;
    private boolean setup;

    public VaultHook(SXBans plugin) {
        this.plugin = plugin;
        this.setup = false;
    }

    /**
     * Setup Vault hooks.
     *
     * @return true if successful
     */
    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Permission> permissionProvider =
                Bukkit.getServicesManager().getRegistration(Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }

        RegisteredServiceProvider<Chat> chatProvider =
                Bukkit.getServicesManager().getRegistration(Chat.class);
        if (chatProvider != null) {
            chat = chatProvider.getProvider();
        }

        setup = permission != null;
        return setup;
    }

    /**
     * Check if Vault is set up.
     *
     * @return true if set up
     */
    public boolean isSetup() {
        return setup;
    }

    /**
     * Get the permission provider.
     *
     * @return The permission provider, or null if not available
     */
    public Permission getPermission() {
        return permission;
    }

    /**
     * Get the chat provider.
     *
     * @return The chat provider, or null if not available
     */
    public Chat getChat() {
        return chat;
    }

    /**
     * Check if a player has a permission.
     *
     * @param player The player
     * @param permissionNode The permission node
     * @return true if has permission
     */
    public boolean hasPermission(Player player, String permissionNode) {
        if (this.permission != null) {
            return this.permission.has(player, permissionNode);
        }
        return player.hasPermission(permissionNode);
    }

    /**
     * Add a permission to a player.
     *
     * @param player The player
     * @param permissionNode The permission node
     * @return true if successful
     */
    public boolean addPermission(Player player, String permissionNode) {
        if (this.permission != null) {
            return this.permission.playerAdd(player, permissionNode);
        }
        return false;
    }

    /**
     * Remove a permission from a player.
     *
     * @param player The player
     * @param permissionNode The permission node
     * @return true if successful
     */
    public boolean removePermission(Player player, String permissionNode) {
        if (this.permission != null) {
            return this.permission.playerRemove(player, permissionNode);
        }
        return false;
    }

    /**
     * Get a player's permission level.
     *
     * @param playerName The player name
     * @return The permission level (0-100)
     */
    public int getPermissionLevel(String playerName) {
        if (permission == null) return 0;

        // Fix: استفاده از World برای جلوگیری از ambiguity
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        for (int i = 100; i >= 1; i--) {
            if (permission.playerHas(world, playerName, "sxbans.adminlvl." + i)) {
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
     * Get a player's prefix.
     *
     * @param player The player
     * @return The prefix
     */
    public String getPrefix(Player player) {
        if (chat != null) {
            return chat.getPlayerPrefix(player);
        }
        return "";
    }

    /**
     * Get a player's suffix.
     *
     * @param player The player
     * @return The suffix
     */
    public String getSuffix(Player player) {
        if (chat != null) {
            return chat.getPlayerSuffix(player);
        }
        return "";
    }

    /**
     * Set a player's prefix.
     *
     * @param player The player
     * @param prefix The prefix
     * @return true if successful
     */
    public boolean setPrefix(Player player, String prefix) {
        if (chat != null) {
            chat.setPlayerPrefix(player, prefix);
            return true;
        }
        return false;
    }

    /**
     * Set a player's suffix.
     *
     * @param player The player
     * @param suffix The suffix
     * @return true if successful
     */
    public boolean setSuffix(Player player, String suffix) {
        if (chat != null) {
            chat.setPlayerSuffix(player, suffix);
            return true;
        }
        return false;
    }

    /**
     * Get a player's group.
     *
     * @param player The player
     * @return The group name
     */
    public String getGroup(Player player) {
        if (permission != null) {
            return permission.getPrimaryGroup(player);
        }
        return "";
    }

    /**
     * Get a player's groups.
     *
     * @param player The player
     * @return Array of group names
     */
    public String[] getGroups(Player player) {
        if (permission != null) {
            return permission.getPlayerGroups(player);
        }
        return new String[0];
    }
}
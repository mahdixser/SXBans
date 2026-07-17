package ir.sxtm.sxbans.hooks;

import ir.sxtm.sxbans.SXBans;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager for handling external plugin hooks.
 * Supports PlaceholderAPI, Vault, and LuckPerms.
 */
public class HookManager {
    private final SXBans plugin;
    private final Map<HookType, Boolean> hookStatus;
    private PlaceholderAPIHook placeholderAPIHook;
    private VaultHook vaultHook;
    private LuckPermsHook luckPermsHook;

    public enum HookType {
        PLACEHOLDER_API,
        VAULT,
        LUCKPERMS
    }

    public HookManager(SXBans plugin) {
        this.plugin = plugin;
        this.hookStatus = new HashMap<>();
    }

    /**
     * Initialize all hooks.
     */
    public void initialize() {
        // PlaceholderAPI
        Plugin placeholderPlugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderPlugin != null && placeholderPlugin.isEnabled()) {
            placeholderAPIHook = new PlaceholderAPIHook(plugin);
            placeholderAPIHook.registerExpansion();
            hookStatus.put(HookType.PLACEHOLDER_API, true);
            plugin.getSXBansLogger().info("PlaceholderAPI hook enabled");
        } else {
            hookStatus.put(HookType.PLACEHOLDER_API, false);
            plugin.getSXBansLogger().info("PlaceholderAPI not found, hook disabled");
        }

        // Vault
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultHook = new VaultHook(plugin);
            vaultHook.setup();
            hookStatus.put(HookType.VAULT, true);
            plugin.getSXBansLogger().info("Vault hook enabled");
        } else {
            hookStatus.put(HookType.VAULT, false);
        }

        // LuckPerms
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsHook = new LuckPermsHook(plugin);
            luckPermsHook.setup();
            hookStatus.put(HookType.LUCKPERMS, true);
            plugin.getSXBansLogger().info("LuckPerms hook enabled");
        } else {
            hookStatus.put(HookType.LUCKPERMS, false);
        }
    }

    /**
     * Check if a hook is enabled.
     *
     * @param type The hook type
     * @return true if enabled
     */
    public boolean isHookEnabled(HookType type) {
        return hookStatus.getOrDefault(type, false);
    }

    /**
     * Get PlaceholderAPI hook.
     *
     * @return The PlaceholderAPI hook, or null if not available
     */
    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }

    /**
     * Get Vault hook.
     *
     * @return The Vault hook, or null if not available
     */
    public VaultHook getVaultHook() {
        return vaultHook;
    }

    /**
     * Get LuckPerms hook.
     *
     * @return The LuckPerms hook, or null if not available
     */
    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }

    /**
     * Get a player's permission level using available hooks.
     *
     * @param playerName The player name
     * @return The permission level (0-100)
     */
    public int getPlayerPermissionLevel(String playerName) {
        // Try LuckPerms first
        if (isHookEnabled(HookType.LUCKPERMS) && luckPermsHook != null) {
            int level = luckPermsHook.getPermissionLevel(playerName);
            if (level > 0) return level;
        }

        // Try Vault
        if (isHookEnabled(HookType.VAULT) && vaultHook != null) {
            int level = vaultHook.getPermissionLevel(playerName);
            if (level > 0) return level;
        }

        // Default: check permissions directly
        org.bukkit.entity.Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            for (int i = 100; i >= 1; i--) {
                if (player.hasPermission("sxbans.adminlvl." + i)) {
                    return i;
                }
            }
        }

        return 0;
    }

    /**
     * Get a player's permission level using available hooks.
     *
     * @param player The player
     * @return The permission level (0-100)
     */
    public int getPlayerPermissionLevel(org.bukkit.entity.Player player) {
        return getPlayerPermissionLevel(player.getName());
    }

    /**
     * Format a message with PlaceholderAPI if available.
     *
     * @param player The player
     * @param message The message to format
     * @return The formatted message
     */
    public String formatPlaceholders(org.bukkit.entity.Player player, String message) {
        if (isHookEnabled(HookType.PLACEHOLDER_API) && placeholderAPIHook != null) {
            return placeholderAPIHook.formatPlaceholders(player, message);
        }
        return message;
    }
}
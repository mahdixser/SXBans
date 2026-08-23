package ir.sxtm.sxbans.hooks;

import ir.sxtm.sxbans.SXBans;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

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

    public void initialize() {

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

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultHook = new VaultHook(plugin);
            vaultHook.setup();
            hookStatus.put(HookType.VAULT, true);
            plugin.getSXBansLogger().info("Vault hook enabled");
        } else {
            hookStatus.put(HookType.VAULT, false);
        }

        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsHook = new LuckPermsHook(plugin);
            luckPermsHook.setup();
            hookStatus.put(HookType.LUCKPERMS, true);
            plugin.getSXBansLogger().info("LuckPerms hook enabled");
        } else {
            hookStatus.put(HookType.LUCKPERMS, false);
        }
    }

    public boolean isHookEnabled(HookType type) {
        return hookStatus.getOrDefault(type, false);
    }

    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }

    public int getPlayerPermissionLevel(String playerName) {

        if (isHookEnabled(HookType.LUCKPERMS) && luckPermsHook != null) {
            int level = luckPermsHook.getPermissionLevel(playerName);
            if (level > 0) return level;
        }

        if (isHookEnabled(HookType.VAULT) && vaultHook != null) {
            int level = vaultHook.getPermissionLevel(playerName);
            if (level > 0) return level;
        }

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

    public int getPlayerPermissionLevel(org.bukkit.entity.Player player) {
        return getPlayerPermissionLevel(player.getName());
    }

    public String formatPlaceholders(org.bukkit.entity.Player player, String message) {
        if (isHookEnabled(HookType.PLACEHOLDER_API) && placeholderAPIHook != null) {
            return placeholderAPIHook.formatPlaceholders(player, message);
        }
        return message;
    }
}
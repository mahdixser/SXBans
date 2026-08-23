package ir.sxtm.sxbans.hooks;

import ir.sxtm.sxbans.SXBans;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {
    private final SXBans plugin;
    private Permission permission;
    private Chat chat;
    private boolean setup;

    public VaultHook(SXBans plugin) {
        this.plugin = plugin;
        this.setup = false;
    }

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

    public boolean isSetup() {
        return setup;
    }

    public Permission getPermission() {
        return permission;
    }

    public Chat getChat() {
        return chat;
    }

    public boolean hasPermission(Player player, String permissionNode) {
        if (this.permission != null) {
            return this.permission.has(player, permissionNode);
        }
        return player.hasPermission(permissionNode);
    }

    public boolean addPermission(Player player, String permissionNode) {
        if (this.permission != null) {
            return this.permission.playerAdd(player, permissionNode);
        }
        return false;
    }

    public boolean removePermission(Player player, String permissionNode) {
        if (this.permission != null) {
            return this.permission.playerRemove(player, permissionNode);
        }
        return false;
    }

    public int getPermissionLevel(String playerName) {
        if (permission == null) return 0;

        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        for (int i = 100; i >= 1; i--) {
            if (permission.playerHas(world, playerName, "sxbans.adminlvl." + i)) {
                return i;
            }
        }
        return 0;
    }

    public int getPermissionLevel(Player player) {
        return getPermissionLevel(player.getName());
    }

    public String getPrefix(Player player) {
        if (chat != null) {
            return chat.getPlayerPrefix(player);
        }
        return "";
    }

    public String getSuffix(Player player) {
        if (chat != null) {
            return chat.getPlayerSuffix(player);
        }
        return "";
    }

    public boolean setPrefix(Player player, String prefix) {
        if (chat != null) {
            chat.setPlayerPrefix(player, prefix);
            return true;
        }
        return false;
    }

    public boolean setSuffix(Player player, String suffix) {
        if (chat != null) {
            chat.setPlayerSuffix(player, suffix);
            return true;
        }
        return false;
    }

    public String getGroup(Player player) {
        if (permission != null) {
            return permission.getPrimaryGroup(player);
        }
        return "";
    }

    public String[] getGroups(Player player) {
        if (permission != null) {
            return permission.getPlayerGroups(player);
        }
        return new String[0];
    }
}
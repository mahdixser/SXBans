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

public class LuckPermsHook {
    private final SXBans plugin;
    private LuckPerms luckPerms;
    private boolean setup;

    public LuckPermsHook(SXBans plugin) {
        this.plugin = plugin;
        this.setup = false;
    }

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

    public boolean isSetup() {
        return setup;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public User getUser(UUID uuid) {
        if (!setup || luckPerms == null) return null;
        return luckPerms.getUserManager().getUser(uuid);
    }

    public User getUser(String playerName) {
        if (!setup || luckPerms == null) return null;
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) return null;
        return getUser(player.getUniqueId());
    }

    public User getUser(Player player) {
        if (!setup || luckPerms == null) return null;
        return getUser(player.getUniqueId());
    }

    public boolean hasPermission(Player player, String permission) {
        if (!setup || luckPerms == null) {
            return player.hasPermission(permission);
        }

        User user = getUser(player);
        if (user == null) return player.hasPermission(permission);

        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }

    public boolean addPermission(Player player, String permission) {
        if (!setup || luckPerms == null) return false;

        User user = getUser(player);
        if (user == null) return false;

        Node node = PermissionNode.builder(permission).build();
        user.data().add(node);
        luckPerms.getUserManager().saveUser(user);
        return true;
    }

    public boolean removePermission(Player player, String permission) {
        if (!setup || luckPerms == null) return false;

        User user = getUser(player);
        if (user == null) return false;

        user.data().remove(Node.builder(permission).build());
        luckPerms.getUserManager().saveUser(user);
        return true;
    }

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

    public int getPermissionLevel(Player player) {
        return getPermissionLevel(player.getName());
    }

    public String getPrimaryGroup(Player player) {
        if (!setup || luckPerms == null) return "";

        User user = getUser(player);
        if (user == null) return "";

        return user.getPrimaryGroup();
    }

    public String[] getGroups(Player player) {
        if (!setup || luckPerms == null) return new String[0];

        User user = getUser(player);
        if (user == null) return new String[0];

        return user.getInheritedGroups(user.getQueryOptions()).stream()
                .map(group -> group.getName())
                .toArray(String[]::new);
    }

    public boolean addToGroup(Player player, String group) {
        if (!setup || luckPerms == null) return false;

        User user = getUser(player);
        if (user == null) return false;

        user.data().add(Node.builder("group." + group).build());
        luckPerms.getUserManager().saveUser(user);
        return true;
    }

    public boolean removeFromGroup(Player player, String group) {
        if (!setup || luckPerms == null) return false;

        User user = getUser(player);
        if (user == null) return false;

        user.data().remove(Node.builder("group." + group).build());
        luckPerms.getUserManager().saveUser(user);
        return true;
    }

    public CompletableFuture<Integer> getPermissionLevelAsync(String playerName) {
        if (!setup || luckPerms == null) {
            return CompletableFuture.completedFuture(0);
        }

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
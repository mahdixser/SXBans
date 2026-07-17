package ir.sxtm.sxbans.commands;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import ir.sxtm.sxbans.utils.PermissionHelper;
import ir.sxtm.sxbans.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {
    protected final SXBans plugin;
    protected final PermissionHelper permissionHelper;

    public BaseCommand(SXBans plugin) {
        this.plugin = plugin;
        this.permissionHelper = new PermissionHelper(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender)) {
            sendMessage(sender, "error.no-permission");
            return true;
        }

        if (!validateArgs(sender, args)) {
            sendUsage(sender);
            return true;
        }

        return execute(sender, args);
    }

    protected abstract boolean hasPermission(CommandSender sender);
    protected abstract boolean validateArgs(CommandSender sender, String[] args);
    protected abstract boolean execute(CommandSender sender, String[] args);
    protected abstract void sendUsage(CommandSender sender);

    protected Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }

    protected UUID getPlayerUUID(String name) {
        Player player = getPlayer(name);
        if (player != null) {
            return player.getUniqueId();
        }
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    protected String getPlayerName(String name) {
        Player player = getPlayer(name);
        if (player != null) {
            return player.getName();
        }
        return name;
    }

    protected void sendMessage(CommandSender sender, String key) {
        String message = plugin.getMessagesManager().getMessage(key);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(plugin.getMessagesManager().colorize(message));
        }
    }

    protected void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = plugin.getMessagesManager().getMessage(key, placeholders);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(plugin.getMessagesManager().colorize(message));
        }
    }

    protected void sendRawMessage(CommandSender sender, String message) {
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(plugin.getMessagesManager().colorize(message));
        }
    }

    protected void broadcastPunishment(Punishment punishment) {
        String message = plugin.getMessagesManager().getPunishmentBroadcast(punishment);
        if (message != null && !message.isEmpty()) {
            Bukkit.broadcastMessage(message);
        }
    }

    protected boolean checkPermissionLevel(CommandSender sender, Player target) {
        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;
        int senderLevel = permissionHelper.getPermissionLevel(player);
        int targetLevel = permissionHelper.getPermissionLevel(target);

        if (senderLevel <= targetLevel) {
            sendMessage(sender, "error.higher-level");
            return false;
        }
        return true;
    }

    protected boolean checkPermissionLevel(CommandSender sender, UUID targetUUID) {
        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null) return true;

        return checkPermissionLevel(sender, target);
    }

    protected long parseTime(String timeStr) {
        return TimeUtils.parseTime(timeStr);
    }

    protected String formatTime(long millis) {
        return TimeUtils.formatTime(millis);
    }

    protected boolean isPlayerOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    protected List<String> getOnlinePlayers() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    protected List<String> getPunishmentTypes() {
        List<String> types = new ArrayList<>();
        for (PunishmentType type : PunishmentType.values()) {
            types.add(type.name().toLowerCase());
        }
        return types;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasPermission(sender)) {
            return Collections.emptyList();
        }
        return tabComplete(sender, args);
    }

    protected abstract List<String> tabComplete(CommandSender sender, String[] args);
}
package ir.sxtm.sxbans.commands;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class IpBanCommand extends BaseCommand {

    public IpBanCommand(SXBans plugin) {
        super(plugin);
    }

    @Override
    protected boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("sxbans.ipban") || sender.hasPermission("sxbans.admin");
    }

    @Override
    protected boolean validateArgs(CommandSender sender, String[] args) {
        return args.length >= 2;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        String target = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        String ip;
        String playerName = null;

        // Check if target is a player or IP
        Player player = Bukkit.getPlayer(target);
        if (player != null) {
            ip = player.getAddress().getAddress().getHostAddress();
            playerName = player.getName();
        } else if (target.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
            ip = target;
        } else {
            sendMessage(sender, "error.invalid-ip");
            return true;
        }

        // Check if IP is already banned
        if (plugin.getPunishmentManager().isIpBanned(ip)) {
            sendMessage(sender, "error.already-banned", Map.of("player", ip));
            return true;
        }

        // Apply IP ban
        UUID executorUUID = sender instanceof Player ? ((Player) sender).getUniqueId() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");
        String executorName = sender.getName();

        Punishment punishment = plugin.getPunishmentManager().applyPunishment(
                UUID.randomUUID(),
                playerName != null ? playerName : ip,
                PunishmentType.IP_BAN,
                reason,
                -1,
                executorUUID,
                executorName
        );

        if (punishment != null) {
            punishment.setIpAddress(ip);
            plugin.getPunishmentStorage().savePunishment(punishment);

            // Kick all players with this IP
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getAddress().getAddress().getHostAddress().equals(ip)) {
                    onlinePlayer.kickPlayer(plugin.getMessagesManager().getBanMessage(punishment));
                }
            }

            sendMessage(sender, "success.ipban", Map.of("ip", ip));
            broadcastPunishment(punishment);
        }

        return true;
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "command.ipban.usage");
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getOnlinePlayers();
        }
        return Collections.emptyList();
    }
}
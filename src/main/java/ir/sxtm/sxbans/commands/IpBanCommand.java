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

        if (plugin.getPunishmentManager().isIpBanned(ip)) {
            sendMessage(sender, "error.already-banned", Map.of("player", ip));
            return true;
        }

        UUID executorUUID = sender instanceof Player ? ((Player) sender).getUniqueId() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");
        String executorName = sender.getName();

        UUID targetUUID = player != null ? player.getUniqueId() : UUID.randomUUID();

        Punishment punishment = plugin.getPunishmentManager().applyPunishment(
                targetUUID,
                playerName != null ? playerName : ip,
                PunishmentType.IP_BAN,
                reason,
                -1,
                executorUUID,
                executorName,
                ip
        );

        if (punishment != null) {

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getAddress() != null && onlinePlayer.getAddress().getAddress() != null &&
                        onlinePlayer.getAddress().getAddress().getHostAddress().equals(ip)) {
                    onlinePlayer.kickPlayer(plugin.getMessagesManager().getBanMessage(punishment));
                }
            }

            sendMessage(sender, "success.ipban", Map.of("ip", ip));

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
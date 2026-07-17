package ir.sxtm.sxbans.commands;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class BanCommand extends BaseCommand {

    public BanCommand(SXBans plugin) {
        super(plugin);
    }

    @Override
    protected boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("sxbans.ban") || sender.hasPermission("sxbans.admin");
    }

    @Override
    protected boolean validateArgs(CommandSender sender, String[] args) {
        return args.length >= 2;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        String playerName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sendMessage(sender, "error.player-not-found");
            return true;
        }

        // Check permission level
        if (!checkPermissionLevel(sender, target)) {
            return true;
        }

        // Check if already banned
        if (plugin.getPunishmentManager().isPlayerBanned(target.getUniqueId())) {
            sendMessage(sender, "error.already-banned", Map.of("player", target.getName()));
            return true;
        }

        // Apply ban
        UUID executorUUID = sender instanceof Player ? ((Player) sender).getUniqueId() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");
        String executorName = sender.getName();

        Punishment punishment = plugin.getPunishmentManager().applyPunishment(
                target.getUniqueId(),
                target.getName(),
                PunishmentType.BAN,
                reason,
                -1,
                executorUUID,
                executorName
        );

        if (punishment != null) {
            sendMessage(sender, "success.ban", Map.of("player", target.getName()));
            broadcastPunishment(punishment);
        }

        return true;
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "command.ban.usage");
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getOnlinePlayers();
        }
        return Collections.emptyList();
    }
}
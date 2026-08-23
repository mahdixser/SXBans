package ir.sxtm.sxbans.commands;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class MuteCommand extends BaseCommand {

    public MuteCommand(SXBans plugin) {
        super(plugin);
    }

    @Override
    protected boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("sxbans.mute") || sender.hasPermission("sxbans.admin");
    }

    @Override
    protected boolean validateArgs(CommandSender sender, String[] args) {
        return args.length >= 2;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        String playerName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        UUID targetUUID = getPlayerUUID(playerName);
        if (targetUUID == null) {
            sendMessage(sender, "error.player-not-found");
            return true;
        }
        String targetName = getPlayerName(playerName);

        if (!checkPermissionLevel(sender, targetUUID)) {
            return true;
        }

        if (plugin.getPunishmentManager().isPlayerMuted(targetUUID)) {
            sendMessage(sender, "error.already-muted", Map.of("player", targetName));
            return true;
        }

        UUID executorUUID = sender instanceof Player ? ((Player) sender).getUniqueId() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");
        String executorName = sender.getName();

        Punishment punishment = plugin.getPunishmentManager().applyPunishment(
                targetUUID,
                targetName,
                PunishmentType.MUTE,
                reason,
                -1,
                executorUUID,
                executorName
        );

        if (punishment != null) {
            sendMessage(sender, "success.mute", Map.of("player", targetName));

        }

        return true;
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "command.mute.usage");
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getOnlinePlayers();
        }
        return Collections.emptyList();
    }
}
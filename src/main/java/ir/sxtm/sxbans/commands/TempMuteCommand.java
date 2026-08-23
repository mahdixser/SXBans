package ir.sxtm.sxbans.commands;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class TempMuteCommand extends BaseCommand {

    public TempMuteCommand(SXBans plugin) {
        super(plugin);
    }

    @Override
    protected boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("sxbans.mute") || sender.hasPermission("sxbans.admin");
    }

    @Override
    protected boolean validateArgs(CommandSender sender, String[] args) {
        return args.length >= 3;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        String playerName = args[0];
        String timeStr = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

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

        long duration = parseTime(timeStr);
        if (duration <= 0) {
            sendMessage(sender, "error.invalid-time");
            return true;
        }

        UUID executorUUID = sender instanceof Player ? ((Player) sender).getUniqueId() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");
        String executorName = sender.getName();

        Punishment punishment = plugin.getPunishmentManager().applyPunishment(
                targetUUID,
                targetName,
                PunishmentType.TEMP_MUTE,
                reason,
                duration,
                executorUUID,
                executorName
        );

        if (punishment != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            placeholders.put("duration", formatTime(duration));
            sendMessage(sender, "success.mute", placeholders);

        }

        return true;
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "command.tempmute.usage");
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getOnlinePlayers();
        }
        if (args.length == 2) {
            return Arrays.asList("1s", "5s", "10s", "30s", "1m", "5m", "10m", "30m", "1h", "2h", "6h", "12h", "1d", "2d", "7d", "30d");
        }
        return Collections.emptyList();
    }
}
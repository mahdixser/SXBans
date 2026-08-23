package ir.sxtm.sxbans.commands;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class WarnCommand extends BaseCommand {

    public WarnCommand(SXBans plugin) {
        super(plugin);
    }

    @Override
    protected boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("sxbans.warn") || sender.hasPermission("sxbans.admin");
    }

    @Override
    protected boolean validateArgs(CommandSender sender, String[] args) {
        return args.length >= 2;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        String playerName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Player onlineTarget = Bukkit.getPlayer(playerName);
        UUID targetUUID = getPlayerUUID(playerName);
        if (targetUUID == null) {
            sendMessage(sender, "error.player-not-found");
            return true;
        }
        String targetName = getPlayerName(playerName);

        if (!checkPermissionLevel(sender, targetUUID)) {
            return true;
        }

        UUID executorUUID = sender instanceof Player ? ((Player) sender).getUniqueId() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");
        String executorName = sender.getName();

        Punishment punishment = plugin.getPunishmentManager().applyPunishment(
                targetUUID,
                targetName,
                PunishmentType.WARN,
                reason,
                -1,
                executorUUID,
                executorName
        );

        if (punishment != null) {
            int warnings = plugin.getPunishmentManager().getWarningCount(targetUUID);
            int maxWarnings = plugin.getConfigManager().getMaxWarnings();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            placeholders.put("count", String.valueOf(warnings));
            placeholders.put("max", String.valueOf(maxWarnings));

            sendMessage(sender, "success.warn", placeholders);

            if (onlineTarget != null) {
                onlineTarget.sendMessage(plugin.getMessagesManager().getColoredMessage("punishment.warning-count", placeholders));
            }

            if (warnings >= maxWarnings && plugin.getConfigManager().isAutoBanEnabled()) {

            }
        }

        return true;
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "command.warn.usage");
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getOnlinePlayers();
        }
        return Collections.emptyList();
    }
}
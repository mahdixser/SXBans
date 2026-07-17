package ir.sxtm.sxbans.commands;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.IPData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class CheckCommand extends BaseCommand {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public CheckCommand(SXBans plugin) {
        super(plugin);
    }

    @Override
    protected boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("sxbans.check") || sender.hasPermission("sxbans.admin");
    }

    @Override
    protected boolean validateArgs(CommandSender sender, String[] args) {
        return args.length >= 1;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        String playerName = args[0];

        Player target = Bukkit.getPlayer(playerName);
        UUID targetUUID;
        String name;
        String ip;

        if (target != null) {
            targetUUID = target.getUniqueId();
            name = target.getName();
            ip = target.getAddress().getAddress().getHostAddress();
        } else {
            targetUUID = getPlayerUUID(playerName);
            name = playerName;
            ip = "Unknown";
        }

        if (targetUUID == null) {
            sendMessage(sender, "error.player-not-found");
            return true;
        }

        // Send player info header
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("check.title",
                Map.of("player", name)));
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("history.header"));

        // Check ban status
        if (plugin.getPunishmentManager().isPlayerBanned(targetUUID)) {
            Punishment ban = plugin.getPunishmentManager().getActiveBan(targetUUID);
            if (ban != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("reason", ban.getReason());
                placeholders.put("executor", ban.getExecutorName());
                placeholders.put("duration", ban.getFormattedTimeRemaining());
                sender.sendMessage(plugin.getMessagesManager().getColoredMessage("check.status.banned", placeholders));
            }
        } else {
            sender.sendMessage(plugin.getMessagesManager().getColoredMessage("check.status.clean"));
        }

        // Check mute status
        if (plugin.getPunishmentManager().isPlayerMuted(targetUUID)) {
            Punishment mute = plugin.getPunishmentManager().getActiveMute(targetUUID);
            if (mute != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("reason", mute.getReason());
                placeholders.put("executor", mute.getExecutorName());
                placeholders.put("duration", mute.getFormattedTimeRemaining());
                sender.sendMessage(plugin.getMessagesManager().getColoredMessage("check.status.muted", placeholders));
            }
        }

        // Show warnings
        int warnings = plugin.getPunishmentManager().getWarningCount(targetUUID);
        int maxWarnings = plugin.getConfigManager().getMaxWarnings();
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("check.status.warnings",
                Map.of("count", String.valueOf(warnings), "max", String.valueOf(maxWarnings))));

        // Show IP info
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("check.ip",
                Map.of("ip", ip)));

        // Show IP data if available
        if (!"Unknown".equals(ip)) {
            IPData ipData = plugin.getIPTracker().getIPData(ip);
            if (ipData != null) {
                sender.sendMessage(plugin.getMessagesManager().getColoredMessage("check.last-login",
                        Map.of("time", dateFormat.format(new Date(ipData.getLastSeen())))));

                int playerCount = ipData.getPlayerCount();
                if (playerCount > 1) {
                    sender.sendMessage(plugin.getMessagesManager().getColoredMessage("check.alt-accounts",
                            Map.of("count", String.valueOf(playerCount - 1))));
                }
            }
        }

        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("history.footer"));

        return true;
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "command.check.usage");
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getOnlinePlayers();
        }
        return Collections.emptyList();
    }
}
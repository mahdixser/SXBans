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

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sendMessage(sender, "error.player-not-found");
            return true;
        }

        // Check permission level
        if (!checkPermissionLevel(sender, target)) {
            return true;
        }

        // Apply warn
        UUID executorUUID = sender instanceof Player ? ((Player) sender).getUniqueId() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");
        String executorName = sender.getName();

        Punishment punishment = plugin.getPunishmentManager().applyPunishment(
                target.getUniqueId(),
                target.getName(),
                PunishmentType.WARN,
                reason,
                -1,
                executorUUID,
                executorName
        );

        if (punishment != null) {
            int warnings = plugin.getPunishmentManager().getWarningCount(target.getUniqueId());
            int maxWarnings = plugin.getConfigManager().getMaxWarnings();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            placeholders.put("count", String.valueOf(warnings));
            placeholders.put("max", String.valueOf(maxWarnings));

            sendMessage(sender, "success.warn", placeholders);

            // Broadcast warning
            if (plugin.getConfigManager().getBoolean("broadcast.warn", true)) {
                broadcastPunishment(punishment);
            }

            // Show warning count to player
            target.sendMessage(plugin.getMessagesManager().getColoredMessage("punishment.warning-count", placeholders));

            // Check auto-ban
            if (warnings >= maxWarnings && plugin.getConfigManager().isAutoBanEnabled()) {
                // Auto-ban will be handled by PunishmentManager
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
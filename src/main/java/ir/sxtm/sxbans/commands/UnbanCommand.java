package ir.sxtm.sxbans.commands;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class UnbanCommand extends BaseCommand {

    public UnbanCommand(SXBans plugin) {
        super(plugin);
    }

    @Override
    protected boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("sxbans.unban") || sender.hasPermission("sxbans.admin");
    }

    @Override
    protected boolean validateArgs(CommandSender sender, String[] args) {
        return args.length >= 1;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        String playerName = args[0];

        plugin.getSXBansLogger().info("Unban command executed for: " + playerName + " by: " + sender.getName());

        UUID targetUUID = getPlayerUUID(playerName);
        if (targetUUID == null) {
            sendMessage(sender, "error.player-not-found");
            plugin.getSXBansLogger().warning("Player not found: " + playerName);
            return true;
        }

        plugin.getSXBansLogger().info("Target UUID: " + targetUUID);

        if (!plugin.getPunishmentManager().isPlayerBanned(targetUUID)) {
            sendMessage(sender, "error.not-banned", Map.of("player", playerName));
            plugin.getSXBansLogger().info("Player is not banned: " + playerName);
            return true;
        }

        Punishment ban = plugin.getPunishmentManager().getActiveBan(targetUUID);
        if (ban == null) {
            sendMessage(sender, "error.not-banned", Map.of("player", playerName));
            plugin.getSXBansLogger().warning("Active ban is null for: " + playerName);
            return true;
        }

        plugin.getSXBansLogger().info("Found active ban: " + ban.getId() + " - Type: " + ban.getType());

        UUID removerUUID = sender instanceof Player ? ((Player) sender).getUniqueId() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");
        String removerName = sender.getName();
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";

        plugin.getSXBansLogger().info("Attempting to remove punishment: " + ban.getId());

        boolean success = plugin.getPunishmentManager().removePunishment(
                ban.getId(),
                removerUUID,
                removerName,
                reason
        );

        if (success) {
            sendRawMessage(sender, "&a" + playerName + " has been unbanned successfully!");

            plugin.getSXBansLogger().info("Player " + playerName + " was unbanned successfully by " + removerName);
        } else {
            sendRawMessage(sender, "&cFailed to unban " + playerName + "! Check console for errors.");
            plugin.getSXBansLogger().severe("Failed to unban " + playerName + " - removePunishment returned false");
        }

        return true;
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "command.unban.usage");
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
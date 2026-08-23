package ir.sxtm.sxbans.commands;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class UnmuteCommand extends BaseCommand {

    public UnmuteCommand(SXBans plugin) {
        super(plugin);
    }

    @Override
    protected boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("sxbans.unmute") || sender.hasPermission("sxbans.admin");
    }

    @Override
    protected boolean validateArgs(CommandSender sender, String[] args) {
        return args.length >= 1;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        String playerName = args[0];

        UUID targetUUID = getPlayerUUID(playerName);
        if (targetUUID == null) {
            sendMessage(sender, "error.player-not-found");
            return true;
        }

        if (!plugin.getPunishmentManager().isPlayerMuted(targetUUID)) {
            sendMessage(sender, "error.not-muted", Map.of("player", playerName));
            return true;
        }

        Punishment mute = plugin.getPunishmentManager().getActiveMute(targetUUID);
        if (mute == null) {
            sendMessage(sender, "error.not-muted", Map.of("player", playerName));
            return true;
        }

        UUID removerUUID = sender instanceof Player ? ((Player) sender).getUniqueId() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");
        String removerName = sender.getName();
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";

        boolean success = plugin.getPunishmentManager().removePunishment(
                mute.getId(),
                removerUUID,
                removerName,
                reason
        );

        if (success) {
            sendMessage(sender, "success.unmute", Map.of("player", playerName));

        }

        return true;
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "command.unmute.usage");
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {

            List<String> mutedPlayers = new ArrayList<>();

            return mutedPlayers;
        }
        return Collections.emptyList();
    }
}
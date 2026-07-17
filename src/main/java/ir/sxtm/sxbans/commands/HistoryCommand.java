package ir.sxtm.sxbans.commands;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryCommand extends BaseCommand {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public HistoryCommand(SXBans plugin) {
        super(plugin);
    }

    @Override
    protected boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("sxbans.history") || sender.hasPermission("sxbans.admin");
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

        List<Punishment> punishments = plugin.getPunishmentManager().getPlayerPunishments(targetUUID);

        if (punishments.isEmpty()) {
            sendMessage(sender, "history.no-records", Map.of("player", playerName));
            return true;
        }

        // Send header
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("history.header"));
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("history.title",
                Map.of("player", playerName)));
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("history.header"));

        // Send each punishment
        int page = 1;
        int maxPerPage = 10;
        int totalPages = (int) Math.ceil(punishments.size() / (double) maxPerPage);

        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        int start = (page - 1) * maxPerPage;
        int end = Math.min(start + maxPerPage, punishments.size());

        for (int i = start; i < end; i++) {
            Punishment p = punishments.get(i);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("type", p.getType().getDisplayName());
            placeholders.put("reason", p.getReason());
            placeholders.put("executor", p.getExecutorName());
            placeholders.put("status", p.getStatus().getDisplayName());
            placeholders.put("date", dateFormat.format(new Date(p.getStartTime())));
            placeholders.put("duration", p.getFormattedTimeRemaining());

            sender.sendMessage(plugin.getMessagesManager().getColoredMessage("history.entry", placeholders));
        }

        // Send footer with page info
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("history.footer"));
        if (totalPages > 1) {
            sender.sendMessage(plugin.getMessagesManager().getColoredMessage("history.page",
                    Map.of("page", String.valueOf(page), "total", String.valueOf(totalPages))));
        }

        return true;
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "command.history.usage");
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Return all players who have punishments
            List<String> players = new ArrayList<>();
            // This would need to get from storage
            return players;
        }
        return Collections.emptyList();
    }
}
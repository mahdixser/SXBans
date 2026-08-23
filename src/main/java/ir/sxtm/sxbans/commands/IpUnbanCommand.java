package ir.sxtm.sxbans.commands;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class IpUnbanCommand extends BaseCommand {

    public IpUnbanCommand(SXBans plugin) {
        super(plugin);
    }

    @Override
    protected boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("sxbans.ipunban") || sender.hasPermission("sxbans.admin");
    }

    @Override
    protected boolean validateArgs(CommandSender sender, String[] args) {
        return args.length >= 1;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        String ip = args[0];

        if (!ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
            sendMessage(sender, "error.invalid-ip");
            return true;
        }

        if (!plugin.getPunishmentManager().isIpBanned(ip)) {
            sendMessage(sender, "error.ip-not-found");
            return true;
        }

        Punishment ipBan = null;
        for (Punishment p : plugin.getPunishmentManager().getAllActivePunishments()) {
            if (p.getType() == Punishment.PunishmentType.IP_BAN &&
                    ip.equals(p.getIpAddress())) {
                ipBan = p;
                break;
            }
        }

        if (ipBan == null) {
            sendMessage(sender, "error.ip-not-found");
            return true;
        }

        UUID removerUUID = sender instanceof Player ? ((Player) sender).getUniqueId() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");
        String removerName = sender.getName();
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";

        boolean success = plugin.getPunishmentManager().removePunishment(
                ipBan.getId(),
                removerUUID,
                removerName,
                reason
        );

        if (success) {
            sendMessage(sender, "success.ipunban", Map.of("ip", ip));

        }

        return true;
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "command.ipunban.usage");
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {

            List<String> bannedIPs = new ArrayList<>();

            return bannedIPs;
        }
        return Collections.emptyList();
    }
}
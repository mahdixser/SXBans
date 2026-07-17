package ir.sxtm.sxbans.utils;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {
    private final SXBans plugin;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    // ===== HARD-CODED PERMISSIONS (غیرقابل تغییر توسط کاربر) =====
    private static final Map<String, String> PUNISHMENT_PERMISSIONS = new HashMap<>();

    static {
        PUNISHMENT_PERMISSIONS.put("ban", "sxbans.admin.punishview.ban");
        PUNISHMENT_PERMISSIONS.put("tempban", "sxbans.admin.punishview.tempban");
        PUNISHMENT_PERMISSIONS.put("unban", "sxbans.admin.punishview.unban");
        PUNISHMENT_PERMISSIONS.put("mute", "sxbans.admin.punishview.mute");
        PUNISHMENT_PERMISSIONS.put("tempmute", "sxbans.admin.punishview.tempmute");
        PUNISHMENT_PERMISSIONS.put("unmute", "sxbans.admin.punishview.unmute");
        PUNISHMENT_PERMISSIONS.put("kick", "sxbans.admin.punishview.kick");
        PUNISHMENT_PERMISSIONS.put("ipkick", "sxbans.admin.punishview.ipkick");
        PUNISHMENT_PERMISSIONS.put("warn", "sxbans.admin.punishview.warn");
        PUNISHMENT_PERMISSIONS.put("ipban", "sxbans.admin.punishview.ipban");
        PUNISHMENT_PERMISSIONS.put("ipunban", "sxbans.admin.punishview.ipunban");
        PUNISHMENT_PERMISSIONS.put("ipmute", "sxbans.admin.punishview.ipmute");
        PUNISHMENT_PERMISSIONS.put("ipunmute", "sxbans.admin.punishview.ipunmute");
    }

    public MessageUtils(SXBans plugin) {
        this.plugin = plugin;
    }

    public static String getPermissionForPunishment(String punishmentType) {
        return PUNISHMENT_PERMISSIONS.getOrDefault(
                punishmentType.toLowerCase(),
                "sxbans.admin.punishview." + punishmentType.toLowerCase()
        );
    }

    public void sendBroadcastWithPermission(String basePath, Map<String, String> placeholders, Punishment punishment) {
        String message = plugin.getMessagesManager().getBroadcastMessage(basePath);
        List<String> hoverLines = plugin.getMessagesManager().getHoverLines(basePath);
        boolean toEveryone = plugin.getMessagesManager().isBroadcastToEveryone(basePath);

        if (message == null || message.isEmpty()) {
            return;
        }

        String punishmentType = basePath.substring(basePath.lastIndexOf(".") + 1);
        String requiredPermission = getPermissionForPunishment(punishmentType);

        message = replacePlaceholders(message, placeholders);
        message = colorize(message);

        TextComponent messageComponent = new TextComponent(message);

        if (hoverLines != null && !hoverLines.isEmpty()) {
            List<String> processedHover = new ArrayList<>();
            for (String line : hoverLines) {
                line = replacePlaceholders(line, placeholders);
                processedHover.add(colorize(line));
            }
            String hoverText = String.join("\n", processedHover);
            messageComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    TextComponent.fromLegacyText(hoverText)));
        }

        if (toEveryone) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(messageComponent);
            }
            Bukkit.getConsoleSender().sendMessage(message);
        } else {
            boolean sentToAnyone = false;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(requiredPermission) || player.hasPermission("sxbans.admin.*")) {
                    player.spigot().sendMessage(messageComponent);
                    sentToAnyone = true;
                }
            }
            Bukkit.getConsoleSender().sendMessage(message);

            if (!sentToAnyone && !Bukkit.getOnlinePlayers().isEmpty()) {
                plugin.getSXBansLogger().debug("Broadcast sent to console only (permission: " + requiredPermission + ")");
            }
        }
    }

    public Map<String, String> formatPunishmentPlaceholders(Punishment punishment) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, String> placeholders = new HashMap<>();

        placeholders.put("player", punishment.getPlayerName());
        placeholders.put("executor", punishment.getExecutorName());
        placeholders.put("reason", punishment.getReason());
        placeholders.put("duration", punishment.getFormattedTimeRemaining());
        placeholders.put("date", sdf.format(new Date(punishment.getCreatedAt())));
        placeholders.put("server", punishment.getServerName() != null ? punishment.getServerName() : "Global");
        placeholders.put("ip", punishment.getIpAddress() != null ? punishment.getIpAddress() : "Unknown");

        if (!punishment.isPermanent()) {
            placeholders.put("expires", sdf.format(new Date(punishment.getEndTime())));
        }

        return placeholders;
    }

    public void sendHoverMessage(Player player, String message, String hoverText) {
        if (player == null) return;

        TextComponent text = new TextComponent(message);
        text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(hoverText)));

        player.spigot().sendMessage(text);
    }

    public String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) return null;
        if (placeholders == null) return text;

        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (entry.getValue() != null) {
                result = result.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return result;
    }

    public String colorize(String message) {
        if (message == null) return null;

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(sb, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(sb);

        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public String stripColor(String message) {
        if (message == null) return null;
        return net.md_5.bungee.api.ChatColor.stripColor(colorize(message));
    }

    public List<String> parseHover(List<String> lines, Map<String, String> placeholders) {
        if (lines == null) return new ArrayList<>();

        List<String> result = new ArrayList<>();
        for (String line : lines) {
            line = replacePlaceholders(line, placeholders);
            result.add(colorize(line));
        }
        return result;
    }
}
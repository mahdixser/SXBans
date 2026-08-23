package ir.sxtm.sxbans.config;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesManager {
    private final SXBans plugin;
    private FileConfiguration messages;
    private File messagesFile;
    private final Map<String, String> messageCache;
    private final Map<String, List<String>> hoverCache;
    private final Map<String, Boolean> broadcastToEveryoneCache;
    private String prefix;

    public MessagesManager(SXBans plugin) {
        this.plugin = plugin;
        this.messageCache = new HashMap<>();
        this.hoverCache = new HashMap<>();
        this.broadcastToEveryoneCache = new HashMap<>();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        loadFromResourceDefaults();

        cacheAllMessages();
        this.prefix = getMessage("prefix", "&8[&6SXBans&8] &7");
    }

    private void loadFromResourceDefaults() {

        messages = YamlConfiguration.loadConfiguration(messagesFile);

        try (java.io.InputStream defaultStream = plugin.getResource("messages.yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(defaultStream, java.nio.charset.StandardCharsets.UTF_8));
                messages.setDefaults(defaultConfig);
                messages.options().copyDefaults(true);
            }
        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Failed to load messages.yml defaults: " + e.getMessage());
        }

        saveMessages();
    }

    private void loadDefaults() {

        messages.addDefault("broadcast.ban.broadcast_to_everyone", false);
        messages.addDefault("broadcast.tempban.broadcast_to_everyone", true);
        messages.addDefault("broadcast.unban.broadcast_to_everyone", true);
        messages.addDefault("broadcast.mute.broadcast_to_everyone", false);
        messages.addDefault("broadcast.tempmute.broadcast_to_everyone", true);
        messages.addDefault("broadcast.unmute.broadcast_to_everyone", true);
        messages.addDefault("broadcast.kick.broadcast_to_everyone", false);
        messages.addDefault("broadcast.ipkick.broadcast_to_everyone", false);
        messages.addDefault("broadcast.warn.broadcast_to_everyone", false);
        messages.addDefault("broadcast.ipban.broadcast_to_everyone", false);
        messages.addDefault("broadcast.ipunban.broadcast_to_everyone", true);
        messages.addDefault("broadcast.ipmute.broadcast_to_everyone", false);
        messages.addDefault("broadcast.ipunmute.broadcast_to_everyone", true);

        messages.addDefault("broadcast.ban.message", "&c%player% &7was banned by &c%executor%");
        messages.addDefault("broadcast.tempban.message", "&c%player% &7was temporarily banned by &c%executor% &7(%duration%)");
        messages.addDefault("broadcast.unban.message", "&a%player% &7was unbanned by &a%executor%");
        messages.addDefault("broadcast.mute.message", "&c%player% &7was muted by &c%executor%");
        messages.addDefault("broadcast.tempmute.message", "&c%player% &7was temporarily muted by &c%executor% &7(%duration%)");
        messages.addDefault("broadcast.unmute.message", "&a%player% &7was unmuted by &a%executor%");
        messages.addDefault("broadcast.kick.message", "&c%player% &7was kicked by &c%executor%");
        messages.addDefault("broadcast.ipkick.message", "&c%player% &7was IP kicked by &c%executor%");
        messages.addDefault("broadcast.warn.message", "&c%player% &7was warned by &c%executor%");
        messages.addDefault("broadcast.ipban.message", "&cIP %ip% &7was banned by &c%executor%");
        messages.addDefault("broadcast.ipunban.message", "&aIP %ip% &7was unbanned by &a%executor%");
        messages.addDefault("broadcast.ipmute.message", "&cIP %ip% &7was muted by &c%executor%");
        messages.addDefault("broadcast.ipunmute.message", "&aIP %ip% &7was unmuted by &a%executor%");

        List<String> defaultHover = java.util.Arrays.asList(
                "&6&l=== SXBans Punishment ===",
                "&7Player: &f%player%",
                "&7Executor: &f%executor%",
                "&7Reason: &f%reason%",
                "&7Duration: &f%duration%",
                "&7Date: &f%date%",
                "&7Server: &f%server%",
                "&6&l========================"
        );

        messages.addDefault("broadcast.ban.hover", defaultHover);
        messages.addDefault("broadcast.tempban.hover", defaultHover);
        messages.addDefault("broadcast.unban.hover", defaultHover);
        messages.addDefault("broadcast.mute.hover", defaultHover);
        messages.addDefault("broadcast.tempmute.hover", defaultHover);
        messages.addDefault("broadcast.unmute.hover", defaultHover);
        messages.addDefault("broadcast.kick.hover", defaultHover);
        messages.addDefault("broadcast.ipkick.hover", defaultHover);
        messages.addDefault("broadcast.warn.hover", defaultHover);
        messages.addDefault("broadcast.ipban.hover", defaultHover);
        messages.addDefault("broadcast.ipunban.hover", defaultHover);
        messages.addDefault("broadcast.ipmute.hover", defaultHover);
        messages.addDefault("broadcast.ipunmute.hover", defaultHover);

        messages.addDefault("prefix", "&8[&6SXBans&8] &7");

        messages.addDefault("punishment.ban.message-padding", "center");
        List<String> banMessage = java.util.Arrays.asList(
                "&c&l═══════════════════════════",
                "&c&l   YOU HAVE BEEN BANNED!",
                "&c&l═══════════════════════════",
                "&7Reason: &f{reason}",
                "&7Duration: &f{duration}",
                "&7Executor: &f{executor}",
                "&c&l═══════════════════════════"
        );
        messages.addDefault("punishment.ban.message", banMessage);

        messages.addDefault("punishment.tempban.message-padding", "center");
        List<String> tempBanMessage = java.util.Arrays.asList(
                "&c&l═══════════════════════════",
                "&c&l   YOU HAVE BEEN TEMP-BANNED!",
                "&c&l═══════════════════════════",
                "&7Reason: &f{reason}",
                "&7Duration: &f{duration}",
                "&7Expires: &f{expires}",
                "&7Executor: &f{executor}",
                "&c&l═══════════════════════════"
        );
        messages.addDefault("punishment.tempban.message", tempBanMessage);

        messages.addDefault("punishment.mute.message-padding", "center");
        List<String> muteMessage = java.util.Arrays.asList(
                "&e&l═══════════════════════════",
                "&e&l    YOU HAVE BEEN MUTED!",
                "&e&l═══════════════════════════",
                "&7Reason: &f{reason}",
                "&7Duration: &f{duration}",
                "&7Executor: &f{executor}",
                "&e&l═══════════════════════════"
        );
        messages.addDefault("punishment.mute.message", muteMessage);

        messages.addDefault("punishment.tempmute.message-padding", "center");
        List<String> tempMuteMessage = java.util.Arrays.asList(
                "&e&l═══════════════════════════",
                "&e&l  YOU HAVE BEEN TEMP-MUTED!",
                "&e&l═══════════════════════════",
                "&7Reason: &f{reason}",
                "&7Duration: &f{duration}",
                "&7Expires: &f{expires}",
                "&7Executor: &f{executor}",
                "&e&l═══════════════════════════"
        );
        messages.addDefault("punishment.tempmute.message", tempMuteMessage);

        messages.addDefault("punishment.kick.message-padding", "center");
        List<String> kickMessage = java.util.Arrays.asList(
                "&c&l═══════════════════════════",
                "&c&l    YOU HAVE BEEN KICKED!",
                "&c&l═══════════════════════════",
                "&7Reason: &f{reason}",
                "&7Executor: &f{executor}",
                "&c&l═══════════════════════════"
        );
        messages.addDefault("punishment.kick.message", kickMessage);

        messages.addDefault("punishment.ipban.message-padding", "center");
        List<String> ipBanMessage = java.util.Arrays.asList(
                "&c&l═══════════════════════════",
                "&c&l   YOUR IP HAS BEEN BANNED!",
                "&c&l═══════════════════════════",
                "&7Reason: &f{reason}",
                "&c&l═══════════════════════════"
        );
        messages.addDefault("punishment.ipban.message", ipBanMessage);

        messages.addDefault("punishment.ipmute.message-padding", "center");
        List<String> ipMuteMessage = java.util.Arrays.asList(
                "&e&l═══════════════════════════",
                "&e&l   YOUR IP HAS BEEN MUTED!",
                "&e&l═══════════════════════════",
                "&7Reason: &f{reason}",
                "&e&l═══════════════════════════"
        );
        messages.addDefault("punishment.ipmute.message", ipMuteMessage);

        messages.addDefault("punishment.ipkick.message-padding", "center");
        List<String> ipKickMessage = java.util.Arrays.asList(
                "&c&l═══════════════════════════",
                "&c&l   YOUR IP HAS BEEN KICKED!",
                "&c&l═══════════════════════════",
                "&7Reason: &f{reason}",
                "&7Executor: &f{executor}",
                "&c&l═══════════════════════════"
        );
        messages.addDefault("punishment.ipkick.message", ipKickMessage);

        messages.addDefault("punishment.warn.message-padding", "center");
        List<String> warnMessage = java.util.Arrays.asList(
                "&e&l═══════════════════════════",
                "&e&l    YOU HAVE BEEN WARNED!",
                "&e&l═══════════════════════════",
                "&7Reason: &f{reason}",
                "&7Warning: &f{count}/{max}",
                "&7Executor: &f{executor}",
                "&e&l═══════════════════════════"
        );
        messages.addDefault("punishment.warn.message", warnMessage);

        messages.addDefault("punishment.unban.message-padding", "center");
        List<String> unbanMessage = java.util.Arrays.asList(
                "&a&l═══════════════════════════",
                "&a&l   YOU HAVE BEEN UNBANNED!",
                "&a&l═══════════════════════════",
                "&7Reason: &f{reason}",
                "&7Executor: &f{executor}",
                "&a&l═══════════════════════════"
        );
        messages.addDefault("punishment.unban.message", unbanMessage);

        messages.addDefault("punishment.unmute.message-padding", "center");
        List<String> unmuteMessage = java.util.Arrays.asList(
                "&a&l═══════════════════════════",
                "&a&l   YOU HAVE BEEN UNMUTED!",
                "&a&l═══════════════════════════",
                "&7Reason: &f{reason}",
                "&7Executor: &f{executor}",
                "&a&l═══════════════════════════"
        );
        messages.addDefault("punishment.unmute.message", unmuteMessage);

        messages.addDefault("punishment.warning-count.message-padding", "left");
        List<String> warningCountMessage = java.util.Arrays.asList(
                "&eYou have &f{count}/{max} &ewarnings!"
        );
        messages.addDefault("punishment.warning-count.message", warningCountMessage);

        messages.addDefault("punishment.auto-unban.message-padding", "center");
        List<String> autoUnbanMessage = java.util.Arrays.asList(
                "&a&l═══════════════════════════",
                "&a&l   YOUR BAN HAS EXPIRED!",
                "&a&l  You have been unbanned!",
                "&a&l═══════════════════════════"
        );
        messages.addDefault("punishment.auto-unban.message", autoUnbanMessage);

        messages.addDefault("punishment.auto-unmute.message-padding", "center");
        List<String> autoUnmuteMessage = java.util.Arrays.asList(
                "&a&l═══════════════════════════",
                "&a&l   YOUR MUTE HAS EXPIRED!",
                "&a&l  You have been unmuted!",
                "&a&l═══════════════════════════"
        );
        messages.addDefault("punishment.auto-unmute.message", autoUnmuteMessage);

        messages.addDefault("punishment.expired.message-padding", "center");
        List<String> expiredMessage = java.util.Arrays.asList(
                "&a&l═══════════════════════════",
                "&a&l  Your &f{type} &ahas expired!",
                "&a&l═══════════════════════════"
        );
        messages.addDefault("punishment.expired.message", expiredMessage);

        messages.addDefault("punishment.mute.filter.message-padding", "center");
        List<String> filterMessage = java.util.Arrays.asList(
                "&c&l═══════════════════════════",
                "&c&l  MESSAGE BLOCKED!",
                "&c&l═══════════════════════════",
                "&7Your message was blocked due to",
                "&7inappropriate language!",
                "&c&l═══════════════════════════"
        );
        messages.addDefault("punishment.mute.filter.message", filterMessage);

        messages.addDefault("punishment.mute.wildcard.message-padding", "center");
        List<String> wildcardMessage = java.util.Arrays.asList(
                "&c&l═══════════════════════════",
                "&c&l     YOU ARE MUTED!",
                "&c&l═══════════════════════════",
                "&7Your username contains",
                "&7restricted words!",
                "&c&l═══════════════════════════"
        );
        messages.addDefault("punishment.mute.wildcard.message", wildcardMessage);

        messages.addDefault("error.no-permission", "&cYou don't have permission to use this command!");
        messages.addDefault("error.player-not-found", "&cPlayer not found!");
        messages.addDefault("error.already-banned", "&c%player% is already banned!");
        messages.addDefault("error.already-muted", "&c%player% is already muted!");
        messages.addDefault("error.not-banned", "&c%player% is not banned!");
        messages.addDefault("error.not-muted", "&c%player% is not muted!");
        messages.addDefault("error.ip-not-found", "&cIP address not found or not banned!");
        messages.addDefault("error.invalid-ip", "&cInvalid IP address format!");
        messages.addDefault("error.invalid-time", "&cInvalid time format! Use: 1s, 1m, 1h, 1d, 1w, 1M, 1y");
        messages.addDefault("error.higher-level", "&cYou cannot punish this player because they have a higher/equal permission level!");
        messages.addDefault("error.self-punish", "&cYou cannot punish yourself!");

        messages.addDefault("success.ban", "&a%player% has been banned!");
        messages.addDefault("success.unban", "&a%player% has been unbanned!");
        messages.addDefault("success.mute", "&a%player% has been muted!");
        messages.addDefault("success.unmute", "&a%player% has been unmuted!");
        messages.addDefault("success.kick", "&a%player% has been kicked!");
        messages.addDefault("success.warn", "&a%player% has been warned! (%count%/%max%)");
        messages.addDefault("success.ipban", "&aIP %ip% has been banned!");
        messages.addDefault("success.ipunban", "&aIP %ip% has been unbanned!");
        messages.addDefault("success.ipmute", "&aIP %ip% has been muted!");
        messages.addDefault("success.ipunmute", "&aIP %ip% has been unmuted!");

        messages.addDefault("command.ban.usage", "&cUsage: /ban <player> <reason>");
        messages.addDefault("command.tempban.usage", "&cUsage: /tempban <player> <time> <reason>");
        messages.addDefault("command.unban.usage", "&cUsage: /unban <player>");
        messages.addDefault("command.mute.usage", "&cUsage: /mute <player> <reason>");
        messages.addDefault("command.tempmute.usage", "&cUsage: /tempmute <player> <time> <reason>");
        messages.addDefault("command.unmute.usage", "&cUsage: /unmute <player>");
        messages.addDefault("command.kick.usage", "&cUsage: /kick <player> <reason>");
        messages.addDefault("command.warn.usage", "&cUsage: /warn <player> <reason>");
        messages.addDefault("command.history.usage", "&cUsage: /history <player> [page]");
        messages.addDefault("command.check.usage", "&cUsage: /check <player>");
        messages.addDefault("command.ipban.usage", "&cUsage: /ipban <player/IP> <reason>");
        messages.addDefault("command.ipunban.usage", "&cUsage: /ipunban <IP>");
        messages.addDefault("command.ipmute.usage", "&cUsage: /ipmute <player/IP> <reason>");
        messages.addDefault("command.ipunmute.usage", "&cUsage: /ipunmute <IP>");

        messages.addDefault("history.header", "&6&m------------------------------");
        messages.addDefault("history.title", "&6&lHistory for &e%player%");
        messages.addDefault("history.entry", "&7%date% &8| &f%type% &8| &7%reason% &8| &7Executor: &f%executor% &8| &7Status: &f%status%");
        messages.addDefault("history.footer", "&6&m------------------------------");
        messages.addDefault("history.page", "&7Page &f%page% &7of &f%total%");
        messages.addDefault("history.no-records", "&eNo punishment records found for &f%player%");

        messages.addDefault("check.title", "&6&l=== Player Info: &e%player% &6===");
        messages.addDefault("check.status.clean", "&aThis player has no active punishments!");
        messages.addDefault("check.status.banned", "&c&lBANNED &7- Reason: &f%reason% &7- By: &f%executor% &7- &f%duration%");
        messages.addDefault("check.status.muted", "&e&lMUTED &7- Reason: &f%reason% &7- By: &f%executor% &7- &f%duration%");
        messages.addDefault("check.status.warnings", "&eWarnings: &f%count%/%max%");
        messages.addDefault("check.ip", "&7IP Address: &f%ip%");
        messages.addDefault("check.last-login", "&7Last seen: &f%time%");
        messages.addDefault("check.alt-accounts", "&eDetected &f%count% &ealt account(s) on this IP!");

        messages.addDefault("reload.config", "&aConfiguration reloaded!");
        messages.addDefault("reload.messages", "&aMessages reloaded!");
        messages.addDefault("reload.web-users", "&aWeb users reloaded!");
        messages.addDefault("reload.complete", "&a&lAll configurations reloaded successfully!");

        messages.addDefault("stats.total-punishments", "&7Total Punishments: &f%count%");
        messages.addDefault("stats.active-bans", "&7Active Bans: &f%count%");
        messages.addDefault("stats.active-mutes", "&7Active Mutes: &f%count%");
        messages.addDefault("stats.total-warnings", "&7Total Warnings: &f%count%");
        messages.addDefault("stats.web-status", "&7Web Server: %status%");
        messages.addDefault("stats.database-type", "&7Database: &f%type%");

        messages.addDefault("appeal.submitted", "&e%player% &7has submitted a ban appeal: &f%reason%");
        messages.addDefault("appeal.approved", "&aYour ban appeal has been approved! &7- &f%response%");
        messages.addDefault("appeal.denied", "&cYour ban appeal has been denied! &7- &f%response%");
        messages.addDefault("appeal.closed", "&eYour ban appeal has been closed.");

        messages.addDefault("alt-detection.found", "&e&l[Alt Detection] &7%player% &ehas &f%count% &ealt account(s)!");

        messages.addDefault("connection.limit", "&cToo many connections from your IP! (Max: %max%)");

        messages.options().copyDefaults(true);
        saveMessages();
    }

    private void cacheAllMessages() {
        messageCache.clear();
        hoverCache.clear();
        broadcastToEveryoneCache.clear();

        for (String key : messages.getKeys(true)) {
            if (messages.isString(key)) {
                messageCache.put(key, messages.getString(key));
            } else if (messages.isList(key)) {
                hoverCache.put(key, messages.getStringList(key));
            } else if (messages.isBoolean(key)) {
                broadcastToEveryoneCache.put(key, messages.getBoolean(key));
            }
        }
    }

    public boolean isBroadcastToEveryone(String basePath) {
        String path = basePath + ".broadcast_to_everyone";
        Boolean cached = broadcastToEveryoneCache.get(path);
        if (cached != null) {
            return cached;
        }
        boolean value = messages.getBoolean(path, false);
        broadcastToEveryoneCache.put(path, value);
        return value;
    }

    public List<String> getHoverLines(String basePath) {
        String path = basePath + ".hover";
        List<String> cached = hoverCache.get(path);
        if (cached != null) {
            return cached;
        }
        List<String> value = messages.getStringList(path);
        hoverCache.put(path, value);
        return value;
    }

    public String getBroadcastMessage(String basePath) {
        String path = basePath + ".message";
        return getMessage(path);
    }

    public void reloadMessages() {

        loadFromResourceDefaults();
        cacheAllMessages();
        this.prefix = getMessage("prefix", "&8[&6SXBans&8] &7");
    }

    public void saveMessages() {

        try (java.io.Writer writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(messagesFile), java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(messages.saveToString());
        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Failed to save messages: " + e.getMessage());
        }
    }

    public String getMessage(String path) {
        return getMessage(path, (String) null);
    }

    public String getMessage(String path, String defaultValue) {
        String message = messageCache.get(path);
        if (message == null) {
            message = messages.getString(path, defaultValue);
            if (message != null) {
                messageCache.put(path, message);
            }
        }
        return message;
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        if (message == null) return null;

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                if (entry.getValue() != null) {
                    message = message.replace("%" + entry.getKey() + "%", entry.getValue());
                }
            }
        }

        return message;
    }

    public List<String> getMessagesList(String path) {
        if (messages.isList(path)) {
            return messages.getStringList(path);
        }
        return null;
    }

    public String getMessageWithPadding(String path, Map<String, String> placeholders) {
        String padding = getString(path + ".message-padding", "left");
        List<String> lines = getMessagesList(path + ".message");

        if (lines == null || lines.isEmpty()) {
            return null;
        }

        List<String> processed = new ArrayList<>();
        for (String line : lines) {
            String processedLine = line;
            if (placeholders != null) {
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    if (entry.getValue() != null) {
                        processedLine = processedLine.replace("{" + entry.getKey() + "}", entry.getValue());
                        processedLine = processedLine.replace("%" + entry.getKey() + "%", entry.getValue());
                    }
                }
            }
            processed.add(processedLine);
        }

        int maxWidth = 0;
        for (String line : processed) {
            String plain = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', line));
            if (plain.length() > maxWidth) maxWidth = plain.length();
        }

        List<String> result = new ArrayList<>();
        for (String line : processed) {
            String colored = ChatColor.translateAlternateColorCodes('&', line);
            String plain = ChatColor.stripColor(colored);
            int paddingAmount = maxWidth - plain.length();

            String padded;
            switch (padding.toLowerCase()) {
                case "right":
                    padded = " ".repeat(Math.max(0, paddingAmount)) + colored;
                    break;
                case "center":
                case "middle":
                    int left = paddingAmount / 2;
                    int right = paddingAmount - left;
                    padded = " ".repeat(Math.max(0, left)) + colored + " ".repeat(Math.max(0, right));
                    break;
                case "left":
                default:
                    padded = colored + " ".repeat(Math.max(0, paddingAmount));
                    break;
            }
            result.add(padded);
        }

        return String.join("\n", result);
    }

    public String getColoredMessage(String path) {
        String message = getMessage(path);
        return colorize(message);
    }

    public String getColoredMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        if (message == null) return null;

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                if (entry.getValue() != null) {
                    message = message.replace("%" + entry.getKey() + "%", entry.getValue());
                }
            }
        }

        return colorize(message);
    }

    public String getPrefix() {
        return colorize(prefix);
    }

    public String getString(String path, String defaultValue) {
        return messages.getString(path, defaultValue);
    }

    public String getPunishmentBroadcast(Punishment punishment) {
        if (punishment == null) return null;

        String path = "broadcast." + punishment.getType().name().toLowerCase().replace("_", "");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", punishment.getPlayerName() != null ? punishment.getPlayerName() : "Unknown");
        placeholders.put("executor", punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown");
        placeholders.put("reason", punishment.getReason() != null ? punishment.getReason() : "No reason");
        placeholders.put("duration", punishment.getFormattedTimeRemaining() != null ? punishment.getFormattedTimeRemaining() : "Permanent");
        placeholders.put("ip", punishment.getIpAddress() != null ? punishment.getIpAddress() : "Unknown");
        placeholders.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(punishment.getCreatedAt())));
        placeholders.put("server", punishment.getServerName() != null ? punishment.getServerName() : "Global");

        String message = getColoredMessage(path + ".message", placeholders);
        if (message == null || message.isEmpty()) {
            return colorize("&6" + punishment.getType().getDisplayName() + " &7applied to &f" + punishment.getPlayerName() +
                    " &7by &f" + punishment.getExecutorName());
        }
        return message;
    }

    public String getBanMessage(Punishment punishment) {
        if (punishment == null) {
            return colorize("&c&lYou have been banned!");
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason() != null ? punishment.getReason() : "No reason provided");
        placeholders.put("duration", punishment.getFormattedTimeRemaining() != null ? punishment.getFormattedTimeRemaining() : "Permanent");
        placeholders.put("executor", punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown");
        placeholders.put("ip", punishment.getIpAddress() != null ? punishment.getIpAddress() : "Unknown");
        placeholders.put("expires", punishment.isPermanent() ? "Never" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(punishment.getEndTime())));

        String message = getMessageWithPadding("punishment.ban", placeholders);

        if (message == null || message.isEmpty()) {
            return colorize("&c&lYou have been banned from this server!\n&7Reason: &f" +
                    (punishment.getReason() != null ? punishment.getReason() : "No reason") +
                    "\n&7Duration: &f" + punishment.getFormattedTimeRemaining() +
                    "\n&7Executor: &f" + (punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown"));
        }

        return message;
    }

    public String getTempBanMessage(Punishment punishment) {
        if (punishment == null) {
            return colorize("&c&lYou have been temporarily banned!");
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason() != null ? punishment.getReason() : "No reason provided");
        placeholders.put("duration", punishment.getFormattedTimeRemaining() != null ? punishment.getFormattedTimeRemaining() : "Permanent");
        placeholders.put("executor", punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown");
        placeholders.put("expires", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(punishment.getEndTime())));

        String message = getMessageWithPadding("punishment.tempban", placeholders);

        if (message == null || message.isEmpty()) {
            return colorize("&c&lYou have been temporarily banned!\n&7Reason: &f" +
                    (punishment.getReason() != null ? punishment.getReason() : "No reason") +
                    "\n&7Duration: &f" + punishment.getFormattedTimeRemaining() +
                    "\n&7Expires: &f" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date(punishment.getEndTime())) +
                    "\n&7Executor: &f" + (punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown"));
        }

        return message;
    }

    public String getKickMessage(Punishment punishment) {
        if (punishment == null) {
            return colorize("&c&lYou have been kicked!");
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason() != null ? punishment.getReason() : "No reason provided");
        placeholders.put("executor", punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown");

        String message = getMessageWithPadding("punishment.kick", placeholders);

        if (message == null || message.isEmpty()) {
            return colorize("&c&lYou have been kicked from the server!\n&7Reason: &f" +
                    (punishment.getReason() != null ? punishment.getReason() : "No reason") +
                    "\n&7Executor: &f" + (punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown"));
        }

        return message;
    }

    public String getMuteMessage(Punishment punishment) {
        if (punishment == null) {
            return colorize("&c&lYou have been muted!");
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason() != null ? punishment.getReason() : "No reason provided");
        placeholders.put("duration", punishment.getFormattedTimeRemaining() != null ? punishment.getFormattedTimeRemaining() : "Permanent");
        placeholders.put("executor", punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown");
        placeholders.put("expires", punishment.isPermanent() ? "Never" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(punishment.getEndTime())));

        String message = getMessageWithPadding("punishment.mute", placeholders);

        if (message == null || message.isEmpty()) {
            return colorize("&c&lYou have been muted!\n&7Reason: &f" +
                    (punishment.getReason() != null ? punishment.getReason() : "No reason") +
                    "\n&7Duration: &f" + punishment.getFormattedTimeRemaining() +
                    "\n&7Executor: &f" + (punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown"));
        }

        return message;
    }

    public String getTempMuteMessage(Punishment punishment) {
        if (punishment == null) {
            return colorize("&c&lYou have been temporarily muted!");
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason() != null ? punishment.getReason() : "No reason provided");
        placeholders.put("duration", punishment.getFormattedTimeRemaining() != null ? punishment.getFormattedTimeRemaining() : "Permanent");
        placeholders.put("executor", punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown");
        placeholders.put("expires", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(punishment.getEndTime())));

        String message = getMessageWithPadding("punishment.tempmute", placeholders);

        if (message == null || message.isEmpty()) {
            return colorize("&c&lYou have been temporarily muted!\n&7Reason: &f" +
                    (punishment.getReason() != null ? punishment.getReason() : "No reason") +
                    "\n&7Duration: &f" + punishment.getFormattedTimeRemaining() +
                    "\n&7Expires: &f" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date(punishment.getEndTime())) +
                    "\n&7Executor: &f" + (punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown"));
        }

        return message;
    }

    public String getWarnMessage(Punishment punishment) {
        if (punishment == null) {
            return colorize("&e&lYou have been warned!");
        }

        int warnings = plugin.getPunishmentManager().getWarningCount(punishment.getPlayerUUID());
        int maxWarnings = plugin.getConfigManager().getMaxWarnings();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason() != null ? punishment.getReason() : "No reason provided");
        placeholders.put("executor", punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown");
        placeholders.put("count", String.valueOf(warnings));
        placeholders.put("max", String.valueOf(maxWarnings));

        String message = getMessageWithPadding("punishment.warn", placeholders);

        if (message == null || message.isEmpty()) {
            return colorize("&e&lYou have been warned!\n&7Reason: &f" +
                    (punishment.getReason() != null ? punishment.getReason() : "No reason") +
                    "\n&7Warning: &f" + warnings + "/" + maxWarnings);
        }

        return message;
    }

    public String getIpBanMessage(Punishment punishment) {
        if (punishment == null) {
            return colorize("&c&lYour IP has been banned!");
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason() != null ? punishment.getReason() : "No reason provided");
        placeholders.put("ip", punishment.getIpAddress() != null ? punishment.getIpAddress() : "Unknown");

        String message = getMessageWithPadding("punishment.ipban", placeholders);

        if (message == null || message.isEmpty()) {
            return colorize("&c&lYour IP has been banned!\n&7Reason: &f" +
                    (punishment.getReason() != null ? punishment.getReason() : "No reason"));
        }

        return message;
    }

    public String getIpMuteMessage(Punishment punishment) {
        if (punishment == null) {
            return colorize("&c&lYour IP has been muted!");
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason() != null ? punishment.getReason() : "No reason provided");
        placeholders.put("ip", punishment.getIpAddress() != null ? punishment.getIpAddress() : "Unknown");

        String message = getMessageWithPadding("punishment.ipmute", placeholders);

        if (message == null || message.isEmpty()) {
            return colorize("&c&lYour IP has been muted!\n&7Reason: &f" +
                    (punishment.getReason() != null ? punishment.getReason() : "No reason"));
        }

        return message;
    }

    public String getIpKickMessage(Punishment punishment) {
        if (punishment == null) {
            return colorize("&c&lYour IP has been kicked!");
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason() != null ? punishment.getReason() : "No reason provided");
        placeholders.put("executor", punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown");

        String message = getMessageWithPadding("punishment.ipkick", placeholders);

        if (message == null || message.isEmpty()) {
            return colorize("&c&lYour IP has been kicked!\n&7Reason: &f" +
                    (punishment.getReason() != null ? punishment.getReason() : "No reason") +
                    "\n&7Executor: &f" + (punishment.getExecutorName() != null ? punishment.getExecutorName() : "Unknown"));
        }

        return message;
    }

    public String getUnbanMessage(Punishment punishment) {
        if (punishment == null) {
            return colorize("&a&lYou have been unbanned!");
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getRemoveReason() != null ? punishment.getRemoveReason() : "No reason provided");
        placeholders.put("executor", punishment.getRemoverName() != null ? punishment.getRemoverName() : "Unknown");

        String message = getMessageWithPadding("punishment.unban", placeholders);

        if (message == null || message.isEmpty()) {
            return colorize("&a&lYou have been unbanned!\n&7Reason: &f" +
                    (punishment.getRemoveReason() != null ? punishment.getRemoveReason() : "No reason") +
                    "\n&7Executor: &f" + (punishment.getRemoverName() != null ? punishment.getRemoverName() : "Unknown"));
        }

        return message;
    }

    public String getUnmuteMessage(Punishment punishment) {
        if (punishment == null) {
            return colorize("&a&lYou have been unmuted!");
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getRemoveReason() != null ? punishment.getRemoveReason() : "No reason provided");
        placeholders.put("executor", punishment.getRemoverName() != null ? punishment.getRemoverName() : "Unknown");

        String message = getMessageWithPadding("punishment.unmute", placeholders);

        if (message == null || message.isEmpty()) {
            return colorize("&a&lYou have been unmuted!\n&7Reason: &f" +
                    (punishment.getRemoveReason() != null ? punishment.getRemoveReason() : "No reason") +
                    "\n&7Executor: &f" + (punishment.getRemoverName() != null ? punishment.getRemoverName() : "Unknown"));
        }

        return message;
    }

    public String getWarningCountMessage(Punishment punishment) {
        if (punishment == null) {
            return colorize("&eYou have warnings!");
        }

        int warnings = plugin.getPunishmentManager().getWarningCount(punishment.getPlayerUUID());
        int maxWarnings = plugin.getConfigManager().getMaxWarnings();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(warnings));
        placeholders.put("max", String.valueOf(maxWarnings));

        String message = getMessageWithPadding("punishment.warning-count", placeholders);

        if (message == null || message.isEmpty()) {
            return colorize("&eYou have &f" + warnings + "/" + maxWarnings + " &ewarnings!");
        }

        return message;
    }

    public String getAutoUnbanMessage() {
        String message = getMessageWithPadding("punishment.auto-unban", null);
        if (message == null || message.isEmpty()) {
            return colorize("&a&lYour ban has expired and you have been automatically unbanned!");
        }
        return message;
    }

    public String getAutoUnmuteMessage() {
        String message = getMessageWithPadding("punishment.auto-unmute", null);
        if (message == null || message.isEmpty()) {
            return colorize("&a&lYour mute has expired and you have been automatically unmuted!");
        }
        return message;
    }

    public String getExpiredMessage(String type) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("type", type != null ? type : "punishment");

        String message = getMessageWithPadding("punishment.expired", placeholders);
        if (message == null || message.isEmpty()) {
            return colorize("&aYour &f" + type + " &ahas expired!");
        }
        return message;
    }

    public String getMuteFilterMessage() {
        String message = getMessageWithPadding("punishment.mute.filter", null);
        if (message == null || message.isEmpty()) {
            return colorize("&cYour message was blocked due to inappropriate language!");
        }
        return message;
    }

    public String getMuteWildcardMessage() {
        String message = getMessageWithPadding("punishment.mute.wildcard", null);
        if (message == null || message.isEmpty()) {
            return colorize("&cYou are muted due to your username containing restricted words!");
        }
        return message;
    }

    public String colorize(String message) {
        if (message == null) return null;
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String stripColor(String message) {
        if (message == null) return null;
        return ChatColor.stripColor(message);
    }

    public String[] getColoredLines(String message) {
        if (message == null) return new String[0];
        String colored = colorize(message);
        return colored.split("\n");
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public void setMessages(FileConfiguration messages) {
        this.messages = messages;
    }

    public File getMessagesFile() {
        return messagesFile;
    }

    public void setMessagesFile(File messagesFile) {
        this.messagesFile = messagesFile;
    }

    public Map<String, String> getMessageCache() {
        return messageCache;
    }

    public Map<String, List<String>> getHoverCache() {
        return hoverCache;
    }

    public Map<String, Boolean> getBroadcastToEveryoneCache() {
        return broadcastToEveryoneCache;
    }
}
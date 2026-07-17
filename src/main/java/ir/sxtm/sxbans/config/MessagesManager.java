package ir.sxtm.sxbans.config;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
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
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        loadDefaults();
        cacheAllMessages();
        this.prefix = getMessage("prefix", "&8[&6SXBans&8] &7");
    }

    private void loadDefaults() {
        // Default broadcast_to_everyone (قابل تغییر توسط کاربر)
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

        // Default messages
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

        // Default hover messages (قابل تغییر توسط کاربر)
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

    // ===== User can only change these values =====

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

    // ===== Rest of existing methods =====

    public void reloadMessages() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        cacheAllMessages();
        this.prefix = getMessage("prefix", "&8[&6SXBans&8] &7");
    }

    public void saveMessages() {
        try {
            messages.save(messagesFile);
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

    public String getColoredMessage(String path) {
        return colorize(getMessage(path));
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

    public String getPunishmentBroadcast(Punishment punishment) {
        String path = "broadcast." + punishment.getType().name().toLowerCase();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", punishment.getPlayerName());
        placeholders.put("executor", punishment.getExecutorName());
        placeholders.put("reason", punishment.getReason());
        placeholders.put("duration", punishment.getFormattedTimeRemaining());
        placeholders.put("ip", punishment.getIpAddress() != null ? punishment.getIpAddress() : "Unknown");

        return getColoredMessage(path, placeholders);
    }

    public String getBanMessage(Punishment punishment) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason());
        placeholders.put("duration", punishment.getFormattedTimeRemaining());
        placeholders.put("executor", punishment.getExecutorName());
        placeholders.put("ip", punishment.getIpAddress() != null ? punishment.getIpAddress() : "Unknown");

        return getColoredMessage("punishment.ban.message", placeholders);
    }

    public String getKickMessage(Punishment punishment) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason());
        placeholders.put("executor", punishment.getExecutorName());

        return getColoredMessage("punishment.kick.message", placeholders);
    }

    public String getMuteMessage(Punishment punishment) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason());
        placeholders.put("duration", punishment.getFormattedTimeRemaining());
        placeholders.put("executor", punishment.getExecutorName());

        return getColoredMessage("punishment.mute.message", placeholders);
    }

    public String getWarnMessage(Punishment punishment) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reason", punishment.getReason());
        placeholders.put("executor", punishment.getExecutorName());
        placeholders.put("count", String.valueOf(plugin.getPunishmentManager().getWarningCount(punishment.getPlayerUUID())));
        placeholders.put("max", String.valueOf(plugin.getConfigManager().getMaxWarnings()));

        return getColoredMessage("punishment.warn.message", placeholders);
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
}
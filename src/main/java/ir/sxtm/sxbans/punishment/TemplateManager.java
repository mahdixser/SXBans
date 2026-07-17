package ir.sxtm.sxbans.punishment;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TemplateManager {
    private final SXBans plugin;
    private final Map<String, PunishmentTemplate> templates;
    private File templatesFile;
    private FileConfiguration config;

    public TemplateManager(SXBans plugin) {
        this.plugin = plugin;
        this.templates = new ConcurrentHashMap<>();
        loadTemplates();
    }

    public void loadTemplates() {
        templatesFile = new File(plugin.getDataFolder(), "templates.yml");

        if (!templatesFile.exists()) {
            plugin.saveResource("templates.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(templatesFile);
        loadDefaults();
        loadTemplatesFromConfig();
    }

    private void loadDefaults() {
        // Default templates
        config.addDefault("templates.hacking.name", "Hacking");
        config.addDefault("templates.hacking.type", "TEMP_BAN");
        config.addDefault("templates.hacking.duration", "7d");
        config.addDefault("templates.hacking.reason", "&cUsing hacked client or unfair advantages");
        config.addDefault("templates.hacking.broadcast", true);

        config.addDefault("templates.spam.name", "Spam");
        config.addDefault("templates.spam.type", "TEMP_MUTE");
        config.addDefault("templates.spam.duration", "1h");
        config.addDefault("templates.spam.reason", "&cSpamming in chat");
        config.addDefault("templates.spam.broadcast", false);

        config.addDefault("templates.abuse.name", "Abuse");
        config.addDefault("templates.abuse.type", "TEMP_BAN");
        config.addDefault("templates.abuse.duration", "1d");
        config.addDefault("templates.abuse.reason", "&cAbusive behavior towards staff or players");
        config.addDefault("templates.abuse.broadcast", true);

        config.addDefault("templates.griefing.name", "Griefing");
        config.addDefault("templates.griefing.type", "TEMP_BAN");
        config.addDefault("templates.griefing.duration", "3d");
        config.addDefault("templates.griefing.reason", "&cGriefing other players' builds");
        config.addDefault("templates.griefing.broadcast", true);

        config.addDefault("templates.xray.name", "X-Ray");
        config.addDefault("templates.xray.type", "BAN");
        config.addDefault("templates.xray.duration", "-1");
        config.addDefault("templates.xray.reason", "&cUsing X-Ray texture pack or mod");
        config.addDefault("templates.xray.broadcast", true);

        config.options().copyDefaults(true);
        saveTemplates();
    }

    private void loadTemplatesFromConfig() {
        templates.clear();

        ConfigurationSection section = config.getConfigurationSection("templates");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection templateSection = section.getConfigurationSection(key);
            if (templateSection == null) continue;

            String name = templateSection.getString("name", key);
            String typeStr = templateSection.getString("type", "TEMP_BAN");
            String durationStr = templateSection.getString("duration", "1d");
            String reason = templateSection.getString("reason", "&cYou have been punished");
            boolean broadcast = templateSection.getBoolean("broadcast", true);

            PunishmentType type = parsePunishmentType(typeStr);
            long duration = parseDuration(durationStr);

            PunishmentTemplate template = new PunishmentTemplate(
                    key, name, type, duration, reason, broadcast
            );

            templates.put(key, template);
        }

        plugin.getLogger().info("Loaded " + templates.size() + " punishment templates");
    }

    private PunishmentType parsePunishmentType(String type) {
        try {
            return PunishmentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PunishmentType.TEMP_BAN;
        }
    }

    private long parseDuration(String duration) {
        if (duration.equals("-1") || duration.equalsIgnoreCase("permanent")) {
            return -1;
        }

        char unit = duration.charAt(duration.length() - 1);
        long amount = Long.parseLong(duration.substring(0, duration.length() - 1));

        switch (unit) {
            case 's': return amount * 1000;
            case 'm': return amount * 60 * 1000;
            case 'h': return amount * 60 * 60 * 1000;
            case 'd': return amount * 24 * 60 * 60 * 1000;
            case 'w': return amount * 7 * 24 * 60 * 60 * 1000;
            case 'M': return amount * 30 * 24 * 60 * 60 * 1000;
            case 'y': return amount * 365 * 24 * 60 * 60 * 1000;
            default: return -1;
        }
    }

    public PunishmentTemplate getTemplate(String name) {
        return templates.get(name.toLowerCase());
    }

    public List<PunishmentTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    public void saveTemplates() {
        try {
            config.save(templatesFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save templates: " + e.getMessage());
        }
    }

    public void reloadTemplates() {
        config = YamlConfiguration.loadConfiguration(templatesFile);
        loadTemplatesFromConfig();
    }

    public static class PunishmentTemplate {
        private final String id;
        private final String name;
        private final PunishmentType type;
        private final long duration;
        private final String reason;
        private final boolean broadcast;

        public PunishmentTemplate(String id, String name, PunishmentType type,
                                  long duration, String reason, boolean broadcast) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.duration = duration;
            this.reason = reason;
            this.broadcast = broadcast;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public PunishmentType getType() { return type; }
        public long getDuration() { return duration; }
        public String getReason() { return reason; }
        public boolean isBroadcast() { return broadcast; }

        public boolean isPermanent() { return duration <= 0; }
        public String getFormattedDuration() { return isPermanent() ? "Permanent" : formatDuration(duration); }

        private String formatDuration(long millis) {
            long seconds = millis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            long weeks = days / 7;
            long months = days / 30;
            long years = days / 365;

            if (years > 0) return years + "y " + (months % 12) + "M";
            if (months > 0) return months + "M " + (weeks % 4) + "w";
            if (weeks > 0) return weeks + "w " + (days % 7) + "d";
            if (days > 0) return days + "d " + (hours % 24) + "h";
            if (hours > 0) return hours + "h " + (minutes % 60) + "m";
            if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
            return seconds + "s";
        }
    }
}
package ir.sxtm.sxbans.config;

import ir.sxtm.sxbans.SXBans;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final SXBans plugin;
    private FileConfiguration config;
    private File configFile;
    private final Map<String, Object> configCache;

    public ConfigManager(SXBans plugin) {
        this.plugin = plugin;
        this.configCache = new HashMap<>();
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        loadFromResourceDefaults();
        cacheAllValues();
    }

    private void loadFromResourceDefaults() {

        config = YamlConfiguration.loadConfiguration(configFile);

        try (java.io.InputStream defaultStream = plugin.getResource("config.yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(defaultStream, java.nio.charset.StandardCharsets.UTF_8));
                config.setDefaults(defaultConfig);
            }
        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Failed to load config.yml resource defaults: " + e.getMessage());
        }

        loadDefaults();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    private void loadDefaults() {

        config.addDefault("database.type", "json");
        config.addDefault("database.host", "localhost");
        config.addDefault("database.port", 3306);
        config.addDefault("database.name", "sxbans");
        config.addDefault("database.username", "root");
        config.addDefault("database.password", "");

        config.addDefault("web.enabled", true);
        config.addDefault("web.port", 8080);
        config.addDefault("web.host", "0.0.0.0");
        config.addDefault("web.ssl.enabled", false);
        config.addDefault("web.ssl.keystore", "");
        config.addDefault("web.ssl.password", "");

        config.addDefault("redis.enabled", false);
        config.addDefault("redis.host", "localhost");
        config.addDefault("redis.port", 6379);
        config.addDefault("redis.password", "");
        config.addDefault("redis.database", 0);

        config.addDefault("network.server-name", "game-server");
        config.addDefault("network.sync-punishments", true);
        config.addDefault("network.bungee-messaging.enabled", false);

        config.addDefault("punishments.max-warnings", 5);
        config.addDefault("punishments.warning-expiry", 2592000000L);
        config.addDefault("punishments.auto-ban.enabled", true);
        config.addDefault("punishments.auto-ban.ban-duration", 86400000L);
        config.addDefault("punishments.temp-ban.default-duration", 86400000L);
        config.addDefault("punishments.temp-mute.default-duration", 3600000L);

        config.addDefault("logging.console", true);
        config.addDefault("logging.file", true);
        config.addDefault("logging.file.format", "json");
        config.addDefault("logging.web", true);
        config.addDefault("logging.discord.enabled", false);
        config.addDefault("logging.discord.webhook-url", "");

        config.options().copyDefaults(true);
        saveConfig();
    }

    private void cacheAllValues() {
        configCache.clear();
        for (String key : config.getKeys(true)) {
            configCache.put(key, config.get(key));
        }
    }

    public void reloadConfig() {

        loadFromResourceDefaults();
        cacheAllValues();
    }

    public void saveConfig() {

        try (java.io.Writer writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(configFile), java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(config.saveToString());
        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Failed to save config: " + e.getMessage());
        }
    }

    public String getString(String path) {
        return config.getString(path);
    }

    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    public int getInt(String path) {
        return config.getInt(path);
    }

    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    public long getLong(String path) {
        return config.getLong(path);
    }

    public long getLong(String path, long defaultValue) {
        return config.getLong(path, defaultValue);
    }

    public double getDouble(String path) {
        return config.getDouble(path);
    }

    public ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }

    public boolean contains(String path) {
        return config.contains(path);
    }

    public String getDatabaseType() {
        return getString("database.type", "json");
    }

    public boolean isWebEnabled() {
        return getBoolean("web.enabled", true);
    }

    public int getWebPort() {
        return getInt("web.port", 8080);
    }

    public String getWebHost() {
        return getString("web.host", "0.0.0.0");
    }

    public boolean isRedisEnabled() {
        return getBoolean("redis.enabled", false);
    }

    public String getServerName() {
        return getString("network.server-name", "game-server");
    }

    public boolean isNetworkSyncEnabled() {
        return isRedisEnabled() && getBoolean("network.sync-punishments", true);
    }

    public boolean isBungeeMessagingEnabled() {
        return getBoolean("network.bungee-messaging.enabled", false);
    }

    public int getMaxWarnings() {
        return getInt("punishments.max-warnings", 5);
    }

    public long getWarningExpiry() {
        return getLong("punishments.warning-expiry", 2592000000L);
    }

    public boolean isAutoBanEnabled() {
        return getBoolean("punishments.auto-ban.enabled", true);
    }

    public long getAutoBanDuration() {
        return getLong("punishments.auto-ban.ban-duration", 86400000L);
    }

    public boolean isConsoleLogging() {
        return getBoolean("logging.console", true);
    }

    public boolean isFileLogging() {
        return getBoolean("logging.file", true);
    }

    public boolean isWebLogging() {
        return getBoolean("logging.web", true);
    }

    public String getMessage(String path) {
        return getString("messages." + path);
    }
}
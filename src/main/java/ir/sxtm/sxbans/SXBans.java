package ir.sxtm.sxbans;

import ir.sxtm.sxbans.api.SXBansAPI;
import ir.sxtm.sxbans.commands.*;
import ir.sxtm.sxbans.config.ConfigManager;
import ir.sxtm.sxbans.config.MessagesManager;
import ir.sxtm.sxbans.config.WebUsersManager;
import ir.sxtm.sxbans.database.DatabaseManager;
import ir.sxtm.sxbans.database.PunishmentStorage;
import ir.sxtm.sxbans.database.RedisManager;
import ir.sxtm.sxbans.hooks.HookManager;
import ir.sxtm.sxbans.listeners.ChatListener;
import ir.sxtm.sxbans.listeners.ConnectionListener;
import ir.sxtm.sxbans.listeners.PlayerListener;
import ir.sxtm.sxbans.proxy.ProxyManager;
import ir.sxtm.sxbans.punishment.*;
import ir.sxtm.sxbans.utils.*;
import ir.sxtm.sxbans.web.WebServer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SXBans extends JavaPlugin {
    private static SXBans instance;

    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private WebUsersManager webUsersManager;
    private DatabaseManager databaseManager;
    private PunishmentStorage punishmentStorage;
    private RedisManager redisManager;
    private PunishmentManager punishmentManager;
    private HookManager hookManager;
    private ProxyManager proxyManager;
    private WebServer webServer;
    private SXBansAPI api;

    private SXBansLogger logger;
    private CacheManager cacheManager;
    private RateLimiter rateLimiter;
    private ThreadPoolManager threadPoolManager;
    private ExportImportManager exportImportManager;

    private BanAppealManager banAppealManager;
    private BanWaveManager banWaveManager;
    private AutoBanManager autoBanManager;
    private IPTracker ipTracker;
    private AltAccountDetector altAccountDetector;
    private TemplateManager templateManager;
    private ScheduledPunishmentManager scheduledPunishmentManager;

    private ExecutorService executorService;
    private boolean pluginEnabled = false;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;

        try {

            executorService = Executors.newCachedThreadPool();

            configManager = new ConfigManager(this);
            configManager.loadConfig();

            logger = new SXBansLogger(this);
            logger.info("Starting SXBans v" + getDescription().getVersion() + "...");

            extractWebFiles();

            threadPoolManager = new ThreadPoolManager(this);
            logger.debug("Thread pool manager initialized");

            messagesManager = new MessagesManager(this);
            messagesManager.loadMessages();
            logger.debug("Messages loaded");

            webUsersManager = new WebUsersManager(this);
            webUsersManager.loadUsers();
            logger.debug("Web users loaded");

            cacheManager = new CacheManager(this);
            logger.debug("Cache manager initialized");

            rateLimiter = new RateLimiter(this);
            logger.debug("Rate limiter initialized");

            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            logger.debug("Database initialized");

            if (configManager.isRedisEnabled()) {
                redisManager = new RedisManager(this);
                redisManager.initialize();
                logger.debug("Redis initialized");
            }

            punishmentStorage = new PunishmentStorage(this);
            punishmentStorage.initialize();
            logger.debug("Punishment storage initialized");

            punishmentManager = new PunishmentManager(this);
            punishmentManager.initialize();
            logger.debug("Punishment manager initialized");

            banAppealManager = new BanAppealManager(this);
            banWaveManager = new BanWaveManager(this);
            autoBanManager = new AutoBanManager(this);
            ipTracker = new IPTracker(this);
            altAccountDetector = new AltAccountDetector(this);
            templateManager = new TemplateManager(this);
            scheduledPunishmentManager = new ScheduledPunishmentManager(this);
            logger.debug("Punishment features initialized");

            proxyManager = new ProxyManager(this);
            proxyManager.initialize();
            logger.debug("Proxy manager initialized");

            hookManager = new HookManager(this);
            hookManager.initialize();
            logger.debug("Hook manager initialized");

            exportImportManager = new ExportImportManager(this);
            logger.debug("Export/Import manager initialized");

            registerCommands();
            logger.debug("Commands registered");

            registerListeners();
            logger.debug("Listeners registered");

            if (configManager.isWebEnabled()) {
                webServer = new WebServer(this);
                webServer.start();
                logger.debug("Web server started on port " + configManager.getWebPort());
            }

            api = new SXBansAPI(this);
            logger.debug("API initialized");

            registerShutdownHook();

            pluginEnabled = true;
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("SXBans v" + getDescription().getVersion() + " enabled successfully in " + elapsed + "ms!");

        } catch (Exception e) {
            if (logger != null) {
                logger.severe("Failed to enable SXBans: " + e.getMessage());
            } else {
                getLogger().severe("Failed to enable SXBans: " + e.getMessage());
            }
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void extractWebFiles() {
        try {
            File webDir = new File(getDataFolder(), "web");
            if (!webDir.exists()) {
                webDir.mkdirs();
            }

            String[] htmlFiles = {"login.html", "dashboard.html", "players.html",
                    "history.html", "console.html", "settings.html", "player-card.html"};
            for (String file : htmlFiles) {
                extractFile("/web/html/" + file, new File(webDir, "html/" + file));
            }

            String[] cssFiles = {"style.css", "glassmorphism.css", "neumorphism.css"};
            for (String file : cssFiles) {
                extractFile("/web/css/" + file, new File(webDir, "css/" + file));
            }

            String[] jsFiles = {"main.js", "dashboard.js", "players.js", "console.js", "animations.js"};
            for (String file : jsFiles) {
                extractFile("/web/js/" + file, new File(webDir, "js/" + file));
            }

            logger.info("Web files extracted successfully to: " + webDir.getAbsolutePath());
        } catch (Exception e) {
            logger.warning("Failed to extract web files: " + e.getMessage());
        }
    }

    private void extractFile(String resourcePath, File targetFile) {
        try {

            if (targetFile.exists()) {
                return;
            }

            targetFile.getParentFile().mkdirs();

            try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    logger.warning("Resource not found in JAR: " + resourcePath);
                    return;
                }

                try (FileOutputStream out = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }

                logger.debug("Extracted: " + resourcePath + " -> " + targetFile.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.warning("Failed to extract file " + resourcePath + ": " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (!pluginEnabled) return;

        if (logger != null) {
            logger.info("Disabling SXBans...");
        }

        try {

            if (webServer != null) {
                webServer.stop();
                if (logger != null) logger.debug("Web server stopped");
            }

            if (scheduledPunishmentManager != null) {
                scheduledPunishmentManager.shutdown();
            }

            if (punishmentManager != null) {
                punishmentManager.saveAll();
                if (logger != null) logger.debug("Punishments saved");
            }

            if (databaseManager != null) {
                databaseManager.close();
                if (logger != null) logger.debug("Database connections closed");
            }

            if (redisManager != null) {
                redisManager.close();
                if (logger != null) logger.debug("Redis connection closed");
            }

            if (threadPoolManager != null) {
                threadPoolManager.shutdown();
                if (logger != null) logger.debug("Thread pool shut down");
            }

            if (cacheManager != null) {
                cacheManager.clearAll();
                if (logger != null) logger.debug("Caches cleared");
            }

            pluginEnabled = false;
            if (logger != null) {
                logger.info("SXBans disabled successfully!");
            }

        } catch (Exception e) {
            if (logger != null) {
                logger.severe("Error during disable: " + e.getMessage());
            } else {
                getLogger().severe("Error during disable: " + e.getMessage());
            }
            e.printStackTrace();
        }

        instance = null;
    }

    private void registerCommands() {

        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("tempban").setExecutor(new TempBanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));

        getCommand("kick").setExecutor(new KickCommand(this));

        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("tempmute").setExecutor(new TempMuteCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));

        getCommand("warn").setExecutor(new WarnCommand(this));

        getCommand("history").setExecutor(new HistoryCommand(this));
        getCommand("check").setExecutor(new CheckCommand(this));

        getCommand("ipban").setExecutor(new IpBanCommand(this));
        getCommand("ipunban").setExecutor(new IpUnbanCommand(this));
        getCommand("ipmute").setExecutor(new IpMuteCommand(this));
        getCommand("ipunmute").setExecutor(new IpUnmuteCommand(this));

        getCommand("sxbans").setExecutor(new SxBansCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (punishmentManager != null) {
                punishmentManager.saveAll();
            }
        }));
    }

    public static SXBans getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public WebUsersManager getWebUsersManager() {
        return webUsersManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PunishmentStorage getPunishmentStorage() {
        return punishmentStorage;
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    public WebServer getWebServer() {
        return webServer;
    }

    public SXBansAPI getAPI() {
        return api;
    }

    public SXBansLogger getSXBansLogger() {
        return logger;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public ThreadPoolManager getThreadPoolManager() {
        return threadPoolManager;
    }

    public ExportImportManager getExportImportManager() {
        return exportImportManager;
    }

    public BanAppealManager getBanAppealManager() {
        return banAppealManager;
    }

    public BanWaveManager getBanWaveManager() {
        return banWaveManager;
    }

    public AutoBanManager getAutoBanManager() {
        return autoBanManager;
    }

    public IPTracker getIPTracker() {
        return ipTracker;
    }

    public AltAccountDetector getAltAccountDetector() {
        return altAccountDetector;
    }

    public TemplateManager getTemplateManager() {
        return templateManager;
    }

    public ScheduledPunishmentManager getScheduledPunishmentManager() {
        return scheduledPunishmentManager;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public boolean isPluginEnabled() {
        return pluginEnabled;
    }
}
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

    // Managers
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

    // Utilities
    private SXBansLogger logger;
    private CacheManager cacheManager;
    private RateLimiter rateLimiter;
    private ThreadPoolManager threadPoolManager;
    private ExportImportManager exportImportManager;

    // Punishment features
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
            // 1. Initialize thread pool
            executorService = Executors.newCachedThreadPool();

            // 2. Initialize configuration
            configManager = new ConfigManager(this);
            configManager.loadConfig();

            // 3. Initialize logger
            logger = new SXBansLogger(this);
            logger.info("Starting SXBans v" + getDescription().getVersion() + "...");

            // 4. Extract web files to plugin folder
            extractWebFiles();

            // 5. Initialize thread pool manager
            threadPoolManager = new ThreadPoolManager(this);
            logger.debug("Thread pool manager initialized");

            // 6. Initialize messages
            messagesManager = new MessagesManager(this);
            messagesManager.loadMessages();
            logger.debug("Messages loaded");

            // 7. Initialize web users
            webUsersManager = new WebUsersManager(this);
            webUsersManager.loadUsers();
            logger.debug("Web users loaded");

            // 8. Initialize cache
            cacheManager = new CacheManager(this);
            logger.debug("Cache manager initialized");

            // 9. Initialize rate limiter
            rateLimiter = new RateLimiter(this);
            logger.debug("Rate limiter initialized");

            // 10. Initialize database
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            logger.debug("Database initialized");

            // 11. Initialize Redis
            if (configManager.isRedisEnabled()) {
                redisManager = new RedisManager(this);
                redisManager.initialize();
                logger.debug("Redis initialized");
            }

            // 12. Initialize storage
            punishmentStorage = new PunishmentStorage(this);
            punishmentStorage.initialize();
            logger.debug("Punishment storage initialized");

            // 13. Initialize punishment manager
            punishmentManager = new PunishmentManager(this);
            punishmentManager.initialize();
            logger.debug("Punishment manager initialized");

            // 14. Initialize ban features
            banAppealManager = new BanAppealManager(this);
            banWaveManager = new BanWaveManager(this);
            autoBanManager = new AutoBanManager(this);
            ipTracker = new IPTracker(this);
            altAccountDetector = new AltAccountDetector(this);
            templateManager = new TemplateManager(this);
            scheduledPunishmentManager = new ScheduledPunishmentManager(this);
            logger.debug("Punishment features initialized");

            // 15. Initialize proxy
            proxyManager = new ProxyManager(this);
            proxyManager.initialize();
            logger.debug("Proxy manager initialized");

            // 16. Initialize hooks
            hookManager = new HookManager(this);
            hookManager.initialize();
            logger.debug("Hook manager initialized");

            // 17. Initialize export/import
            exportImportManager = new ExportImportManager(this);
            logger.debug("Export/Import manager initialized");

            // 18. Register commands
            registerCommands();
            logger.debug("Commands registered");

            // 19. Register listeners
            registerListeners();
            logger.debug("Listeners registered");

            // 20. Initialize web server
            if (configManager.isWebEnabled()) {
                webServer = new WebServer(this);
                webServer.start();
                logger.debug("Web server started on port " + configManager.getWebPort());
            }

            // 21. Initialize API
            api = new SXBansAPI(this);
            logger.debug("API initialized");

            // 22. Register shutdown hook
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

    /**
     * Extract web files (HTML, CSS, JS) from JAR to plugin folder.
     */
    private void extractWebFiles() {
        try {
            File webDir = new File(getDataFolder(), "web");
            if (!webDir.exists()) {
                webDir.mkdirs();
            }

            // Extract HTML files
            String[] htmlFiles = {"login.html", "dashboard.html", "players.html",
                    "history.html", "console.html", "settings.html", "player-card.html"};
            for (String file : htmlFiles) {
                extractFile("/web/html/" + file, new File(webDir, "html/" + file));
            }

            // Extract CSS files
            String[] cssFiles = {"style.css", "glassmorphism.css", "neumorphism.css"};
            for (String file : cssFiles) {
                extractFile("/web/css/" + file, new File(webDir, "css/" + file));
            }

            // Extract JS files
            String[] jsFiles = {"main.js", "dashboard.js", "players.js", "console.js", "animations.js"};
            for (String file : jsFiles) {
                extractFile("/web/js/" + file, new File(webDir, "js/" + file));
            }

            logger.info("Web files extracted successfully to: " + webDir.getAbsolutePath());
        } catch (Exception e) {
            logger.warning("Failed to extract web files: " + e.getMessage());
        }
    }

    /**
     * Extract a single file from JAR to external folder.
     */
    private void extractFile(String resourcePath, File targetFile) {
        try {
            // اگر فایل از قبل وجود دارد، آن را دوباره استخراج نکن
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
            // Shutdown web server
            if (webServer != null) {
                webServer.stop();
                if (logger != null) logger.debug("Web server stopped");
            }

            // Shutdown scheduled tasks
            if (scheduledPunishmentManager != null) {
                scheduledPunishmentManager.shutdown();
            }

            // Save all data
            if (punishmentManager != null) {
                punishmentManager.saveAll();
                if (logger != null) logger.debug("Punishments saved");
            }

            // Close database connections
            if (databaseManager != null) {
                databaseManager.close();
                if (logger != null) logger.debug("Database connections closed");
            }

            // Close Redis
            if (redisManager != null) {
                redisManager.close();
                if (logger != null) logger.debug("Redis connection closed");
            }

            // Shutdown thread pool
            if (threadPoolManager != null) {
                threadPoolManager.shutdown();
                if (logger != null) logger.debug("Thread pool shut down");
            }

            // Clear caches
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
        // Ban commands
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("tempban").setExecutor(new TempBanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));

        // Kick commands
        getCommand("kick").setExecutor(new KickCommand(this));

        // Mute commands
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("tempmute").setExecutor(new TempMuteCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));

        // Warn command
        getCommand("warn").setExecutor(new WarnCommand(this));

        // Info commands
        getCommand("history").setExecutor(new HistoryCommand(this));
        getCommand("check").setExecutor(new CheckCommand(this));

        // IP commands
        getCommand("ipban").setExecutor(new IpBanCommand(this));
        getCommand("ipunban").setExecutor(new IpUnbanCommand(this));
        getCommand("ipmute").setExecutor(new IpMuteCommand(this));
        getCommand("ipunmute").setExecutor(new IpUnmuteCommand(this));

        // Admin command
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

    // ========== Getters ==========

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
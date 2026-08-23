package ir.sxtm.sxbans.commands;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.database.DatabaseManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class SxBansCommand extends BaseCommand {

    public SxBansCommand(SXBans plugin) {
        super(plugin);
    }

    @Override
    protected boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("sxbans.admin");
    }

    @Override
    protected boolean validateArgs(CommandSender sender, String[] args) {
        return args.length >= 1;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "stats":
                handleStats(sender);
                break;
            case "web":
                handleWeb(sender, args);
                break;
            case "backup":
                handleBackup(sender);
                break;
            case "clear":
                handleClear(sender, args);
                break;
            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        try {

            plugin.getConfigManager().reloadConfig();
            sender.sendMessage(plugin.getMessagesManager().getColoredMessage("reload.config"));

            plugin.getMessagesManager().reloadMessages();
            sender.sendMessage(plugin.getMessagesManager().getColoredMessage("reload.messages"));

            plugin.getWebUsersManager().loadUsers();
            sender.sendMessage(plugin.getMessagesManager().getColoredMessage("reload.web-users"));

            plugin.getTemplateManager().reloadTemplates();
            sender.sendMessage("&aTemplates reloaded!");

            sender.sendMessage(plugin.getMessagesManager().getColoredMessage("reload.complete"));
        } catch (Exception e) {
            sender.sendMessage("&cFailed to reload: " + e.getMessage());
        }
    }

    private void handleStats(CommandSender sender) {
        Map<String, String> placeholders = new HashMap<>();

        int totalPunishments = plugin.getPunishmentManager().getAllActivePunishments().size();
        placeholders.put("count", String.valueOf(totalPunishments));
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("stats.total-punishments", placeholders));

        int activeBans = plugin.getPunishmentManager().getAllActivePunishments().stream()
                .filter(p -> p.getType().toString().contains("BAN"))
                .toArray().length;
        placeholders.put("count", String.valueOf(activeBans));
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("stats.active-bans", placeholders));

        int activeMutes = plugin.getPunishmentManager().getAllActivePunishments().stream()
                .filter(p -> p.getType().toString().contains("MUTE"))
                .toArray().length;
        placeholders.put("count", String.valueOf(activeMutes));
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("stats.active-mutes", placeholders));

        int totalWarnings = plugin.getPunishmentManager().getAllActivePunishments().stream()
                .filter(p -> p.getType().toString().contains("WARN"))
                .toArray().length;
        placeholders.put("count", String.valueOf(totalWarnings));
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("stats.total-warnings", placeholders));

        boolean webEnabled = plugin.getConfigManager().isWebEnabled();
        placeholders.put("status", webEnabled ? "&aEnabled" : "&cDisabled");
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("stats.web-status", placeholders));

        DatabaseManager.DatabaseType dbType = plugin.getDatabaseManager().getType();
        placeholders.put("type", dbType.getName());
        sender.sendMessage(plugin.getMessagesManager().getColoredMessage("stats.database-type", placeholders));
    }

    private void handleWeb(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("&cUsage: /sxbans web <start|stop|status>");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "start":
                if (plugin.getWebServer() != null && plugin.getWebServer().isRunning()) {
                    sender.sendMessage("&cWeb server is already running!");
                } else {
                    plugin.getWebServer().start();
                    sender.sendMessage("&aWeb server started on port " + plugin.getConfigManager().getWebPort());
                }
                break;

            case "stop":
                if (plugin.getWebServer() != null && plugin.getWebServer().isRunning()) {
                    plugin.getWebServer().stop();
                    sender.sendMessage("&aWeb server stopped!");
                } else {
                    sender.sendMessage("&cWeb server is not running!");
                }
                break;

            case "status":
                boolean running = plugin.getWebServer() != null && plugin.getWebServer().isRunning();
                sender.sendMessage("&6Web Server Status: " + (running ? "&aRunning" : "&cStopped"));
                if (running) {
                    sender.sendMessage("&7Port: &f" + plugin.getConfigManager().getWebPort());
                    sender.sendMessage("&7Host: &f" + plugin.getConfigManager().getWebHost());
                }
                break;

            default:
                sender.sendMessage("&cUnknown action! Use: start, stop, or status");
                break;
        }
    }

    private void handleBackup(CommandSender sender) {

        sender.sendMessage("&aCreating backup...");
        try {
            java.io.File backupDir = new java.io.File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            java.util.List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();
            String fileName = "backup-" + new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date()) + ".json";
            java.io.File backupFile = new java.io.File(backupDir, fileName);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                    .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(backupFile, allPunishments);

            sender.sendMessage("&aBackup created successfully! (" + allPunishments.size() + " punishments) -> backups/" + fileName);
            plugin.getSXBansLogger().info("Backup created by " + sender.getName() + ": " + fileName);
        } catch (Exception e) {
            sender.sendMessage("&cFailed to create backup: " + e.getMessage());
            plugin.getSXBansLogger().severe("Backup failed: " + e.getMessage());
        }
    }

    private final Map<String, Long> pendingClearAllConfirmations = new HashMap<>();
    private static final long CONFIRM_TIMEOUT_MS = 30_000;

    private void handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("&cUsage: /sxbans clear <all|player> [name]");
            return;
        }

        String target = args[1].toLowerCase();

        if (target.equals("all")) {
            pendingClearAllConfirmations.put(sender.getName(), System.currentTimeMillis());
            sender.sendMessage("&cThis will PERMANENTLY delete ALL punishments from the database. " +
                    "This cannot be undone. Run /sxbans clear confirm within 30 seconds to proceed.");
            return;
        }

        if (target.equals("confirm")) {
            Long requestedAt = pendingClearAllConfirmations.remove(sender.getName());
            if (requestedAt == null || System.currentTimeMillis() - requestedAt > CONFIRM_TIMEOUT_MS) {
                sender.sendMessage("&cNo pending clear-all request (or it expired). Run /sxbans clear all first.");
                return;
            }

            int removed = plugin.getPunishmentManager().clearAllData();
            sender.sendMessage("&aAll punishments cleared! (" + removed + " records removed)");
            plugin.getSXBansLogger().severe(sender.getName() + " cleared ALL punishment data (" + removed + " records)");
            return;
        }

        String playerName = target.equals("player") && args.length >= 3 ? args[2] : args[1];
        UUID targetUUID = org.bukkit.Bukkit.getOfflinePlayer(playerName).getUniqueId();
        int removed = plugin.getPunishmentManager().clearPlayerData(targetUUID);

        if (removed > 0) {
            sender.sendMessage("&aCleared " + removed + " punishment(s) for " + playerName);
            plugin.getSXBansLogger().severe(sender.getName() + " cleared " + removed + " punishments for " + playerName);
        } else {
            sender.sendMessage("&7No punishments found for " + playerName);
        }
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sender.sendMessage("&6&lSXBans Commands:");
        sender.sendMessage("&7/sxbans reload &f- Reload configuration");
        sender.sendMessage("&7/sxbans stats &f- View statistics");
        sender.sendMessage("&7/sxbans web <start|stop|status> &f- Manage web server");
        sender.sendMessage("&7/sxbans backup &f- Create backup");
        sender.sendMessage("&7/sxbans clear <all|player> &f- Clear punishments");
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "stats", "web", "backup", "clear");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("web")) {
                return Arrays.asList("start", "stop", "status");
            }
            if (args[0].equalsIgnoreCase("clear")) {
                return Arrays.asList("all", "confirm");
            }
        }
        return Collections.emptyList();
    }
}
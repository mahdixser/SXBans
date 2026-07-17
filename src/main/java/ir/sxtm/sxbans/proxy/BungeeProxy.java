package ir.sxtm.sxbans.proxy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BungeeCord/Waterfall proxy for cross-server communication.
 */
public class BungeeProxy {
    private final SXBans plugin;
    private final Gson gson;
    private final Map<String, Long> lastSync;
    private boolean enabled;

    // Plugin channels
    private static final String CHANNEL = "sxbans:punish";
    private static final String SUB_CHANNEL = "sxbans:callback";

    public BungeeProxy(SXBans plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.lastSync = new ConcurrentHashMap<>();
        this.enabled = false;
    }

    /**
     * Initialize Bungee proxy.
     */
    public void initialize() {
        // Register plugin channel
        try {
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, SUB_CHANNEL,
                    (channel, player, message) -> handleCallback(message));
            enabled = true;
            plugin.getSXBansLogger().info("Bungee proxy initialized");
        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to initialize Bungee proxy: " + e.getMessage());
        }
    }

    /**
     * Handle callback from Bungee.
     */
    private void handleCallback(byte[] message) {
        try {
            String data = new String(message);
            JsonObject json = gson.fromJson(data, JsonObject.class);
            String type = json.get("type").getAsString();

            switch (type) {
                case "punishment":
                    handlePunishmentCallback(json);
                    break;
                case "banwave":
                    handleBanWaveCallback(json);
                    break;
                case "sync":
                    handleSyncCallback(json);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to handle Bungee callback: " + e.getMessage());
        }
    }

    /**
     * Handle punishment callback.
     */
    private void handlePunishmentCallback(JsonObject data) {
        String action = data.get("action").getAsString();
        String punishmentJson = data.get("punishment").getAsString();

        try {
            Punishment punishment = gson.fromJson(punishmentJson, Punishment.class);

            if ("apply".equals(action)) {
                plugin.getPunishmentManager().applyPunishment(
                        punishment.getPlayerUUID(),
                        punishment.getPlayerName(),
                        punishment.getType(),
                        punishment.getReason(),
                        punishment.getDuration(),
                        punishment.getExecutorUUID(),
                        punishment.getExecutorName()
                );
            } else if ("remove".equals(action)) {
                plugin.getPunishmentManager().removePunishment(
                        punishment.getId(),
                        punishment.getRemoverUUID(),
                        punishment.getRemoverName(),
                        punishment.getRemoveReason()
                );
            }
        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to handle punishment callback: " + e.getMessage());
        }
    }

    /**
     * Handle ban wave callback.
     */
    private void handleBanWaveCallback(JsonObject data) {
        String waveId = data.get("waveId").getAsString();
        String playerName = data.get("player").getAsString();
        String executor = data.get("executor").getAsString();

        plugin.getSXBansLogger().info("Ban wave [" + waveId + "] affected: " + playerName + " by " + executor);
    }

    /**
     * Handle sync callback.
     */
    private void handleSyncCallback(JsonObject data) {
        try {
            List<Punishment> punishments = new ArrayList<>();
            punishments = gson.fromJson(data.get("punishments"),
                    new com.google.gson.reflect.TypeToken<List<Punishment>>(){}.getType());

            for (Punishment p : punishments) {
                // Fix: استفاده از getPunishment و بررسی null به جای isPresent
                Punishment existing = plugin.getPunishmentStorage().getPunishment(p.getId());
                if (existing == null) {
                    plugin.getPunishmentStorage().savePunishment(p);
                    // Fix: استفاده از متد عمومی addPunishmentToCache
                    plugin.getPunishmentManager().addPunishmentToCache(p);
                }
            }

            lastSync.put("bungee_sync", System.currentTimeMillis());

        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to handle sync callback: " + e.getMessage());
        }
    }

    /**
     * Send a message to the Bungee proxy.
     */
    private void sendMessage(String subChannel, String data) {
        if (!enabled) return;

        try {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteArray);
            out.writeUTF(subChannel);
            out.writeUTF(data);

            // Send to all players (Bungee will handle the message)
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                player.sendPluginMessage(plugin, CHANNEL, byteArray.toByteArray());
            });

        } catch (IOException e) {
            plugin.getSXBansLogger().warning("Failed to send Bungee message: " + e.getMessage());
        }
    }

    /**
     * Send a punishment to Bungee.
     *
     * @param punishment The punishment
     */
    public void sendPunishment(Punishment punishment) {
        if (!enabled) return;

        JsonObject data = new JsonObject();
        data.addProperty("action", "apply");
        data.add("punishment", gson.toJsonTree(punishment));

        sendMessage("punishment", gson.toJson(data));
    }

    /**
     * Send a punishment removal to Bungee.
     *
     * @param punishment The punishment
     */
    public void sendPunishmentRemoval(Punishment punishment) {
        if (!enabled) return;

        JsonObject data = new JsonObject();
        data.addProperty("action", "remove");
        data.add("punishment", gson.toJsonTree(punishment));

        sendMessage("punishment", gson.toJson(data));
    }

    /**
     * Send a ban wave to Bungee.
     *
     * @param waveId The wave ID
     * @param playerName The player name
     * @param executor The executor name
     */
    public void sendBanWave(String waveId, String playerName, String executor) {
        if (!enabled) return;

        JsonObject data = new JsonObject();
        data.addProperty("waveId", waveId);
        data.addProperty("player", playerName);
        data.addProperty("executor", executor);
        data.addProperty("timestamp", System.currentTimeMillis());

        sendMessage("banwave", gson.toJson(data));
    }

    /**
     * Sync data with Bungee.
     */
    public void syncData() {
        if (!enabled) return;

        List<Punishment> punishments = plugin.getPunishmentStorage().getAllPunishments();

        JsonObject data = new JsonObject();
        data.addProperty("type", "sync");
        data.addProperty("server", plugin.getServer().getName());
        data.add("punishments", gson.toJsonTree(punishments));

        sendMessage("sync", gson.toJson(data));
    }

    /**
     * Get player info from Bungee.
     *
     * @param playerName The player name
     * @return CompletableFuture with player info
     */
    public CompletableFuture<ProxyPlayerInfo> getPlayerInfo(String playerName) {
        // Bungee doesn't support direct queries, return null
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Get all network players from Bungee.
     *
     * @return CompletableFuture with list of players
     */
    public CompletableFuture<List<ProxyPlayerInfo>> getNetworkPlayers() {
        // Bungee doesn't support direct queries, return empty list
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    /**
     * Kick a player from the Bungee network.
     *
     * @param playerName The player name
     * @param reason The reason
     */
    public void kickPlayer(String playerName, String reason) {
        if (!enabled) return;

        JsonObject data = new JsonObject();
        data.addProperty("action", "kick");
        data.addProperty("player", playerName);
        data.addProperty("reason", reason);

        sendMessage("kick", gson.toJson(data));
    }

    /**
     * Check if Bungee proxy is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get status information.
     *
     * @return Status map
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", enabled);
        status.put("last_sync", lastSync);
        return status;
    }

    /**
     * Shutdown Bungee proxy.
     */
    public void shutdown() {
        enabled = false;
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, SUB_CHANNEL);
    }
}
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

public class BungeeProxy {
    private final SXBans plugin;
    private final Gson gson;
    private final Map<String, Long> lastSync;
    private boolean enabled;

    private static final String CHANNEL = "sxbans:punish";
    private static final String SUB_CHANNEL = "sxbans:callback";

    public BungeeProxy(SXBans plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.lastSync = new ConcurrentHashMap<>();
        this.enabled = false;
    }

    public void initialize() {

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

    private void handlePunishmentCallback(JsonObject data) {
        String action = data.get("action").getAsString();
        String punishmentJson = data.get("punishment").getAsString();

        try {
            Punishment punishment = gson.fromJson(punishmentJson, Punishment.class);

            if (plugin.getConfigManager().getServerName().equals(punishment.getServerName())) {
                return;
            }

            if ("apply".equals(action)) {

                plugin.getPunishmentManager().applyPunishmentFromNetwork(
                        punishment.getPlayerUUID(),
                        punishment.getPlayerName(),
                        punishment.getType(),
                        punishment.getReason(),
                        punishment.getDuration(),
                        punishment.getExecutorUUID(),
                        punishment.getExecutorName(),
                        punishment.getIpAddress()
                );
            } else if ("remove".equals(action)) {
                plugin.getPunishmentManager().removePunishmentFromNetwork(
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

    private void handleBanWaveCallback(JsonObject data) {
        String waveId = data.get("waveId").getAsString();
        String playerName = data.get("player").getAsString();
        String executor = data.get("executor").getAsString();

        plugin.getSXBansLogger().info("Ban wave [" + waveId + "] affected: " + playerName + " by " + executor);
    }

    private void handleSyncCallback(JsonObject data) {
        try {
            List<Punishment> punishments = new ArrayList<>();
            punishments = gson.fromJson(data.get("punishments"),
                    new com.google.gson.reflect.TypeToken<List<Punishment>>(){}.getType());

            for (Punishment p : punishments) {

                Punishment existing = plugin.getPunishmentStorage().getPunishment(p.getId());
                if (existing == null) {
                    plugin.getPunishmentStorage().savePunishment(p);

                    plugin.getPunishmentManager().addPunishmentToCache(p);
                }
            }

            lastSync.put("bungee_sync", System.currentTimeMillis());

        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to handle sync callback: " + e.getMessage());
        }
    }

    private void sendMessage(String subChannel, String data) {
        if (!enabled) return;

        try {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteArray);
            out.writeUTF(subChannel);
            out.writeUTF(data);

            plugin.getServer().getOnlinePlayers().forEach(player -> {
                player.sendPluginMessage(plugin, CHANNEL, byteArray.toByteArray());
            });

        } catch (IOException e) {
            plugin.getSXBansLogger().warning("Failed to send Bungee message: " + e.getMessage());
        }
    }

    public void sendPunishment(Punishment punishment) {
        if (!enabled) return;

        JsonObject data = new JsonObject();
        data.addProperty("action", "apply");
        data.add("punishment", gson.toJsonTree(punishment));

        sendMessage("punishment", gson.toJson(data));
    }

    public void sendPunishmentRemoval(Punishment punishment) {
        if (!enabled) return;

        JsonObject data = new JsonObject();
        data.addProperty("action", "remove");
        data.add("punishment", gson.toJsonTree(punishment));

        sendMessage("punishment", gson.toJson(data));
    }

    public void sendBanWave(String waveId, String playerName, String executor) {
        if (!enabled) return;

        JsonObject data = new JsonObject();
        data.addProperty("waveId", waveId);
        data.addProperty("player", playerName);
        data.addProperty("executor", executor);
        data.addProperty("timestamp", System.currentTimeMillis());

        sendMessage("banwave", gson.toJson(data));
    }

    public void syncData() {
        if (!enabled) return;

        List<Punishment> punishments = plugin.getPunishmentStorage().getAllPunishments();

        JsonObject data = new JsonObject();
        data.addProperty("type", "sync");
        data.addProperty("server", plugin.getConfigManager().getServerName());
        data.add("punishments", gson.toJsonTree(punishments));

        sendMessage("sync", gson.toJson(data));
    }

    public CompletableFuture<ProxyPlayerInfo> getPlayerInfo(String playerName) {

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<List<ProxyPlayerInfo>> getNetworkPlayers() {

        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    public void kickPlayer(String playerName, String reason) {
        if (!enabled) return;

        JsonObject data = new JsonObject();
        data.addProperty("action", "kick");
        data.addProperty("player", playerName);
        data.addProperty("reason", reason);

        sendMessage("kick", gson.toJson(data));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", enabled);
        status.put("last_sync", lastSync);
        return status;
    }

    public void shutdown() {
        enabled = false;
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, SUB_CHANNEL);
    }
}
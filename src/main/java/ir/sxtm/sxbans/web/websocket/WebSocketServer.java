package ir.sxtm.sxbans.web.websocket;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketServer {
    private final SXBans plugin;
    private final Map<Session, String> sessions;
    private final Map<Session, Long> lastHeartbeat;

    public WebSocketServer(SXBans plugin) {
        this.plugin = plugin;
        this.sessions = new ConcurrentHashMap<>();
        this.lastHeartbeat = new ConcurrentHashMap<>();
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        String token = getSessionToken(session);
        if (token == null || !plugin.getWebServer().isValidSession(token)) {
            try {
                session.close(1008, "Unauthorized");
            } catch (Exception e) {
                plugin.getSXBansLogger().warning("Failed to close unauthorized session: " + e.getMessage());
            }
            return;
        }

        String username = plugin.getWebServer().getUsernameFromSession(token);
        sessions.put(session, username);
        lastHeartbeat.put(session, System.currentTimeMillis());

        plugin.getSXBansLogger().info("WebSocket client connected: " + username);

        sendMessage(session, "connected", "Connected to SX Bans WebSocket");
        sendMessage(session, "user", username);
        sendMessage(session, "server", plugin.getServer().getName());
        sendMessage(session, "version", plugin.getDescription().getVersion());

        updateOnlinePlayers();
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        String username = sessions.remove(session);
        lastHeartbeat.remove(session);
        if (username != null) {
            plugin.getSXBansLogger().info("WebSocket client disconnected: " + username);
            updateOnlinePlayers();
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        String username = sessions.get(session);
        if (username == null) return;

        try {
            com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(message, com.google.gson.JsonObject.class);
            String type = json.get("type").getAsString();

            switch (type) {
                case "heartbeat":
                    lastHeartbeat.put(session, System.currentTimeMillis());
                    sendMessage(session, "heartbeat", "ok");
                    break;

                case "subscribe":
                    String channel = json.get("channel").getAsString();
                    break;

                case "command":
                    String command = json.get("command").getAsString();
                    if (command != null && !command.isEmpty()) {
                        plugin.getServer().dispatchCommand(
                                plugin.getServer().getConsoleSender(),
                                command
                        );
                        broadcastMessage("command", command, username);
                    }
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            plugin.getSXBansLogger().warning("WebSocket error: " + e.getMessage());
        }
    }

    public void broadcastMessage(String type, Object data) {
        broadcastMessage(type, data, null);
    }

    public void broadcastMessage(String type, Object data, String sender) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("type", type);
        json.addProperty("timestamp", System.currentTimeMillis());
        if (sender != null) {
            json.addProperty("sender", sender);
        }
        json.add("data", new com.google.gson.Gson().toJsonTree(data));

        String payload = new com.google.gson.Gson().toJson(json);

        for (Session session : sessions.keySet()) {
            if (session.isOpen()) {
                try {
                    session.getRemote().sendString(payload);
                } catch (Exception e) {
                    plugin.getSXBansLogger().warning("Failed to broadcast message: " + e.getMessage());
                }
            }
        }
    }

    public void sendMessage(Session session, String type, Object data) {
        try {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("type", type);
            json.addProperty("timestamp", System.currentTimeMillis());
            json.add("data", new com.google.gson.Gson().toJsonTree(data));

            session.getRemote().sendString(new com.google.gson.Gson().toJson(json));
        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to send message: " + e.getMessage());
        }
    }

    public void updateOnlinePlayers() {
        String[] players = plugin.getServer().getOnlinePlayers().stream()
                .map(p -> p.getName())
                .toArray(String[]::new);
        broadcastMessage("players", players);
    }

    public void broadcastPunishment(Punishment punishment) {
        com.google.gson.JsonObject data = new com.google.gson.JsonObject();
        data.addProperty("id", punishment.getId().toString());
        data.addProperty("player", punishment.getPlayerName());
        data.addProperty("type", punishment.getType().getDisplayName());
        data.addProperty("reason", punishment.getReason());
        data.addProperty("executor", punishment.getExecutorName());
        data.addProperty("duration", punishment.getFormattedTimeRemaining());
        data.addProperty("timestamp", punishment.getCreatedAt());

        broadcastMessage("punishment", data);
    }

    public void broadcastPunishmentRemoved(Punishment punishment) {
        com.google.gson.JsonObject data = new com.google.gson.JsonObject();
        data.addProperty("id", punishment.getId().toString());
        data.addProperty("player", punishment.getPlayerName());
        data.addProperty("executor", punishment.getRemoverName());
        data.addProperty("reason", punishment.getRemoveReason());
        data.addProperty("timestamp", System.currentTimeMillis());

        broadcastMessage("punishment_removed", data);
    }

    public void broadcastWarning(UUID playerUUID, String playerName, int count, int max) {
        com.google.gson.JsonObject data = new com.google.gson.JsonObject();
        data.addProperty("player", playerName);
        data.addProperty("uuid", playerUUID.toString());
        data.addProperty("count", count);
        data.addProperty("max", max);

        broadcastMessage("warning", data);
    }

    public void broadcastBanWave(String name, int affected, int total) {
        com.google.gson.JsonObject data = new com.google.gson.JsonObject();
        data.addProperty("name", name);
        data.addProperty("affected", affected);
        data.addProperty("total", total);
        data.addProperty("timestamp", System.currentTimeMillis());

        broadcastMessage("banwave", data);
    }

    private String getSessionToken(Session session) {
        String query = session.getUpgradeRequest().getQueryString();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] parts = param.split("=");
                if (parts.length == 2 && parts[0].equals("token")) {
                    return parts[1];
                }
            }
        }

        List<java.net.HttpCookie> cookies = session.getUpgradeRequest().getCookies();
        if (cookies != null) {
            for (java.net.HttpCookie cookie : cookies) {
                if (cookie.getName().equals("session")) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    public void shutdown() {
        for (Session session : sessions.keySet()) {
            try {
                session.close(1000, "Server shutting down");
            } catch (Exception e) {
                plugin.getSXBansLogger().warning("Failed to close session: " + e.getMessage());
            }
        }
        sessions.clear();
        lastHeartbeat.clear();
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public Map<Session, String> getSessions() {
        return sessions;
    }
}
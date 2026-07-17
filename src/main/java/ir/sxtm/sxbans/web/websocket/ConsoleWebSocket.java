package ir.sxtm.sxbans.web.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ir.sxtm.sxbans.SXBans;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class ConsoleWebSocket {
    private final SXBans plugin;
    private final Gson gson;
    private final Map<Session, String> sessions;
    private boolean shutdown;

    public ConsoleWebSocket(SXBans plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.sessions = new ConcurrentHashMap<>();
        this.shutdown = false;
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
        plugin.getSXBansLogger().info("WebSocket connected: " + username);

        sendMessage(session, "Connected to SX Bans console");
        sendMessage(session, "Welcome, " + username + "!");
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        String username = sessions.remove(session);
        if (username != null) {
            plugin.getSXBansLogger().info("WebSocket disconnected: " + username);
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        String username = sessions.get(session);
        if (username == null) return;

        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String command = json.get("command").getAsString();

            if (command != null && !command.isEmpty()) {
                plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(),
                        command
                );

                plugin.getSXBansLogger().info("[WebSocket:" + username + "] /" + command);

                JsonObject response = new JsonObject();
                response.addProperty("type", "command");
                response.addProperty("command", command);
                response.addProperty("executor", username);
                session.getRemote().sendString(gson.toJson(response));
            }
        } catch (Exception e) {
            try {
                JsonObject error = new JsonObject();
                error.addProperty("type", "error");
                error.addProperty("message", "Failed to execute command: " + e.getMessage());
                session.getRemote().sendString(gson.toJson(error));
            } catch (Exception ex) {
                plugin.getSXBansLogger().warning("Failed to send error message: " + ex.getMessage());
            }
        }
    }

    public void sendMessage(Session session, String message) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("type", "log");
            json.addProperty("message", message);
            json.addProperty("timestamp", System.currentTimeMillis());
            session.getRemote().sendString(gson.toJson(json));
        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to send message: " + e.getMessage());
        }
    }

    public void broadcastMessage(String message) {
        if (shutdown) return;

        JsonObject json = new JsonObject();
        json.addProperty("type", "log");
        json.addProperty("message", message);
        json.addProperty("timestamp", System.currentTimeMillis());
        String payload = gson.toJson(json);

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

    public void broadcastCommand(String command, String executor) {
        if (shutdown) return;

        JsonObject json = new JsonObject();
        json.addProperty("type", "command");
        json.addProperty("command", command);
        json.addProperty("executor", executor);
        json.addProperty("timestamp", System.currentTimeMillis());
        String payload = gson.toJson(json);

        for (Session session : sessions.keySet()) {
            if (session.isOpen()) {
                try {
                    session.getRemote().sendString(payload);
                } catch (Exception e) {
                    plugin.getSXBansLogger().warning("Failed to broadcast command: " + e.getMessage());
                }
            }
        }
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
        shutdown = true;
        for (Session session : sessions.keySet()) {
            try {
                session.close(1000, "Server shutting down");
            } catch (Exception e) {
                plugin.getSXBansLogger().warning("Failed to close session: " + e.getMessage());
            }
        }
        sessions.clear();
    }

    public int getSessionCount() {
        return sessions.size();
    }
}
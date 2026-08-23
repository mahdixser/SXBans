package ir.sxtm.sxbans.web.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.web.models.WebResponse;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ConsoleController extends HttpServlet {
    private final SXBans plugin;
    private final Gson gson = new Gson();
    private final List<String> consoleLogs;
    private final int MAX_LOGS = 1000;

    public ConsoleController(SXBans plugin) {
        this.plugin = plugin;
        this.consoleLogs = new ArrayList<>();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json");

        try {
            if (!hasConsolePermission(req)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "You do not have permission to use the console")));
                return;
            }

            if (path == null || path.equals("/")) {
                handleGetLogs(req, resp);
            } else if (path.equals("/commands")) {
                handleGetCommands(req, resp);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Endpoint not found")));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Error: " + e.getMessage())));
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json");

        try {
            if (!hasConsolePermission(req)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "You do not have permission to use the console")));
                return;
            }

            if (path == null || path.equals("/execute")) {
                handleExecuteCommand(req, resp);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Endpoint not found")));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Error: " + e.getMessage())));
        }
    }

    private void handleGetLogs(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int limit = getIntParam(req, "limit", 100);
        int offset = getIntParam(req, "offset", 0);

        List<String> logs = consoleLogs;
        int total = logs.size();
        int start = Math.max(0, total - limit - offset);
        int end = Math.max(0, total - offset);

        List<String> pagedLogs = logs.subList(start, end);

        JsonObject response = new JsonObject();
        response.add("logs", gson.toJsonTree(pagedLogs));
        response.addProperty("total", total);
        response.addProperty("limit", limit);
        response.addProperty("offset", offset);

        resp.getWriter().write(gson.toJson(response));
    }

    private void handleGetCommands(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonArray commands = new JsonArray();

        try {
            Server server = Bukkit.getServer();
            Method getCommandMapMethod = server.getClass().getMethod("getCommandMap");
            Object commandMap = getCommandMapMethod.invoke(server);

            if (commandMap instanceof SimpleCommandMap) {
                SimpleCommandMap simpleCommandMap = (SimpleCommandMap) commandMap;

                Method getKnownCommandsMethod = SimpleCommandMap.class.getDeclaredMethod("getKnownCommands");
                getKnownCommandsMethod.setAccessible(true);

                @SuppressWarnings("unchecked")
                Map<String, Command> knownCommands = (Map<String, Command>) getKnownCommandsMethod.invoke(simpleCommandMap);

                for (String command : knownCommands.keySet()) {
                    commands.add(command);
                }
            }
        } catch (Exception e) {
            plugin.getSXBansLogger().warning("Failed to get command map: " + e.getMessage());
        }

        String[] commonCommands = {"ban", "tempban", "unban", "kick", "mute", "tempmute", "unmute",
                "warn", "history", "check", "ipban", "ipunban", "ipmute", "ipunmute", "sxbans",
                "gamemode", "tp", "spawn", "setspawn", "kit", "give", "clear", "whitelist", "op", "deop",
                "list", "help", "say", "broadcast", "tell", "msg", "tellraw", "title", "bossbar",
                "scoreboard", "team", "effect", "particle", "playsound", "stop", "restart", "reload"};

        for (String cmd : commonCommands) {
            boolean exists = false;
            for (int i = 0; i < commands.size(); i++) {
                if (commands.get(i).getAsString().equals(cmd)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                commands.add(cmd);
            }
        }

        resp.getWriter().write(commands.toString());
    }

    private void handleExecuteCommand(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);
        String command = body.get("command").getAsString();
        String username = body.has("username") ? body.get("username").getAsString() : "WebConsole";

        if (command == null || command.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Command required")));
            return;
        }

        CommandSender sender = new WebCommandSender(username);

        boolean success = plugin.getServer().dispatchCommand(sender, command);

        JsonObject response = new JsonObject();
        response.addProperty("success", success);
        response.addProperty("command", command);
        response.addProperty("executor", username);
        response.addProperty("timestamp", System.currentTimeMillis());

        if (sender instanceof WebCommandSender) {
            WebCommandSender webSender = (WebCommandSender) sender;
            response.add("output", gson.toJsonTree(webSender.getMessages()));
        }

        String logEntry = "[" + username + "] /" + command;
        consoleLogs.add(logEntry);
        if (consoleLogs.size() > MAX_LOGS) {
            consoleLogs.remove(0);
        }

        resp.getWriter().write(gson.toJson(response));
    }

    public void addLog(String log) {
        consoleLogs.add(log);
        if (consoleLogs.size() > MAX_LOGS) {
            consoleLogs.remove(0);
        }
    }

    public List<String> getConsoleLogs() {
        return consoleLogs;
    }

    private boolean isAuthenticated(HttpServletRequest req) {
        String token = getAuthToken(req);
        return token != null && plugin.getWebServer().isValidSession(token);
    }

    private boolean hasConsolePermission(HttpServletRequest req) {
        String token = getAuthToken(req);
        if (token == null || !plugin.getWebServer().isValidSession(token)) return false;

        String username = plugin.getWebServer().getUsernameFromSession(token);
        if (username == null) return false;

        ir.sxtm.sxbans.models.WebUser user = plugin.getWebUsersManager().getUser(username);
        return user != null && user.isEnabled() &&
                (user.hasPermission("*") || user.hasPermission("sxbans.web.admin") || user.hasPermission("sxbans.web.console"));
    }

    private String getAuthToken(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }

        String cookieHeader = req.getHeader("Cookie");
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && parts[0].equals("session")) {
                    return parts[1];
                }
            }
        }

        return null;
    }

    private JsonObject parseBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = req.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return gson.fromJson(sb.toString(), JsonObject.class);
    }

    private int getIntParam(HttpServletRequest req, String param, int defaultValue) {
        String value = req.getParameter(param);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static class WebCommandSender implements CommandSender {
        private final String name;
        private final List<String> messages;
        private final Set<PermissionAttachmentInfo> permissions;

        public WebCommandSender(String name) {
            this.name = name;
            this.messages = new ArrayList<>();
            this.permissions = new HashSet<>();
        }

        public void sendMessage(String message) {
            messages.add(message);
        }

        public void sendMessage(String[] messages) {
            for (String msg : messages) {
                this.messages.add(msg);
            }
        }

        public void sendMessage(UUID uuid, String message) {
            messages.add(message);
        }

        public void sendMessage(UUID uuid, String[] messages) {
            for (String msg : messages) {
                this.messages.add(msg);
            }
        }

        public String getName() {
            return name;
        }

        public Server getServer() {
            return Bukkit.getServer();
        }

        public boolean isPermissionSet(String permission) {
            return true;
        }

        public boolean isPermissionSet(Permission permission) {
            return true;
        }

        public boolean hasPermission(String permission) {
            return true;
        }

        public boolean hasPermission(Permission permission) {
            return true;
        }

        public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b) {
            return null;
        }

        public PermissionAttachment addAttachment(Plugin plugin) {
            return null;
        }

        public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b, int i) {
            return null;
        }

        public PermissionAttachment addAttachment(Plugin plugin, int i) {
            return null;
        }

        public void removeAttachment(PermissionAttachment permissionAttachment) {

        }

        public void recalculatePermissions() {

        }

        public Set<PermissionAttachmentInfo> getEffectivePermissions() {
            return permissions;
        }

        public boolean isOp() {
            return true;
        }

        public void setOp(boolean value) {

        }

        public boolean isPlayer() {
            return false;
        }

        public boolean isConsole() {
            return true;
        }

        public org.bukkit.command.CommandSender.Spigot spigot() {
            return new org.bukkit.command.CommandSender.Spigot() {
                public void sendMessage(net.md_5.bungee.api.chat.BaseComponent component) {
                    messages.add(component.toPlainText());
                }

                public void sendMessage(net.md_5.bungee.api.chat.BaseComponent... components) {
                    for (net.md_5.bungee.api.chat.BaseComponent comp : components) {
                        messages.add(comp.toPlainText());
                    }
                }
            };
        }

        public List<String> getMessages() {
            return messages;
        }
    }
}
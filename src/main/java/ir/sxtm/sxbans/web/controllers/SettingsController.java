package ir.sxtm.sxbans.web.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.web.models.WebResponse;
import ir.sxtm.sxbans.models.WebUser;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class SettingsController extends HttpServlet {
    private final SXBans plugin;
    private final Gson gson = new Gson();

    public SettingsController(SXBans plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json");

        try {
            if (!isAuthenticated(req)) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Unauthorized")));
                return;
            }

            if (path == null || path.equals("/")) {
                handleGetSettings(req, resp);
            } else if (path.equals("/users")) {
                handleGetUsers(req, resp);
            } else if (path.startsWith("/users/")) {
                String username = path.substring(7);
                handleGetUser(req, resp, username);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Endpoint not found")));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Error: " + e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json");

        try {
            if (!isAuthenticated(req)) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Unauthorized")));
                return;
            }

            if (path == null || path.equals("/")) {
                handleUpdateSettings(req, resp);
            } else if (path.equals("/users")) {
                handleCreateUser(req, resp);
            } else if (path.startsWith("/users/")) {
                String username = path.substring(7);
                handleUpdateUser(req, resp, username);
            } else if (path.equals("/reload")) {
                handleReload(req, resp);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Endpoint not found")));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Error: " + e.getMessage())));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json");

        try {
            if (!isAuthenticated(req)) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Unauthorized")));
                return;
            }

            if (path != null && path.startsWith("/users/")) {
                String username = path.substring(7);
                handleDeleteUser(req, resp, username);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Endpoint not found")));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Error: " + e.getMessage())));
        }
    }

    private void handleGetSettings(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject settings = new JsonObject();

        settings.addProperty("databaseType", plugin.getConfigManager().getDatabaseType());
        settings.addProperty("databaseHost", plugin.getConfigManager().getString("database.host", "localhost"));
        settings.addProperty("databasePort", plugin.getConfigManager().getInt("database.port", 3306));
        settings.addProperty("databaseName", plugin.getConfigManager().getString("database.name", "sxbans"));

        settings.addProperty("webEnabled", plugin.getConfigManager().isWebEnabled());
        settings.addProperty("webPort", plugin.getConfigManager().getWebPort());
        settings.addProperty("webHost", plugin.getConfigManager().getWebHost());

        settings.addProperty("maxWarnings", plugin.getConfigManager().getMaxWarnings());
        settings.addProperty("warningExpiry", plugin.getConfigManager().getWarningExpiry());
        settings.addProperty("autoBanEnabled", plugin.getConfigManager().isAutoBanEnabled());
        settings.addProperty("autoBanDuration", plugin.getConfigManager().getAutoBanDuration());

        settings.addProperty("consoleLogging", plugin.getConfigManager().isConsoleLogging());
        settings.addProperty("fileLogging", plugin.getConfigManager().isFileLogging());
        settings.addProperty("webLogging", plugin.getConfigManager().isWebLogging());

        settings.addProperty("redisEnabled", plugin.getConfigManager().isRedisEnabled());
        settings.addProperty("redisHost", plugin.getConfigManager().getString("redis.host", "localhost"));
        settings.addProperty("redisPort", plugin.getConfigManager().getInt("redis.port", 6379));

        resp.getWriter().write(gson.toJson(settings));
    }

    private void handleUpdateSettings(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);

        if (body.has("maxWarnings")) {
            plugin.getConfigManager().getConfig().set("punishments.max-warnings", body.get("maxWarnings").getAsInt());
        }
        if (body.has("warningExpiry")) {
            plugin.getConfigManager().getConfig().set("punishments.warning-expiry", body.get("warningExpiry").getAsLong());
        }
        if (body.has("autoBanEnabled")) {
            plugin.getConfigManager().getConfig().set("punishments.auto-ban.enabled", body.get("autoBanEnabled").getAsBoolean());
        }
        if (body.has("autoBanDuration")) {
            plugin.getConfigManager().getConfig().set("punishments.auto-ban.ban-duration", body.get("autoBanDuration").getAsLong());
        }
        if (body.has("webEnabled")) {
            plugin.getConfigManager().getConfig().set("web.enabled", body.get("webEnabled").getAsBoolean());
        }
        if (body.has("webPort")) {
            plugin.getConfigManager().getConfig().set("web.port", body.get("webPort").getAsInt());
        }
        if (body.has("consoleLogging")) {
            plugin.getConfigManager().getConfig().set("logging.console", body.get("consoleLogging").getAsBoolean());
        }
        if (body.has("fileLogging")) {
            plugin.getConfigManager().getConfig().set("logging.file", body.get("fileLogging").getAsBoolean());
        }
        if (body.has("webLogging")) {
            plugin.getConfigManager().getConfig().set("logging.web", body.get("webLogging").getAsBoolean());
        }

        plugin.getConfigManager().saveConfig();
        plugin.getConfigManager().reloadConfig();

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "Settings updated successfully");
        resp.getWriter().write(gson.toJson(response));
    }

    private void handleGetUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<WebUser> users = plugin.getWebUsersManager().getAllUsers();

        List<JsonObject> safeUsers = new ArrayList<>();
        for (WebUser user : users) {
            JsonObject safeUser = new JsonObject();
            safeUser.addProperty("username", user.getUsername());
            safeUser.addProperty("level", user.getLevel());
            safeUser.addProperty("enabled", user.isEnabled());
            safeUser.addProperty("lastLogin", user.getLastLogin());
            safeUser.addProperty("createdAt", user.getCreatedAt());
            safeUser.add("permissions", gson.toJsonTree(user.getPermissions()));
            safeUsers.add(safeUser);
        }

        resp.getWriter().write(gson.toJson(safeUsers));
    }

    private void handleGetUser(HttpServletRequest req, HttpServletResponse resp, String username) throws IOException {
        WebUser user = plugin.getWebUsersManager().getUser(username);

        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "User not found")));
            return;
        }

        JsonObject safeUser = new JsonObject();
        safeUser.addProperty("username", user.getUsername());
        safeUser.addProperty("level", user.getLevel());
        safeUser.addProperty("enabled", user.isEnabled());
        safeUser.addProperty("lastLogin", user.getLastLogin());
        safeUser.addProperty("createdAt", user.getCreatedAt());
        safeUser.add("permissions", gson.toJsonTree(user.getPermissions()));

        resp.getWriter().write(gson.toJson(safeUser));
    }

    private void handleCreateUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();
        int level = body.has("level") ? body.get("level").getAsInt() : 1;

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Username and password required")));
            return;
        }

        boolean success = plugin.getWebUsersManager().createUser(username, password, level);

        if (success) {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "User created successfully");
            resp.getWriter().write(gson.toJson(response));
        } else {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "User already exists")));
        }
    }

    private void handleUpdateUser(HttpServletRequest req, HttpServletResponse resp, String username) throws IOException {
        JsonObject body = parseBody(req);

        WebUser user = plugin.getWebUsersManager().getUser(username);
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "User not found")));
            return;
        }

        if (body.has("level")) {
            plugin.getWebUsersManager().updateUserLevel(username, body.get("level").getAsInt());
        }

        if (body.has("password")) {
            String newPassword = body.get("password").getAsString();
            if (newPassword != null && !newPassword.isEmpty()) {
                plugin.getWebUsersManager().updateUserPassword(username, newPassword);
            }
        }

        if (body.has("enabled")) {
            if (body.get("enabled").getAsBoolean() != user.isEnabled()) {
                plugin.getWebUsersManager().toggleUserEnabled(username);
            }
        }

        if (body.has("permissions")) {
            var permissions = body.get("permissions").getAsJsonArray();
            for (String perm : user.getPermissions()) {
                plugin.getWebUsersManager().removePermission(username, perm);
            }
            for (var perm : permissions) {
                plugin.getWebUsersManager().addPermission(username, perm.getAsString());
            }
        }

        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "User updated successfully");
        resp.getWriter().write(gson.toJson(response));
    }

    private void handleDeleteUser(HttpServletRequest req, HttpServletResponse resp, String username) throws IOException {
        if ("admin".equals(username)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Cannot delete admin user")));
            return;
        }

        boolean success = plugin.getWebUsersManager().deleteUser(username);

        if (success) {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "User deleted successfully");
            resp.getWriter().write(gson.toJson(response));
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "User not found")));
        }
    }

    private void handleReload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getMessagesManager().reloadMessages();
            plugin.getWebUsersManager().loadUsers();
            plugin.getTemplateManager().reloadTemplates();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "All configurations reloaded successfully");
            resp.getWriter().write(gson.toJson(response));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Error reloading: " + e.getMessage())));
        }
    }

    private boolean isAuthenticated(HttpServletRequest req) {
        String token = getAuthToken(req);
        if (token == null) return false;

        if (!plugin.getWebServer().isValidSession(token)) return false;

        String username = plugin.getWebServer().getUsernameFromSession(token);
        if (username == null) return false;

        WebUser user = plugin.getWebUsersManager().getUser(username);
        return user != null && (user.hasPermission("*") || user.hasPermission("sxbans.web.admin"));
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
}
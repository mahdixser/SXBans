package ir.sxtm.sxbans.web.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import ir.sxtm.sxbans.models.Punishment.PunishmentStatus;
import ir.sxtm.sxbans.web.models.WebResponse;
import ir.sxtm.sxbans.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PunishmentController extends HttpServlet {
    private final SXBans plugin;
    private final Gson gson = new Gson();

    public PunishmentController(SXBans plugin) {
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
                handleListPunishments(req, resp);
            } else if (path.startsWith("/")) {
                String id = path.substring(1);
                handleGetPunishment(req, resp, id);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Endpoint not found")));
            }
        } catch (Exception e) {
            plugin.getSXBansLogger().severe("PunishmentController error: " + e.getMessage());
            e.printStackTrace();
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

            if (!hasPunishPermission(req)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "You do not have permission to manage punishments")));
                return;
            }

            if (path == null || path.equals("/")) {
                handleCreatePunishment(req, resp);
            } else if (path.equals("/remove")) {
                handleRemovePunishment(req, resp);
            } else if (path.equals("/expire")) {
                handleExpirePunishment(req, resp);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Endpoint not found")));
            }
        } catch (Exception e) {
            plugin.getSXBansLogger().severe("PunishmentController error: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Error: " + e.getMessage())));
        }
    }

    private String getStringSafe(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return null;
        try {
            if (obj.get(key).isJsonNull()) return null;
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private int getIntSafe(JsonObject obj, String key, int defaultValue) {
        if (obj == null || !obj.has(key)) return defaultValue;
        try {
            if (obj.get(key).isJsonNull()) return defaultValue;
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean getBooleanSafe(JsonObject obj, String key, boolean defaultValue) {
        if (obj == null || !obj.has(key)) return defaultValue;
        try {
            if (obj.get(key).isJsonNull()) return defaultValue;
            return obj.get(key).getAsBoolean();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void handleCreatePunishment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);

        String playerName = getStringSafe(body, "player");
        String typeStr = getStringSafe(body, "type");
        String reason = getStringSafe(body, "reason");
        String durationStr = getStringSafe(body, "duration");
        String server = getStringSafe(body, "server");
        String executorName = getStringSafe(body, "executor");
        String ip = getStringSafe(body, "ip");
        int warnCount = getIntSafe(body, "warnCount", 1);

        if (playerName == null || playerName.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Player name is required")));
            return;
        }

        if (reason == null || reason.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Reason is required")));
            return;
        }

        PunishmentType type;
        try {
            type = PunishmentType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Invalid punishment type: " + typeStr)));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Player not found: " + playerName)));
            return;
        }

        UUID targetUUID = target.getUniqueId();
        String realName = target.getName() != null ? target.getName() : playerName;

        if (type == PunishmentType.BAN || type == PunishmentType.TEMP_BAN) {
            if (plugin.getPunishmentManager().isPlayerBanned(targetUUID)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Player is already banned")));
                return;
            }
        }

        if (type == PunishmentType.MUTE || type == PunishmentType.TEMP_MUTE) {
            if (plugin.getPunishmentManager().isPlayerMuted(targetUUID)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Player is already muted")));
                return;
            }
        }

        long duration = -1;
        if (durationStr != null && !durationStr.isEmpty() &&
                !durationStr.equals("-1") && !durationStr.equalsIgnoreCase("permanent")) {
            duration = TimeUtils.parseTime(durationStr);
            if (duration <= 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Invalid duration format. Use: 1s, 1m, 1h, 1d, 1w, 1M, 1y")));
                return;
            }
        }

        String finalPlayerName = realName;
        UUID finalTargetUUID = targetUUID;

        if (type == PunishmentType.IP_BAN || type == PunishmentType.IP_MUTE || type == PunishmentType.IP_KICK) {
            if (ip == null || ip.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "IP address is required for IP punishments")));
                return;
            }
            finalPlayerName = "IP: " + ip;
            finalTargetUUID = UUID.randomUUID();
        }

        try {
            UUID executorUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

            String execName = getAuthenticatedUsername(req);

            Punishment punishment = plugin.getPunishmentManager().applyPunishment(
                    finalTargetUUID,
                    finalPlayerName,
                    type,
                    reason,
                    duration,
                    executorUUID,
                    execName,
                    (ip != null && !ip.isEmpty()) ? ip : null
            );

            if (punishment != null) {

                if (server != null && !server.isEmpty()) {
                    punishment.setServerName(server);
                    plugin.getPunishmentStorage().updatePunishment(punishment);
                }

                if (type == PunishmentType.WARN && warnCount > 1) {
                    for (int i = 1; i < warnCount; i++) {
                        plugin.getPunishmentManager().applyPunishment(
                                finalTargetUUID,
                                realName,
                                PunishmentType.WARN,
                                reason + " (x" + warnCount + ")",
                                -1,
                                executorUUID,
                                execName
                        );
                    }
                }

                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Punishment applied successfully");
                response.add("punishment", gson.toJsonTree(punishment));
                resp.getWriter().write(gson.toJson(response));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Failed to apply punishment - player may already be punished")));
            }

        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Error creating punishment: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Error: " + e.getMessage())));
        }
    }

    private void handleListPunishments(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 50);
        String type = req.getParameter("type");
        String status = req.getParameter("status");

        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();

        if (type != null && !type.isEmpty()) {
            try {
                PunishmentType filterType = PunishmentType.valueOf(type.toUpperCase());
                allPunishments = allPunishments.stream()
                        .filter(p -> p.getType() == filterType)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        if (status != null && !status.isEmpty()) {
            try {
                PunishmentStatus filterStatus = PunishmentStatus.valueOf(status.toUpperCase());
                allPunishments = allPunishments.stream()
                        .filter(p -> p.getStatus() == filterStatus)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        allPunishments.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        int total = allPunishments.size();
        int start = (page - 1) * limit;
        int end = Math.min(start + limit, total);
        List<Punishment> paged = start < total ? allPunishments.subList(start, end) : new ArrayList<>();

        List<JsonObject> responseList = new ArrayList<>();
        for (Punishment p : paged) {
            JsonObject obj = gson.toJsonTree(p).getAsJsonObject();

            obj.addProperty("executorHead", getPlayerHeadUrl(
                    p.getExecutorUUID() != null ? p.getExecutorUUID().toString() : "00000000-0000-0000-0000-000000000000", 32));
            obj.addProperty("playerHead", getPlayerHeadUrl(
                    p.getPlayerUUID() != null ? p.getPlayerUUID().toString() : "00000000-0000-0000-0000-000000000000", 32));

            obj.addProperty("duration", p.isPermanent() ? "Permanent" : TimeUtils.formatTime(p.getDuration()));
            obj.addProperty("remaining", p.isPermanent() ? "N/A" : p.getFormattedTimeRemaining());
            obj.addProperty("server", p.getServerName() != null ? p.getServerName() : "Global");
            obj.addProperty("type", p.getType().name());
            obj.addProperty("status", p.getStatus().name());
            obj.addProperty("createdAt", p.getCreatedAt());
            obj.addProperty("executorName", p.getExecutorName());
            obj.addProperty("playerName", p.getPlayerName());
            obj.addProperty("reason", p.getReason());
            obj.addProperty("executorUuid", p.getExecutorUUID().toString());
            obj.addProperty("playerUuid", p.getPlayerUUID().toString());
            obj.addProperty("isPermanent", p.isPermanent());
            responseList.add(obj);
        }

        JsonObject response = new JsonObject();
        response.add("punishments", gson.toJsonTree(responseList));
        response.addProperty("total", total);
        response.addProperty("page", page);
        response.addProperty("totalPages", (int) Math.ceil(total / (double) limit));
        response.addProperty("success", true);

        resp.getWriter().write(gson.toJson(response));
    }

    private void handleGetPunishment(HttpServletRequest req, HttpServletResponse resp, String id) throws IOException {
        try {
            UUID punishmentId = UUID.fromString(id);
            Punishment punishment = plugin.getPunishmentStorage().getPunishment(punishmentId);

            if (punishment == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Punishment not found")));
                return;
            }

            JsonObject response = new JsonObject();
            response.add("punishment", gson.toJsonTree(punishment));
            response.addProperty("timeRemaining", punishment.getFormattedTimeRemaining());
            response.addProperty("isActive", punishment.isActive());
            response.addProperty("isPermanent", punishment.isPermanent());
            response.addProperty("isExpired", punishment.isExpired());
            response.addProperty("success", true);

            resp.getWriter().write(gson.toJson(response));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Invalid punishment ID")));
        }
    }

    private void handleRemovePunishment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);
        String idStr = getStringSafe(body, "id");

        if (idStr == null || idStr.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Punishment ID is required")));
            return;
        }

        try {
            UUID punishmentId = UUID.fromString(idStr);
            Punishment punishment = plugin.getPunishmentStorage().getPunishment(punishmentId);

            if (punishment == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Punishment not found")));
                return;
            }

            UUID removerUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
            String removerName = getAuthenticatedUsername(req);

            boolean success = plugin.getPunishmentManager().removePunishment(
                    punishmentId,
                    removerUUID,
                    removerName,
                    "Removed via web panel"
            );

            if (success) {
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Punishment removed successfully");
                resp.getWriter().write(gson.toJson(response));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Failed to remove punishment")));
            }

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Invalid punishment ID")));
        }
    }

    private void handleExpirePunishment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);
        String idStr = getStringSafe(body, "id");

        if (idStr == null || idStr.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Punishment ID is required")));
            return;
        }

        try {
            UUID punishmentId = UUID.fromString(idStr);
            Punishment punishment = plugin.getPunishmentStorage().getPunishment(punishmentId);

            if (punishment == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Punishment not found")));
                return;
            }

            if (!punishment.isActive()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Punishment is not active")));
                return;
            }

            if (punishment.isPermanent()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Cannot expire permanent punishment")));
                return;
            }

            punishment.setEndTime(System.currentTimeMillis());
            punishment.setStatus(PunishmentStatus.EXPIRED);
            punishment.setUpdatedAt(System.currentTimeMillis());

            plugin.getPunishmentStorage().updatePunishment(punishment);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Punishment expired successfully");
            resp.getWriter().write(gson.toJson(response));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Invalid punishment ID")));
        }
    }

    private boolean isAuthenticated(HttpServletRequest req) {
        String token = getAuthToken(req);
        return token != null && plugin.getWebServer().isValidSession(token);
    }

    private boolean hasPunishPermission(HttpServletRequest req) {
        String token = getAuthToken(req);
        if (token == null || !plugin.getWebServer().isValidSession(token)) return false;

        String username = plugin.getWebServer().getUsernameFromSession(token);
        if (username == null) return false;

        ir.sxtm.sxbans.models.WebUser user = plugin.getWebUsersManager().getUser(username);
        return user != null && user.isEnabled() &&
                (user.hasPermission("*") || user.hasPermission("sxbans.web.admin") || user.hasPermission("sxbans.web.punish"));
    }

    private String getAuthenticatedUsername(HttpServletRequest req) {
        String token = getAuthToken(req);
        if (token == null) return "WebPanel";
        String username = plugin.getWebServer().getUsernameFromSession(token);
        return username != null ? username : "WebPanel";
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
        String json = sb.toString();
        if (json == null || json.isEmpty()) {
            return new JsonObject();
        }
        try {
            return gson.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            return new JsonObject();
        }
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

    private String getPlayerHeadUrl(String uuid, int size) {
        String baseUrl = "https://minotar.net/avatar/";
        String fallbackUuid = "00000000-0000-0000-0000-000000000000";
        String id = (uuid != null && !uuid.isEmpty()) ? uuid : fallbackUuid;
        return baseUrl + id + "/" + size;
    }
}
package ir.sxtm.sxbans.web.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.web.models.WebResponse;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController extends HttpServlet {
    private final SXBans plugin;
    private final Gson gson = new Gson();

    public DashboardController(SXBans plugin) {
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

            plugin.getSXBansLogger().info("DashboardController doGet - path: " + path);

            if (path == null || path.equals("/")) {
                handleDashboard(req, resp);
            } else if (path.equals("/stats")) {
                handleStats(req, resp);
            } else if (path.equals("/charts")) {
                handleCharts(req, resp);
            } else if (path.equals("/recent")) {
                handleRecent(req, resp);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write(gson.toJson(new WebResponse(false, "Endpoint not found")));
            }
        } catch (Exception e) {
            plugin.getSXBansLogger().severe("DashboardController error: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Error: " + e.getMessage())));
        }
    }

    private void handleDashboard(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();

        JsonObject response = new JsonObject();
        response.addProperty("totalPunishments", allPunishments.size());
        response.addProperty("success", true);

        long activePunishments = allPunishments.stream()
                .filter(Punishment::isActive)
                .count();
        response.addProperty("activePunishments", activePunishments);

        long activeBans = allPunishments.stream()
                .filter(p -> p.isActive() && (p.getType() == Punishment.PunishmentType.BAN ||
                        p.getType() == Punishment.PunishmentType.TEMP_BAN))
                .count();
        response.addProperty("activeBans", activeBans);

        long activeMutes = allPunishments.stream()
                .filter(p -> p.isActive() && (p.getType() == Punishment.PunishmentType.MUTE ||
                        p.getType() == Punishment.PunishmentType.TEMP_MUTE))
                .count();
        response.addProperty("activeMutes", activeMutes);

        long totalWarnings = allPunishments.stream()
                .filter(p -> p.getType() == Punishment.PunishmentType.WARN)
                .count();
        response.addProperty("totalWarnings", totalWarnings);

        long today = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        long todayPunishments = allPunishments.stream()
                .filter(p -> p.getCreatedAt() > today)
                .count();
        response.addProperty("todayPunishments", todayPunishments);

        Set<UUID> uniquePlayers = allPunishments.stream()
                .map(Punishment::getPlayerUUID)
                .collect(Collectors.toSet());
        response.addProperty("uniquePlayers", uniquePlayers.size());

        plugin.getSXBansLogger().info("Dashboard response: totalPunishments=" + allPunishments.size());
        resp.getWriter().write(gson.toJson(response));
    }

    private void handleStats(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();

        JsonObject response = new JsonObject();
        response.addProperty("success", true);

        Map<String, Long> byType = allPunishments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getType().name(),
                        Collectors.counting()
                ));
        response.add("byType", gson.toJsonTree(byType));

        Map<String, Long> byDay = new LinkedHashMap<>();
        long now = System.currentTimeMillis();
        for (int i = 29; i >= 0; i--) {
            String day = new java.text.SimpleDateFormat("yyyy-MM-dd")
                    .format(new Date(now - (i * 24 * 60 * 60 * 1000L)));
            long count = allPunishments.stream()
                    .filter(p -> day.equals(new java.text.SimpleDateFormat("yyyy-MM-dd")
                            .format(new Date(p.getCreatedAt()))))
                    .count();
            byDay.put(day, count);
        }
        response.add("byDay", gson.toJsonTree(byDay));

        Map<String, Long> topPlayers = allPunishments.stream()
                .collect(Collectors.groupingBy(
                        Punishment::getPlayerName,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        response.add("topPlayers", gson.toJsonTree(topPlayers));

        resp.getWriter().write(gson.toJson(response));
    }

    private void handleCharts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();

        JsonObject response = new JsonObject();
        response.addProperty("success", true);

        Map<String, Long> pieData = allPunishments.stream()
                .filter(Punishment::isActive)
                .collect(Collectors.groupingBy(
                        p -> p.getType().getDisplayName(),
                        Collectors.counting()
                ));
        response.add("pieData", gson.toJsonTree(pieData));

        Map<String, Long> lineData = new LinkedHashMap<>();
        long now = System.currentTimeMillis();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd");
        for (int i = 6; i >= 0; i--) {
            String day = sdf.format(new Date(now - (i * 24 * 60 * 60 * 1000L)));
            long count = allPunishments.stream()
                    .filter(p -> day.equals(sdf.format(new Date(p.getCreatedAt()))))
                    .count();
            lineData.put(day, count);
        }
        response.add("lineData", gson.toJsonTree(lineData));

        resp.getWriter().write(gson.toJson(response));
    }

    private void handleRecent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int limit = getIntParam(req, "limit", 10);

        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();

        List<Punishment> recent = allPunishments.stream()
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());

        resp.getWriter().write(gson.toJson(recent));
    }

    private boolean isAuthenticated(HttpServletRequest req) {
        String token = getAuthToken(req);
        return token != null && plugin.getWebServer().isValidSession(token);
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

    private int getIntParam(HttpServletRequest req, String param, int defaultValue) {
        String value = req.getParameter(param);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
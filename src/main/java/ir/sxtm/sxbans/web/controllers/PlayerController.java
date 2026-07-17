package ir.sxtm.sxbans.web.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.IPData;
import ir.sxtm.sxbans.web.models.WebResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerController extends HttpServlet {
    private final SXBans plugin;
    private final Gson gson = new Gson();

    public PlayerController(SXBans plugin) {
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
                handleListPlayers(req, resp);
            } else if (path.equals("/search")) {
                handleSearchPlayers(req, resp);
            } else {
                String playerName = path.substring(1);
                if (playerName.contains("/")) {
                    String[] parts = playerName.split("/");
                    if (parts.length == 2 && parts[1].equals("punishments")) {
                        handlePlayerPunishments(req, resp, parts[0]);
                    } else if (parts.length == 2 && parts[1].equals("history")) {
                        handlePlayerHistory(req, resp, parts[0]);
                    } else if (parts.length == 2 && parts[1].equals("ip")) {
                        handlePlayerIP(req, resp, parts[0]);
                    } else {
                        handlePlayerDetail(req, resp, playerName);
                    }
                } else {
                    handlePlayerDetail(req, resp, playerName);
                }
            }
        } catch (Exception e) {
            plugin.getSXBansLogger().severe("PlayerController error: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Error: " + e.getMessage())));
        }
    }

    private void handleListPlayers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int page = getIntParam(req, "page", 1);
        int limit = getIntParam(req, "limit", 20);
        String search = req.getParameter("search");

        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();
        Map<String, List<Punishment>> playerMap = allPunishments.stream()
                .filter(p -> p.getPlayerName() != null)
                .collect(Collectors.groupingBy(Punishment::getPlayerName));

        List<JsonObject> players = new ArrayList<>();

        for (Map.Entry<String, List<Punishment>> entry : playerMap.entrySet()) {
            String playerName = entry.getKey();
            List<Punishment> punishments = entry.getValue();

            JsonObject player = new JsonObject();
            player.addProperty("name", playerName);
            player.addProperty("totalPunishments", punishments.size());

            long activePunishments = punishments.stream()
                    .filter(Punishment::isActive)
                    .count();
            player.addProperty("activePunishments", activePunishments);

            boolean isBanned = punishments.stream()
                    .anyMatch(p -> p.isActive() && (p.getType() == Punishment.PunishmentType.BAN ||
                            p.getType() == Punishment.PunishmentType.TEMP_BAN));
            player.addProperty("isBanned", isBanned);

            boolean isMuted = punishments.stream()
                    .anyMatch(p -> p.isActive() && (p.getType() == Punishment.PunishmentType.MUTE ||
                            p.getType() == Punishment.PunishmentType.TEMP_MUTE));
            player.addProperty("isMuted", isMuted);

            long warnings = punishments.stream()
                    .filter(p -> p.getType() == Punishment.PunishmentType.WARN)
                    .count();
            player.addProperty("warnings", warnings);

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            player.addProperty("uuid", offlinePlayer.getUniqueId().toString());

            String ip = punishments.stream()
                    .filter(p -> p.getIpAddress() != null)
                    .findFirst()
                    .map(Punishment::getIpAddress)
                    .orElse("Unknown");
            player.addProperty("ip", ip);

            punishments.stream()
                    .max(Comparator.comparing(Punishment::getCreatedAt))
                    .ifPresent(p -> player.addProperty("lastPunishment", p.getCreatedAt()));

            // ===== استفاده از Minotar با Fallback =====
            player.addProperty("headUrl", getPlayerHeadUrl(offlinePlayer.getUniqueId().toString(), 64));

            players.add(player);
        }

        if (search != null && !search.isEmpty()) {
            players = players.stream()
                    .filter(p -> p.get("name").getAsString().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        int total = players.size();
        int start = (page - 1) * limit;
        int end = Math.min(start + limit, total);
        List<JsonObject> pagedPlayers = start < total ? players.subList(start, end) : new ArrayList<>();

        JsonObject response = new JsonObject();
        response.add("players", gson.toJsonTree(pagedPlayers));
        response.addProperty("total", total);
        response.addProperty("page", page);
        response.addProperty("totalPages", (int) Math.ceil(total / (double) limit));
        response.addProperty("success", true);

        resp.getWriter().write(gson.toJson(response));
    }

    private void handlePlayerDetail(HttpServletRequest req, HttpServletResponse resp, String playerName) throws IOException {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = player.getUniqueId();

        List<Punishment> punishments = plugin.getPunishmentManager().getPlayerPunishments(uuid);

        // تفکیک مجازات‌ها بر اساس نوع
        List<Punishment> bans = punishments.stream()
                .filter(p -> p.getType() == Punishment.PunishmentType.BAN ||
                        p.getType() == Punishment.PunishmentType.TEMP_BAN)
                .collect(Collectors.toList());

        List<Punishment> mutes = punishments.stream()
                .filter(p -> p.getType() == Punishment.PunishmentType.MUTE ||
                        p.getType() == Punishment.PunishmentType.TEMP_MUTE)
                .collect(Collectors.toList());

        List<Punishment> warns = punishments.stream()
                .filter(p -> p.getType() == Punishment.PunishmentType.WARN)
                .collect(Collectors.toList());

        List<Punishment> kicks = punishments.stream()
                .filter(p -> p.getType() == Punishment.PunishmentType.KICK ||
                        p.getType() == Punishment.PunishmentType.IP_KICK)
                .collect(Collectors.toList());

        List<Punishment> ipBans = punishments.stream()
                .filter(p -> p.getType() == Punishment.PunishmentType.IP_BAN)
                .collect(Collectors.toList());

        List<Punishment> ipMutes = punishments.stream()
                .filter(p -> p.getType() == Punishment.PunishmentType.IP_MUTE)
                .collect(Collectors.toList());

        JsonObject response = new JsonObject();
        response.addProperty("name", playerName);
        response.addProperty("uuid", uuid.toString());
        response.addProperty("isOnline", player.isOnline());
        response.addProperty("firstPlayed", player.getFirstPlayed());
        response.addProperty("lastPlayed", player.getLastPlayed());
        response.addProperty("totalPunishments", punishments.size());
        response.addProperty("success", true);

        // آمار
        response.addProperty("banCount", bans.size());
        response.addProperty("muteCount", mutes.size());
        response.addProperty("warnCount", warns.size());
        response.addProperty("kickCount", kicks.size());
        response.addProperty("ipBanCount", ipBans.size());
        response.addProperty("ipMuteCount", ipMutes.size());

        // مجازات‌های فعال
        List<Punishment> activePunishments = punishments.stream()
                .filter(Punishment::isActive)
                .collect(Collectors.toList());
        response.addProperty("activePunishmentsCount", activePunishments.size());
        response.add("activePunishments", gson.toJsonTree(activePunishments));

        // همه مجازات‌ها با تفکیک
        response.add("bans", gson.toJsonTree(bans));
        response.add("mutes", gson.toJsonTree(mutes));
        response.add("warns", gson.toJsonTree(warns));
        response.add("kicks", gson.toJsonTree(kicks));
        response.add("ipBans", gson.toJsonTree(ipBans));
        response.add("ipMutes", gson.toJsonTree(ipMutes));
        response.add("punishments", gson.toJsonTree(punishments));

        int warnings = plugin.getPunishmentManager().getWarningCount(uuid);
        response.addProperty("warnings", warnings);
        response.addProperty("maxWarnings", plugin.getConfigManager().getMaxWarnings());

        boolean isBanned = plugin.getPunishmentManager().isPlayerBanned(uuid);
        boolean isMuted = plugin.getPunishmentManager().isPlayerMuted(uuid);
        response.addProperty("isBanned", isBanned);
        response.addProperty("isMuted", isMuted);

        String ip = punishments.stream()
                .filter(p -> p.getIpAddress() != null)
                .findFirst()
                .map(Punishment::getIpAddress)
                .orElse(null);
        if (ip != null) {
            response.addProperty("ip", ip);
            IPData ipData = plugin.getIPTracker().getIPData(ip);
            if (ipData != null) {
                response.addProperty("ipCountry", ipData.getCountry());
                response.addProperty("ipCity", ipData.getCity());
                response.addProperty("ipPlayerCount", ipData.getPlayerCount());
            }
        }

        response.add("altAccounts", gson.toJsonTree(new ArrayList<>()));

        // ===== استفاده از Minotar با Fallback =====
        response.addProperty("headUrl", getPlayerHeadUrl(uuid.toString(), 128));

        resp.getWriter().write(gson.toJson(response));
    }

    private void handleSearchPlayers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("q");
        if (query == null || query.isEmpty()) {
            resp.getWriter().write(gson.toJson(new WebResponse(false, "Query parameter required")));
            return;
        }

        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();

        List<String> matchingPlayers = allPunishments.stream()
                .map(Punishment::getPlayerName)
                .distinct()
                .filter(name -> name != null && name.toLowerCase().contains(query.toLowerCase()))
                .limit(10)
                .collect(Collectors.toList());

        resp.getWriter().write(gson.toJson(matchingPlayers));
    }

    private void handlePlayerPunishments(HttpServletRequest req, HttpServletResponse resp, String playerName) throws IOException {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = player.getUniqueId();

        List<Punishment> punishments = plugin.getPunishmentManager().getPlayerPunishments(uuid);
        punishments.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        resp.getWriter().write(gson.toJson(punishments));
    }

    private void handlePlayerHistory(HttpServletRequest req, HttpServletResponse resp, String playerName) throws IOException {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = player.getUniqueId();

        List<ir.sxtm.sxbans.models.HistoryEntry> history =
                plugin.getPunishmentStorage().getPlayerHistory(uuid);

        resp.getWriter().write(gson.toJson(history));
    }

    private void handlePlayerIP(HttpServletRequest req, HttpServletResponse resp, String playerName) throws IOException {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = player.getUniqueId();

        Set<String> ips = plugin.getIPTracker().getIPsForPlayer(uuid);
        resp.getWriter().write(gson.toJson(ips));
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

    // ===== متد کمکی برای دریافت هد با Minotar و Fallback =====
    private String getPlayerHeadUrl(String uuid, int size) {
        String baseUrl = "https://minotar.net/avatar/";
        String fallbackUuid = "00000000-0000-0000-0000-000000000000";
        String id = (uuid != null && !uuid.isEmpty()) ? uuid : fallbackUuid;
        return baseUrl + id + "/" + size;
    }
}
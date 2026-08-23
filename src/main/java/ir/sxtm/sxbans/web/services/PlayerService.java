package ir.sxtm.sxbans.web.services;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.IPData;
import ir.sxtm.sxbans.web.models.WebPunishment;
import ir.sxtm.sxbans.web.models.WebResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerService {
    private final SXBans plugin;

    public PlayerService(SXBans plugin) {
        this.plugin = plugin;
    }

    public WebResponse getPlayers(int page, int limit, String search, String sort, String order) {
        List<Punishment> allPunishments = plugin.getPunishmentStorage().getAllPunishments();

        Map<String, List<Punishment>> playerMap = allPunishments.stream()
                .collect(Collectors.groupingBy(Punishment::getPlayerName));

        List<Map<String, Object>> players = new ArrayList<>();

        for (Map.Entry<String, List<Punishment>> entry : playerMap.entrySet()) {
            String playerName = entry.getKey();
            List<Punishment> punishments = entry.getValue();

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            UUID uuid = offlinePlayer.getUniqueId();

            Map<String, Object> player = new HashMap<>();
            player.put("name", playerName);
            player.put("uuid", uuid.toString());
            player.put("totalPunishments", punishments.size());

            long active = punishments.stream().filter(Punishment::isActive).count();
            player.put("activePunishments", active);

            boolean banned = punishments.stream()
                    .anyMatch(p -> p.isActive() &&
                            (p.getType() == Punishment.PunishmentType.BAN ||
                                    p.getType() == Punishment.PunishmentType.TEMP_BAN));
            player.put("isBanned", banned);

            boolean muted = punishments.stream()
                    .anyMatch(p -> p.isActive() &&
                            (p.getType() == Punishment.PunishmentType.MUTE ||
                                    p.getType() == Punishment.PunishmentType.TEMP_MUTE));
            player.put("isMuted", muted);

            long warnings = punishments.stream()
                    .filter(p -> p.getType() == Punishment.PunishmentType.WARN)
                    .count();
            player.put("warnings", warnings);

            punishments.stream()
                    .min(Comparator.comparing(Punishment::getCreatedAt))
                    .ifPresent(p -> player.put("firstPunishment", p.getCreatedAt()));

            punishments.stream()
                    .max(Comparator.comparing(Punishment::getCreatedAt))
                    .ifPresent(p -> player.put("lastPunishment", p.getCreatedAt()));

            String ip = punishments.stream()
                    .filter(p -> p.getIpAddress() != null)
                    .findFirst()
                    .map(Punishment::getIpAddress)
                    .orElse("Unknown");
            player.put("ip", ip);

            player.put("headUrl", "https://crafatar.com/avatars/" + uuid.toString() + "?size=64");

            players.add(player);
        }

        if (search != null && !search.isEmpty()) {
            players = players.stream()
                    .filter(p -> ((String) p.get("name")).toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (sort != null) {
            boolean ascending = "asc".equalsIgnoreCase(order);
            players.sort((a, b) -> {
                int comparison = 0;
                switch (sort.toLowerCase()) {
                    case "name":
                        comparison = ((String) a.get("name")).compareTo((String) b.get("name"));
                        break;
                    case "punishments":
                        comparison = Integer.compare(
                                (Integer) a.get("totalPunishments"),
                                (Integer) b.get("totalPunishments")
                        );
                        break;
                    case "warnings":
                        comparison = Integer.compare(
                                (Integer) a.get("warnings"),
                                (Integer) b.get("warnings")
                        );
                        break;
                    case "lastpunishment":
                        Long aTime = (Long) a.get("lastPunishment");
                        Long bTime = (Long) b.get("lastPunishment");
                        comparison = aTime != null && bTime != null ?
                                Long.compare(aTime, bTime) : 0;
                        break;
                    default:
                        comparison = 0;
                }
                return ascending ? comparison : -comparison;
            });
        }

        int total = players.size();
        int start = (page - 1) * limit;
        int end = Math.min(start + limit, total);
        List<Map<String, Object>> paged = start < total ? players.subList(start, end) : new ArrayList<>();

        Map<String, Object> response = new HashMap<>();
        response.put("players", paged);
        response.put("total", total);
        response.put("page", page);
        response.put("totalPages", (int) Math.ceil(total / (double) limit));

        return new WebResponse(true, "Success", response);
    }

    public WebResponse getPlayer(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (!player.hasPlayedBefore() && !player.isOnline()) {
            return new WebResponse(false, "Player not found");
        }

        UUID uuid = player.getUniqueId();
        List<Punishment> punishments = plugin.getPunishmentManager().getPlayerPunishments(uuid);

        Map<String, Object> response = new HashMap<>();
        response.put("name", playerName);
        response.put("uuid", uuid.toString());
        response.put("isOnline", player.isOnline());
        response.put("firstPlayed", player.getFirstPlayed());
        response.put("lastPlayed", player.getLastPlayed());

        List<WebPunishment> webPunishments = punishments.stream()
                .map(WebPunishment::new)
                .collect(Collectors.toList());
        response.put("punishments", webPunishments);
        response.put("totalPunishments", punishments.size());

        List<WebPunishment> activePunishments = punishments.stream()
                .filter(Punishment::isActive)
                .map(WebPunishment::new)
                .collect(Collectors.toList());
        response.put("activePunishments", activePunishments);
        response.put("activePunishmentsCount", activePunishments.size());

        int warnings = plugin.getPunishmentManager().getWarningCount(uuid);
        response.put("warnings", warnings);
        response.put("maxWarnings", plugin.getConfigManager().getMaxWarnings());

        String ip = punishments.stream()
                .filter(p -> p.getIpAddress() != null)
                .findFirst()
                .map(Punishment::getIpAddress)
                .orElse(null);
        if (ip != null) {
            response.put("ip", ip);
            IPData ipData = plugin.getIPTracker().getIPData(ip);
            if (ipData != null) {
                response.put("ipCountry", ipData.getCountry());
                response.put("ipCity", ipData.getCity());
                response.put("ipIsp", ipData.getIsp());
                response.put("ipPlayerCount", ipData.getPlayerCount());
            }
        }

        Set<UUID> alts = plugin.getAltAccountDetector().detectAlts(player.getPlayer());
        response.put("altAccounts", alts.stream()
                .map(Bukkit::getOfflinePlayer)
                .filter(Objects::nonNull)
                .map(OfflinePlayer::getName)
                .collect(Collectors.toList()));

        response.put("headUrl", "https://crafatar.com/avatars/" + uuid.toString() + "?size=128");

        return new WebResponse(true, "Success", response);
    }

    public WebResponse getPlayerPunishments(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (!player.hasPlayedBefore() && !player.isOnline()) {
            return new WebResponse(false, "Player not found");
        }

        List<Punishment> punishments = plugin.getPunishmentManager().getPlayerPunishments(player.getUniqueId());

        List<WebPunishment> webPunishments = punishments.stream()
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .map(WebPunishment::new)
                .collect(Collectors.toList());

        return new WebResponse(true, "Success", webPunishments);
    }

    public WebResponse getPlayerHistory(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (!player.hasPlayedBefore() && !player.isOnline()) {
            return new WebResponse(false, "Player not found");
        }

        List<ir.sxtm.sxbans.models.HistoryEntry> history =
                plugin.getPunishmentStorage().getPlayerHistory(player.getUniqueId());

        return new WebResponse(true, "Success", history);
    }

    public WebResponse clearWarnings(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (!player.hasPlayedBefore() && !player.isOnline()) {
            return new WebResponse(false, "Player not found");
        }

        plugin.getPunishmentManager().resetWarnings(player.getUniqueId());
        return new WebResponse(true, "Warnings cleared for " + playerName);
    }
}
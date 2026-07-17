package ir.sxtm.sxbans.database;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.HistoryEntry;
import ir.sxtm.sxbans.models.IPData;

import java.sql.*;
import java.util.*;

public class SQLiteStorage {
    private final SXBans plugin;
    private final DatabaseManager dbManager;

    public SQLiteStorage(SXBans plugin) {
        this.plugin = plugin;
        this.dbManager = plugin.getDatabaseManager();
    }

    public void savePunishment(Punishment punishment) {
        String sql = "INSERT OR REPLACE INTO punishments (id, player_uuid, player_name, ip_address, type, reason, " +
                "duration, start_time, end_time, status, executor_uuid, executor_name, " +
                "remover_uuid, remover_name, removed_time, remove_reason, created_at, updated_at, " +
                "server_name, world_name, ip_country, ip_city, client_version, client_brand) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setPunishmentParameters(stmt, punishment);
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to save punishment: " + e.getMessage());
        }
    }

    public Punishment getPunishment(UUID id) {
        String sql = "SELECT * FROM punishments WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractPunishment(rs);
            }

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to get punishment: " + e.getMessage());
        }
        return null;
    }

    public List<Punishment> getAllPunishments() {
        List<Punishment> punishments = new ArrayList<>();
        String sql = "SELECT * FROM punishments ORDER BY start_time DESC";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                punishments.add(extractPunishment(rs));
            }

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to get all punishments: " + e.getMessage());
        }
        return punishments;
    }

    public List<Punishment> getPlayerPunishments(UUID playerUUID) {
        List<Punishment> punishments = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE player_uuid = ? ORDER BY start_time DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                punishments.add(extractPunishment(rs));
            }

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to get player punishments: " + e.getMessage());
        }
        return punishments;
    }

    private void setPunishmentParameters(PreparedStatement stmt, Punishment p) throws SQLException {
        stmt.setString(1, p.getId().toString());
        stmt.setString(2, p.getPlayerUUID().toString());
        stmt.setString(3, p.getPlayerName());
        stmt.setString(4, p.getIpAddress());
        stmt.setString(5, p.getType().name());
        stmt.setString(6, p.getReason());
        stmt.setLong(7, p.getDuration());
        stmt.setLong(8, p.getStartTime());
        stmt.setLong(9, p.getEndTime());
        stmt.setString(10, p.getStatus().name());
        stmt.setString(11, p.getExecutorUUID().toString());
        stmt.setString(12, p.getExecutorName());
        stmt.setString(13, p.getRemoverUUID() != null ? p.getRemoverUUID().toString() : null);
        stmt.setString(14, p.getRemoverName());
        stmt.setLong(15, p.getRemovedTime());
        stmt.setString(16, p.getRemoveReason());
        stmt.setLong(17, p.getCreatedAt());
        stmt.setLong(18, p.getUpdatedAt());
        stmt.setString(19, p.getServerName());
        stmt.setString(20, p.getWorldName());
        stmt.setString(21, p.getIpCountry());
        stmt.setString(22, p.getIpCity());
        stmt.setString(23, p.getClientVersion());
        stmt.setString(24, p.getClientBrand());
    }

    private Punishment extractPunishment(ResultSet rs) throws SQLException {
        Punishment punishment = new Punishment(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("player_name"),
                Punishment.PunishmentType.valueOf(rs.getString("type")),
                rs.getString("reason"),
                rs.getLong("duration"),
                UUID.fromString(rs.getString("executor_uuid")),
                rs.getString("executor_name")
        );

        punishment.setIpAddress(rs.getString("ip_address"));
        punishment.setEndTime(rs.getLong("end_time"));
        punishment.setStatus(Punishment.PunishmentStatus.valueOf(rs.getString("status")));

        String removerUUID = rs.getString("remover_uuid");
        if (removerUUID != null) {
            punishment.setRemoverUUID(UUID.fromString(removerUUID));
        }
        punishment.setRemoverName(rs.getString("remover_name"));
        punishment.setRemovedTime(rs.getLong("removed_time"));
        punishment.setRemoveReason(rs.getString("remove_reason"));
        punishment.setUpdatedAt(rs.getLong("updated_at"));
        punishment.setServerName(rs.getString("server_name"));
        punishment.setWorldName(rs.getString("world_name"));
        punishment.setIpCountry(rs.getString("ip_country"));
        punishment.setIpCity(rs.getString("ip_city"));
        punishment.setClientVersion(rs.getString("client_version"));
        punishment.setClientBrand(rs.getString("client_brand"));

        return punishment;
    }

    public void saveHistory(HistoryEntry entry) {
        String sql = "INSERT INTO history (id, player_uuid, player_name, action, details, " +
                "executor_uuid, executor_name, timestamp, ip_address, server_name, world_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entry.getId().toString());
            stmt.setString(2, entry.getPlayerUUID().toString());
            stmt.setString(3, entry.getPlayerName());
            stmt.setString(4, entry.getAction());
            stmt.setString(5, entry.getDetails());
            stmt.setString(6, entry.getExecutorUUID().toString());
            stmt.setString(7, entry.getExecutorName());
            stmt.setLong(8, entry.getTimestamp());
            stmt.setString(9, entry.getIpAddress());
            stmt.setString(10, entry.getServerName());
            stmt.setString(11, entry.getWorldName());
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to save history: " + e.getMessage());
        }
    }

    public List<HistoryEntry> getPlayerHistory(UUID playerUUID) {
        List<HistoryEntry> history = new ArrayList<>();
        String sql = "SELECT * FROM history WHERE player_uuid = ? ORDER BY timestamp DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                history.add(extractHistory(rs));
            }

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to get player history: " + e.getMessage());
        }
        return history;
    }

    private HistoryEntry extractHistory(ResultSet rs) throws SQLException {
        HistoryEntry entry = new HistoryEntry(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("player_name"),
                rs.getString("action"),
                rs.getString("details"),
                UUID.fromString(rs.getString("executor_uuid")),
                rs.getString("executor_name")
        );

        entry.setIpAddress(rs.getString("ip_address"));
        entry.setServerName(rs.getString("server_name"));
        entry.setWorldName(rs.getString("world_name"));

        return entry;
    }

    public void saveIPData(IPData ipData) {
        String sql = "INSERT OR REPLACE INTO ip_data (ip_address, country, city, isp, hostname, first_seen, " +
                "last_seen, total_logins, total_punishments, is_blacklisted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ipData.getIpAddress());
            stmt.setString(2, ipData.getCountry());
            stmt.setString(3, ipData.getCity());
            stmt.setString(4, ipData.getIsp());
            stmt.setString(5, ipData.getHostname());
            stmt.setLong(6, ipData.getFirstSeen());
            stmt.setLong(7, ipData.getLastSeen());
            stmt.setInt(8, ipData.getTotalLogins());
            stmt.setInt(9, ipData.getTotalPunishments());
            stmt.setBoolean(10, ipData.isBlacklisted());
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to save IP data: " + e.getMessage());
        }
    }

    public IPData getIPData(String ip) {
        String sql = "SELECT * FROM ip_data WHERE ip_address = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ip);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                IPData ipData = new IPData(rs.getString("ip_address"));
                ipData.setCountry(rs.getString("country"));
                ipData.setCity(rs.getString("city"));
                ipData.setIsp(rs.getString("isp"));
                ipData.setHostname(rs.getString("hostname"));
                ipData.setFirstSeen(rs.getLong("first_seen"));
                ipData.setLastSeen(rs.getLong("last_seen"));
                ipData.setBlacklisted(rs.getBoolean("is_blacklisted"));
                return ipData;
            }

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to get IP data: " + e.getMessage());
        }
        return null;
    }
}
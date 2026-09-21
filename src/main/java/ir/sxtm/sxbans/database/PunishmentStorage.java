package ir.sxtm.sxbans.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.HistoryEntry;
import ir.sxtm.sxbans.models.IPData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentStorage {
    private final SXBans plugin;
    private final DatabaseManager dbManager;
    private final Gson gson;
    private final File jsonDataDir;
    private final Map<UUID, Punishment> punishmentCache;
    private final Map<UUID, List<HistoryEntry>> historyCache = new ConcurrentHashMap<>();
    private final Map<String, IPData> ipDataMemCache = new ConcurrentHashMap<>();

    public PunishmentStorage(SXBans plugin) {
        this.plugin = plugin;
        this.dbManager = plugin.getDatabaseManager();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeNulls()
                .create();
        this.jsonDataDir = new File(plugin.getDataFolder(), "data");
        this.punishmentCache = new ConcurrentHashMap<>();
    }

    public void initialize() {
        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            if (!jsonDataDir.exists()) {
                jsonDataDir.mkdirs();
            }
            loadAllFromJson();
            plugin.getSXBansLogger().info("JSON storage initialized at: " + jsonDataDir.getAbsolutePath());
        } else {
            createTables();
        }
    }

    private void loadAllFromJson() {
        File punishmentsDir = new File(jsonDataDir, "punishments");
        if (!punishmentsDir.exists()) {
            punishmentsDir.mkdirs();
            return;
        }

        File[] files = punishmentsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                Punishment p = gson.fromJson(reader, Punishment.class);
                if (p != null && p.getId() != null) {
                    punishmentCache.put(p.getId(), p);
                }
            } catch (Exception e) {
                plugin.getSXBansLogger().warning("Failed to load punishment from " + file.getName() + ": " + e.getMessage());
            }
        }

        plugin.getSXBansLogger().info("Loaded " + punishmentCache.size() + " punishments from JSON storage");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            String createPunishments = "CREATE TABLE IF NOT EXISTS punishments (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(32) NOT NULL," +
                    "ip_address VARCHAR(45)," +
                    "type VARCHAR(20) NOT NULL," +
                    "reason TEXT NOT NULL," +
                    "duration BIGINT NOT NULL," +
                    "start_time BIGINT NOT NULL," +
                    "end_time BIGINT NOT NULL," +
                    "status VARCHAR(20) NOT NULL," +
                    "executor_uuid VARCHAR(36) NOT NULL," +
                    "executor_name VARCHAR(32) NOT NULL," +
                    "remover_uuid VARCHAR(36)," +
                    "remover_name VARCHAR(32)," +
                    "removed_time BIGINT," +
                    "remove_reason TEXT," +
                    "created_at BIGINT NOT NULL," +
                    "updated_at BIGINT NOT NULL," +
                    "server_name VARCHAR(64)," +
                    "world_name VARCHAR(64)," +
                    "ip_country VARCHAR(64)," +
                    "ip_city VARCHAR(64)," +
                    "client_version VARCHAR(32)," +
                    "client_brand VARCHAR(64)" +
                    ")";
            stmt.executeUpdate(withUtf8mb4(createPunishments));

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_uuid ON punishments(player_uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_status ON punishments(status)");

            String createHistory = "CREATE TABLE IF NOT EXISTS history (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(32) NOT NULL," +
                    "action VARCHAR(32) NOT NULL," +
                    "details TEXT," +
                    "executor_uuid VARCHAR(36)," +
                    "executor_name VARCHAR(32)," +
                    "ip_address VARCHAR(45)," +
                    "server_name VARCHAR(64)," +
                    "world_name VARCHAR(64)," +
                    "timestamp BIGINT NOT NULL" +
                    ")";
            stmt.executeUpdate(withUtf8mb4(createHistory));
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_history_player ON history(player_uuid)");

            String createIpData = "CREATE TABLE IF NOT EXISTS ip_data (" +
                    "ip_address VARCHAR(45) PRIMARY KEY," +
                    "data TEXT NOT NULL," +
                    "last_seen BIGINT NOT NULL" +
                    ")";
            stmt.executeUpdate(withUtf8mb4(createIpData));

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    private String withUtf8mb4(String createTableSql) {
        if (dbManager.getType() == DatabaseManager.DatabaseType.MYSQL) {
            return createTableSql + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        }
        return createTableSql;
    }

    // ==================== PUNISHMENTS ====================

    public void savePunishment(Punishment punishment) {
        punishmentCache.put(punishment.getId(), punishment);

        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            savePunishmentJson(punishment);
        } else {
            savePunishmentSql(punishment);
        }
    }

    private void savePunishmentJson(Punishment punishment) {
        try {
            File punishmentsDir = new File(jsonDataDir, "punishments");
            if (!punishmentsDir.exists()) {
                punishmentsDir.mkdirs();
            }
            File file = new File(punishmentsDir, punishment.getId().toString() + ".json");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(punishment, writer);
            }
        } catch (IOException e) {
            plugin.getSXBansLogger().severe("Failed to save punishment to JSON: " + e.getMessage());
        }
    }

    private void savePunishmentSql(Punishment punishment) {
        String sql = buildPunishmentUpsertSql();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setPunishmentParameters(stmt, punishment);
            if (dbManager.getType() == DatabaseManager.DatabaseType.MYSQL) {
                setPunishmentParametersNoId(stmt, punishment, 25);
            }
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to save punishment: " + e.getMessage());
        }
    }

    private String buildPunishmentUpsertSql() {
        String columns = "id, player_uuid, player_name, ip_address, type, reason, " +
                "duration, start_time, end_time, status, executor_uuid, executor_name, " +
                "remover_uuid, remover_name, removed_time, remove_reason, created_at, updated_at, " +
                "server_name, world_name, ip_country, ip_city, client_version, client_brand";
        String placeholders = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";

        switch (dbManager.getType()) {
            case MYSQL:
                return "INSERT INTO punishments (" + columns + ") VALUES (" + placeholders + ") " +
                        "ON DUPLICATE KEY UPDATE player_uuid=?, player_name=?, ip_address=?, type=?, reason=?, " +
                        "duration=?, start_time=?, end_time=?, status=?, executor_uuid=?, executor_name=?, " +
                        "remover_uuid=?, remover_name=?, removed_time=?, remove_reason=?, created_at=?, updated_at=?, " +
                        "server_name=?, world_name=?, ip_country=?, ip_city=?, client_version=?, client_brand=?";
            case POSTGRESQL:
                return "INSERT INTO punishments (" + columns + ") VALUES (" + placeholders + ") " +
                        "ON CONFLICT (id) DO UPDATE SET player_uuid=EXCLUDED.player_uuid, player_name=EXCLUDED.player_name, " +
                        "ip_address=EXCLUDED.ip_address, type=EXCLUDED.type, reason=EXCLUDED.reason, " +
                        "duration=EXCLUDED.duration, start_time=EXCLUDED.start_time, end_time=EXCLUDED.end_time, " +
                        "status=EXCLUDED.status, executor_uuid=EXCLUDED.executor_uuid, executor_name=EXCLUDED.executor_name, " +
                        "remover_uuid=EXCLUDED.remover_uuid, remover_name=EXCLUDED.remover_name, removed_time=EXCLUDED.removed_time, " +
                        "remove_reason=EXCLUDED.remove_reason, created_at=EXCLUDED.created_at, updated_at=EXCLUDED.updated_at, " +
                        "server_name=EXCLUDED.server_name, world_name=EXCLUDED.world_name, ip_country=EXCLUDED.ip_country, " +
                        "ip_city=EXCLUDED.ip_city, client_version=EXCLUDED.client_version, client_brand=EXCLUDED.client_brand";
            case SQLITE:
            case H2:
            default:
                return "INSERT OR REPLACE INTO punishments (" + columns + ") VALUES (" + placeholders + ")";
        }
    }

    private void setPunishmentParametersNoId(PreparedStatement stmt, Punishment p, int startIndex) throws SQLException {
        int i = startIndex;
        stmt.setString(i++, p.getPlayerUUID().toString());
        stmt.setString(i++, p.getPlayerName());
        stmt.setString(i++, p.getIpAddress());
        stmt.setString(i++, p.getType().name());
        stmt.setString(i++, p.getReason());
        stmt.setLong(i++, p.getDuration());
        stmt.setLong(i++, p.getStartTime());
        stmt.setLong(i++, p.getEndTime());
        stmt.setString(i++, p.getStatus().name());
        stmt.setString(i++, p.getExecutorUUID().toString());
        stmt.setString(i++, p.getExecutorName());
        stmt.setString(i++, p.getRemoverUUID() != null ? p.getRemoverUUID().toString() : null);
        stmt.setString(i++, p.getRemoverName());
        stmt.setLong(i++, p.getRemovedTime());
        stmt.setString(i++, p.getRemoveReason());
        stmt.setLong(i++, p.getCreatedAt());
        stmt.setLong(i++, p.getUpdatedAt());
        stmt.setString(i++, p.getServerName());
        stmt.setString(i++, p.getWorldName());
        stmt.setString(i++, p.getIpCountry());
        stmt.setString(i++, p.getIpCity());
        stmt.setString(i++, p.getClientVersion());
        stmt.setString(i, p.getClientBrand());
    }

    public Punishment getPunishment(UUID id) {
        if (punishmentCache.containsKey(id)) {
            return punishmentCache.get(id);
        }

        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            return getPunishmentJson(id);
        } else {
            return getPunishmentSql(id);
        }
    }

    private Punishment getPunishmentJson(UUID id) {
        File file = new File(jsonDataDir, "punishments/" + id.toString() + ".json");
        if (!file.exists()) return null;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Punishment p = gson.fromJson(reader, Punishment.class);
            if (p != null) {
                punishmentCache.put(id, p);
            }
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    private Punishment getPunishmentSql(UUID id) {
        String sql = "SELECT * FROM punishments WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Punishment p = extractPunishment(rs);
                punishmentCache.put(id, p);
                return p;
            }

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to get punishment: " + e.getMessage());
        }
        return null;
    }

    public List<Punishment> getAllPunishments() {
        if (!punishmentCache.isEmpty()) {
            return new ArrayList<>(punishmentCache.values());
        }

        List<Punishment> punishments = new ArrayList<>();

        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            File punishmentsDir = new File(jsonDataDir, "punishments");
            if (!punishmentsDir.exists()) return punishments;

            File[] files = punishmentsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                        Punishment p = gson.fromJson(reader, Punishment.class);
                        if (p != null && p.getId() != null) {
                            punishments.add(p);
                            punishmentCache.put(p.getId(), p);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            String sql = "SELECT * FROM punishments ORDER BY start_time DESC";
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    Punishment p = extractPunishment(rs);
                    punishments.add(p);
                    punishmentCache.put(p.getId(), p);
                }

            } catch (SQLException e) {
                plugin.getSXBansLogger().severe("Failed to get all punishments: " + e.getMessage());
            }
        }

        return punishments;
    }

    public void updatePunishment(Punishment punishment) {
        punishmentCache.put(punishment.getId(), punishment);

        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            savePunishmentJson(punishment);
        } else {
            savePunishmentSql(punishment);
        }
    }

    public void deletePunishment(UUID id) {
        punishmentCache.remove(id);

        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            File file = new File(jsonDataDir, "punishments/" + id.toString() + ".json");
            if (file.exists()) {
                file.delete();
            }
        } else {
            String sql = "DELETE FROM punishments WHERE id = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, id.toString());
                stmt.executeUpdate();

            } catch (SQLException e) {
                plugin.getSXBansLogger().severe("Failed to delete punishment: " + e.getMessage());
            }
        }
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

    // ==================== HISTORY ====================

    public void saveHistory(HistoryEntry entry) {
        historyCache.computeIfAbsent(entry.getPlayerUUID(), k -> new ArrayList<>()).add(entry);

        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            saveHistoryJson(entry);
        } else {
            saveHistorySql(entry);
        }
    }

    private void saveHistoryJson(HistoryEntry entry) {
        try {
            File historyDir = new File(jsonDataDir, "history");
            if (!historyDir.exists()) {
                historyDir.mkdirs();
            }
            File file = new File(historyDir, entry.getId().toString() + ".json");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(entry, writer);
            }
        } catch (IOException e) {
            plugin.getSXBansLogger().severe("Failed to save history entry to JSON: " + e.getMessage());
        }
    }

    private void saveHistorySql(HistoryEntry entry) {
        String sql = "INSERT INTO history (id, player_uuid, player_name, action, details, executor_uuid, " +
                "executor_name, ip_address, server_name, world_name, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entry.getId().toString());
            stmt.setString(2, entry.getPlayerUUID().toString());
            stmt.setString(3, entry.getPlayerName());
            stmt.setString(4, entry.getAction());
            stmt.setString(5, entry.getDetails());
            stmt.setString(6, entry.getExecutorUUID() != null ? entry.getExecutorUUID().toString() : null);
            stmt.setString(7, entry.getExecutorName());
            stmt.setString(8, entry.getIpAddress());
            stmt.setString(9, entry.getServerName());
            stmt.setString(10, entry.getWorldName());
            stmt.setLong(11, entry.getTimestamp());
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to save history entry: " + e.getMessage());
        }
    }

    public List<HistoryEntry> getPlayerHistory(UUID playerUUID) {
        if (historyCache.containsKey(playerUUID)) {
            return new ArrayList<>(historyCache.get(playerUUID));
        }

        List<HistoryEntry> entries = new ArrayList<>();

        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            File historyDir = new File(jsonDataDir, "history");
            if (historyDir.exists()) {
                File[] files = historyDir.listFiles((dir, name) -> name.endsWith(".json"));
                if (files != null) {
                    for (File file : files) {
                        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                            HistoryEntry entry = gson.fromJson(reader, HistoryEntry.class);
                            if (entry != null && playerUUID.equals(entry.getPlayerUUID())) {
                                entries.add(entry);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } else {
            String sql = "SELECT * FROM history WHERE player_uuid = ? ORDER BY timestamp DESC";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerUUID.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        entries.add(extractHistoryEntry(rs));
                    }
                }

            } catch (SQLException e) {
                plugin.getSXBansLogger().severe("Failed to get player history: " + e.getMessage());
            }
        }

        entries.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        historyCache.put(playerUUID, entries);
        return entries;
    }

    private HistoryEntry extractHistoryEntry(ResultSet rs) throws SQLException {
        String executorUuidStr = rs.getString("executor_uuid");
        HistoryEntry entry = new HistoryEntry(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("player_name"),
                rs.getString("action"),
                rs.getString("details"),
                executorUuidStr != null ? UUID.fromString(executorUuidStr) : null,
                rs.getString("executor_name")
        );
        entry.setIpAddress(rs.getString("ip_address"));
        entry.setServerName(rs.getString("server_name"));
        entry.setWorldName(rs.getString("world_name"));
        return entry;
    }

    // ==================== IP DATA ====================

    public void saveIPData(IPData ipData) {
        ipDataMemCache.put(ipData.getIpAddress(), ipData);

        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            saveIPDataJson(ipData);
        } else {
            saveIPDataSql(ipData);
        }
    }

    private void saveIPDataJson(IPData ipData) {
        try {
            File ipDir = new File(jsonDataDir, "ipdata");
            if (!ipDir.exists()) {
                ipDir.mkdirs();
            }
            File file = new File(ipDir, sanitizeIpFileName(ipData.getIpAddress()) + ".json");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(ipData, writer);
            }
        } catch (IOException e) {
            plugin.getSXBansLogger().severe("Failed to save IP data to JSON: " + e.getMessage());
        }
    }

    private void saveIPDataSql(IPData ipData) {
        String sql;
        switch (dbManager.getType()) {
            case MYSQL:
                sql = "INSERT INTO ip_data (ip_address, data, last_seen) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE data=?, last_seen=?";
                break;
            case POSTGRESQL:
                sql = "INSERT INTO ip_data (ip_address, data, last_seen) VALUES (?, ?, ?) " +
                        "ON CONFLICT (ip_address) DO UPDATE SET data=EXCLUDED.data, last_seen=EXCLUDED.last_seen";
                break;
            default:
                sql = "INSERT OR REPLACE INTO ip_data (ip_address, data, last_seen) VALUES (?, ?, ?)";
        }

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String json = gson.toJson(ipData);
            stmt.setString(1, ipData.getIpAddress());
            stmt.setString(2, json);
            stmt.setLong(3, ipData.getLastSeen());
            if (dbManager.getType() == DatabaseManager.DatabaseType.MYSQL) {
                stmt.setString(4, json);
                stmt.setLong(5, ipData.getLastSeen());
            }
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to save IP data: " + e.getMessage());
        }
    }

    public IPData getIPData(String ip) {
        if (ipDataMemCache.containsKey(ip)) {
            return ipDataMemCache.get(ip);
        }

        IPData data = null;
        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            File file = new File(jsonDataDir, "ipdata/" + sanitizeIpFileName(ip) + ".json");
            if (file.exists()) {
                try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    data = gson.fromJson(reader, IPData.class);
                } catch (Exception e) {
                    plugin.getSXBansLogger().warning("Failed to read IP data for " + ip + ": " + e.getMessage());
                }
            }
        } else {
            String sql = "SELECT data FROM ip_data WHERE ip_address = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, ip);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        data = gson.fromJson(rs.getString("data"), IPData.class);
                    }
                }

            } catch (SQLException e) {
                plugin.getSXBansLogger().severe("Failed to get IP data: " + e.getMessage());
            }
        }

        if (data != null) {
            ipDataMemCache.put(ip, data);
        }
        return data;
    }

    public List<IPData> getAllIPData() {
        List<IPData> result = new ArrayList<>();

        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            File ipDir = new File(jsonDataDir, "ipdata");
            if (ipDir.exists()) {
                File[] files = ipDir.listFiles((dir, name) -> name.endsWith(".json"));
                if (files != null) {
                    for (File file : files) {
                        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                            IPData data = gson.fromJson(reader, IPData.class);
                            if (data != null) {
                                result.add(data);
                                ipDataMemCache.put(data.getIpAddress(), data);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } else {
            String sql = "SELECT data FROM ip_data";
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    try {
                        IPData data = gson.fromJson(rs.getString("data"), IPData.class);
                        if (data != null) {
                            result.add(data);
                            ipDataMemCache.put(data.getIpAddress(), data);
                        }
                    } catch (Exception ignored) {
                    }
                }

            } catch (SQLException e) {
                plugin.getSXBansLogger().severe("Failed to get all IP data: " + e.getMessage());
            }
        }

        return result;
    }

    private String sanitizeIpFileName(String ip) {
        return ip.replace(":", "_");
    }

    public void saveAll() {
        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            for (Punishment p : punishmentCache.values()) {
                savePunishmentJson(p);
            }
        }
    }

    public Gson getGson() {
        return gson;
    }
}
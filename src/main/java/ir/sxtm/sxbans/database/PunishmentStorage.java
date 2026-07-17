package ir.sxtm.sxbans.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.HistoryEntry;
import ir.sxtm.sxbans.models.IPData;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentStorage {
    private final SXBans plugin;
    private final DatabaseManager dbManager;
    private final ObjectMapper objectMapper;
    private final File jsonDataDir;
    private final Map<UUID, Punishment> punishmentCache;
    private boolean isInitialized;

    public PunishmentStorage(SXBans plugin) {
        this.plugin = plugin;
        this.dbManager = plugin.getDatabaseManager();
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.jsonDataDir = new File(plugin.getDataFolder(), "data");
        this.punishmentCache = new ConcurrentHashMap<>();
        this.isInitialized = false;
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
        isInitialized = true;
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
            try {
                Punishment p = objectMapper.readValue(file, Punishment.class);
                punishmentCache.put(p.getId(), p);
            } catch (IOException e) {
                plugin.getSXBansLogger().warning("Failed to load punishment from " + file.getName() + ": " + e.getMessage());
            }
        }

        plugin.getSXBansLogger().info("Loaded " + punishmentCache.size() + " punishments from JSON storage");
    }

    private void createTables() {
        // SQL for creating tables (MySQL, SQLite, etc.)
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
            stmt.executeUpdate(createPunishments);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_uuid ON punishments(player_uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_status ON punishments(status)");

        } catch (SQLException e) {
            plugin.getSXBansLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    // ===== Punishment Methods =====

    public void savePunishment(Punishment punishment) {
        punishmentCache.put(punishment.getId(), punishment);

        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            savePunishmentJson(punishment);
            plugin.getSXBansLogger().info("Punishment saved: " + punishment.getId() + " - " + punishment.getPlayerName() + " - " + punishment.getType());
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
            objectMapper.writeValue(file, punishment);
        } catch (IOException e) {
            plugin.getSXBansLogger().severe("Failed to save punishment to JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void savePunishmentSql(Punishment punishment) {
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
        try {
            File file = new File(jsonDataDir, "punishments/" + id.toString() + ".json");
            if (!file.exists()) return null;
            Punishment p = objectMapper.readValue(file, Punishment.class);
            punishmentCache.put(id, p);
            return p;
        } catch (IOException e) {
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
                    try {
                        Punishment p = objectMapper.readValue(file, Punishment.class);
                        punishments.add(p);
                        punishmentCache.put(p.getId(), p);
                    } catch (IOException e) {
                        // Skip corrupted files
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

    // ===== Helper Methods =====

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

    // ===== History Methods =====

    public void saveHistory(HistoryEntry entry) {
        // Implement history saving
    }

    public List<HistoryEntry> getPlayerHistory(UUID playerUUID) {
        return new ArrayList<>();
    }

    // ===== IP Data Methods =====

    public void saveIPData(IPData ipData) {
        // Implement IP data saving
    }

    public IPData getIPData(String ip) {
        return null;
    }

    public List<IPData> getAllIPData() {
        return new ArrayList<>();
    }

    public void saveAll() {
        if (dbManager.getType() == DatabaseManager.DatabaseType.JSON) {
            for (Punishment p : punishmentCache.values()) {
                savePunishmentJson(p);
            }
        }
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
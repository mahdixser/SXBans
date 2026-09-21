package ir.sxtm.sxbans.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.HistoryEntry;
import ir.sxtm.sxbans.models.IPData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JSONStorage {
    private final SXBans plugin;
    private final File dataDir;
    private final Gson gson;
    private final ReentrantReadWriteLock lock;

    private final File punishmentsDir;
    private final File historyDir;
    private final File ipDataDir;
    private final File settingsDir;

    public JSONStorage(SXBans plugin) {
        this.plugin = plugin;
        this.dataDir = new File(plugin.getDataFolder(), "data");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeNulls()
                .create();
        this.lock = new ReentrantReadWriteLock();

        this.punishmentsDir = new File(dataDir, "punishments");
        this.historyDir = new File(dataDir, "history");
        this.ipDataDir = new File(dataDir, "ipdata");
        this.settingsDir = new File(dataDir, "settings");

        createDirectories();
    }

    private void createDirectories() {
        if (!dataDir.exists()) dataDir.mkdirs();
        if (!punishmentsDir.exists()) punishmentsDir.mkdirs();
        if (!historyDir.exists()) historyDir.mkdirs();
        if (!ipDataDir.exists()) ipDataDir.mkdirs();
        if (!settingsDir.exists()) settingsDir.mkdirs();
    }

    public void savePunishment(Punishment punishment) {
        lock.writeLock().lock();
        try {
            File file = new File(punishmentsDir, punishment.getId().toString() + ".json");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(punishment, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save punishment: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Punishment getPunishment(UUID id) {
        lock.readLock().lock();
        try {
            File file = new File(punishmentsDir, id.toString() + ".json");
            if (!file.exists()) return null;
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                return gson.fromJson(reader, Punishment.class);
            }
        } catch (Exception e) {
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Punishment> getAllPunishments() {
        List<Punishment> punishments = new ArrayList<>();
        lock.readLock().lock();
        try {
            File[] files = punishmentsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                        Punishment p = gson.fromJson(reader, Punishment.class);
                        if (p != null && p.getId() != null) {
                            punishments.add(p);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            return punishments;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Punishment> getPlayerPunishments(UUID playerUUID) {
        List<Punishment> result = new ArrayList<>();
        lock.readLock().lock();
        try {
            File[] files = punishmentsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                        Punishment p = gson.fromJson(reader, Punishment.class);
                        if (p != null && p.getPlayerUUID() != null && p.getPlayerUUID().equals(playerUUID)) {
                            result.add(p);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void deletePunishment(UUID id) {
        lock.writeLock().lock();
        try {
            File file = new File(punishmentsDir, id.toString() + ".json");
            if (file.exists()) {
                file.delete();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updatePunishment(Punishment punishment) {
        savePunishment(punishment);
    }

    public void saveHistory(HistoryEntry entry) {
        lock.writeLock().lock();
        try {
            File file = new File(historyDir, entry.getId().toString() + ".json");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(entry, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save history: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<HistoryEntry> getPlayerHistory(UUID playerUUID) {
        List<HistoryEntry> history = new ArrayList<>();
        lock.readLock().lock();
        try {
            File[] files = historyDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                        HistoryEntry entry = gson.fromJson(reader, HistoryEntry.class);
                        if (entry != null && entry.getPlayerUUID() != null && entry.getPlayerUUID().equals(playerUUID)) {
                            history.add(entry);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            history.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            return history;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<HistoryEntry> getAllHistory() {
        List<HistoryEntry> history = new ArrayList<>();
        lock.readLock().lock();
        try {
            File[] files = historyDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                        HistoryEntry entry = gson.fromJson(reader, HistoryEntry.class);
                        if (entry != null) history.add(entry);
                    } catch (Exception ignored) {
                    }
                }
            }
            history.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            return history;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void saveIPData(IPData ipData) {
        lock.writeLock().lock();
        try {
            String fileName = sanitizeIpFileName(ipData.getIpAddress());
            File file = new File(ipDataDir, fileName + ".json");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(ipData, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save IP data: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public IPData getIPData(String ip) {
        lock.readLock().lock();
        try {
            String fileName = sanitizeIpFileName(ip);
            File file = new File(ipDataDir, fileName + ".json");
            if (!file.exists()) return null;
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                return gson.fromJson(reader, IPData.class);
            }
        } catch (Exception e) {
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<IPData> getAllIPData() {
        List<IPData> ipDataList = new ArrayList<>();
        lock.readLock().lock();
        try {
            File[] files = ipDataDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                        IPData data = gson.fromJson(reader, IPData.class);
                        if (data != null) ipDataList.add(data);
                    } catch (Exception ignored) {
                    }
                }
            }
            return ipDataList;
        } finally {
            lock.readLock().unlock();
        }
    }

    private String sanitizeIpFileName(String ip) {
        if (ip == null) return "unknown";
        return ip.replace('.', '_').replace(':', '_');
    }

    public void saveSetting(String key, Object value) {
        lock.writeLock().lock();
        try {
            File file = new File(settingsDir, key + ".json");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(value, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save setting: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Object getSetting(String key) {
        lock.readLock().lock();
        try {
            File file = new File(settingsDir, key + ".json");
            if (!file.exists()) return null;
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                return gson.fromJson(reader, Object.class);
            }
        } catch (Exception e) {
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void deleteSetting(String key) {
        lock.writeLock().lock();
        try {
            File file = new File(settingsDir, key + ".json");
            if (file.exists()) {
                file.delete();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void backup() {
        lock.readLock().lock();
        try {
            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) backupDir.mkdirs();

            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());
            File backup = new File(backupDir, "backup_" + timestamp);
            backup.mkdirs();

            copyDirectory(dataDir, backup);
            plugin.getLogger().info("Backup created at: " + backup.getAbsolutePath());
        } finally {
            lock.readLock().unlock();
        }
    }

    private void copyDirectory(File source, File destination) {
        if (!destination.exists()) destination.mkdirs();

        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    copyDirectory(file, new File(destination, file.getName()));
                } else {
                    try {
                        Files.copy(file.toPath(),
                                new File(destination, file.getName()).toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        plugin.getLogger().warning("Failed to copy file: " + file.getName());
                    }
                }
            }
        }
    }

    public void clearAll() {
        lock.writeLock().lock();
        try {
            deleteDirectory(punishmentsDir);
            deleteDirectory(historyDir);
            deleteDirectory(ipDataDir);
            deleteDirectory(settingsDir);

            punishmentsDir.mkdirs();
            historyDir.mkdirs();
            ipDataDir.mkdirs();
            settingsDir.mkdirs();

            plugin.getLogger().info("All data cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void deleteDirectory(File directory) {
        if (!directory.exists()) return;

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    public File getDataDir() {
        return dataDir;
    }
}
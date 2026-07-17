package ir.sxtm.sxbans.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Manager for exporting and importing data.
 */
public class ExportImportManager {
    private final SXBans plugin;
    private final Gson gson;
    private final SimpleDateFormat dateFormat;

    public ExportImportManager(SXBans plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    }

    /**
     * Export all data to a file.
     *
     * @return The exported file, or null if failed
     */
    public File exportAllData() {
        return exportAllData(null);
    }

    /**
     * Export all data to a file with a custom name.
     *
     * @param fileName The custom file name
     * @return The exported file, or null if failed
     */
    public File exportAllData(String fileName) {
        try {
            // Create exports directory
            File exportDir = new File(plugin.getDataFolder(), "exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // Generate file name
            if (fileName == null) {
                fileName = "sxbans_export_" + dateFormat.format(new Date()) + ".json";
            }
            File exportFile = new File(exportDir, fileName);

            // Collect all data
            Map<String, Object> exportData = new LinkedHashMap<>();
            exportData.put("exportDate", System.currentTimeMillis());
            exportData.put("pluginVersion", plugin.getDescription().getVersion());
            exportData.put("serverName", plugin.getServer().getName());

            // Punishments
            List<Punishment> punishments = plugin.getPunishmentStorage().getAllPunishments();
            exportData.put("punishments", punishments);
            exportData.put("totalPunishments", punishments.size());

            // Settings
            exportData.put("settings", plugin.getConfigManager().getConfig().getValues(true));

            // Write to file
            try (Writer writer = new FileWriter(exportFile, StandardCharsets.UTF_8)) {
                gson.toJson(exportData, writer);
            }

            plugin.getSXBansLogger().info("Data exported to: " + exportFile.getAbsolutePath());
            return exportFile;

        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Failed to export data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Export data as a ZIP archive.
     *
     * @return The exported ZIP file, or null if failed
     */
    public File exportAsZip() {
        try {
            File exportDir = new File(plugin.getDataFolder(), "exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            String zipName = "sxbans_export_" + dateFormat.format(new Date()) + ".zip";
            File zipFile = new File(exportDir, zipName);

            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // Export JSON data
                File jsonFile = exportAllData("temp_export.json");
                if (jsonFile != null) {
                    addToZip(zos, jsonFile, "data.json");
                    jsonFile.delete();
                }

                // Add config files
                addToZip(zos, new File(plugin.getDataFolder(), "config.yml"), "config.yml");
                addToZip(zos, new File(plugin.getDataFolder(), "messages.yml"), "messages.yml");
                addToZip(zos, new File(plugin.getDataFolder(), "webadminusers.json"), "webadminusers.json");
                addToZip(zos, new File(plugin.getDataFolder(), "templates.yml"), "templates.yml");

                // Add log file if exists
                File logFile = new File(plugin.getDataFolder(), "logs" + File.separator + "sxbans.log");
                if (logFile.exists()) {
                    addToZip(zos, logFile, "logs/sxbans.log");
                }
            }

            plugin.getSXBansLogger().info("Data exported to ZIP: " + zipFile.getAbsolutePath());
            return zipFile;

        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Failed to export ZIP: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Add a file to a ZIP archive.
     */
    private void addToZip(ZipOutputStream zos, File file, String entryName) throws IOException {
        if (!file.exists()) return;

        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }

        zos.closeEntry();
    }

    /**
     * Import data from a file.
     *
     * @param file The file to import
     * @return true if successful
     */
    public boolean importData(File file) {
        if (!file.exists()) {
            plugin.getSXBansLogger().severe("Import file not found: " + file.getAbsolutePath());
            return false;
        }

        try {
            // Read file
            String content;
            try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                content = sb.toString();
            }

            // Parse JSON
            Map<String, Object> importData = gson.fromJson(content, Map.class);

            // Import punishments
            Object punishmentsObj = importData.get("punishments");
            if (punishmentsObj instanceof List) {
                List<?> punishmentList = (List<?>) punishmentsObj;
                for (Object obj : punishmentList) {
                    String json = gson.toJson(obj);
                    Punishment punishment = gson.fromJson(json, Punishment.class);
                    if (punishment != null) {
                        // Check if punishment already exists - استفاده از getPunishment به جای isPresent
                        Punishment existing = plugin.getPunishmentStorage().getPunishment(punishment.getId());
                        if (existing == null) {
                            plugin.getPunishmentStorage().savePunishment(punishment);
                            // استفاده از متد عمومی addPunishmentToCache
                            plugin.getPunishmentManager().addPunishmentToCache(punishment);
                        }
                    }
                }
                plugin.getSXBansLogger().info("Imported " + punishmentList.size() + " punishments");
            }

            plugin.getSXBansLogger().info("Data imported successfully from: " + file.getName());
            return true;

        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Failed to import data: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Import data from a ZIP archive.
     *
     * @param zipFile The ZIP file to import
     * @return true if successful
     */
    public boolean importFromZip(File zipFile) {
        if (!zipFile.exists()) {
            plugin.getSXBansLogger().severe("ZIP file not found: " + zipFile.getAbsolutePath());
            return false;
        }

        try {
            // Extract and import
            // This would require unzipping and importing each file
            plugin.getSXBansLogger().info("ZIP import not fully implemented yet");
            return false;

        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Failed to import ZIP: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get a list of export files.
     *
     * @return List of export files
     */
    public List<File> getExportFiles() {
        File exportDir = new File(plugin.getDataFolder(), "exports");
        List<File> files = new ArrayList<>();
        if (exportDir.exists()) {
            File[] fileArray = exportDir.listFiles((dir, name) ->
                    name.endsWith(".json") || name.endsWith(".zip")
            );
            if (fileArray != null) {
                files.addAll(Arrays.asList(fileArray));
                files.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            }
        }
        return files;
    }

    /**
     * Delete an export file.
     *
     * @param file The file to delete
     * @return true if deleted
     */
    public boolean deleteExportFile(File file) {
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
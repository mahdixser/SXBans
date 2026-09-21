package ir.sxtm.sxbans.database;

import ir.sxtm.sxbans.SXBans;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class DriverLoader {

    private DriverLoader() {}

    public static boolean ensureDriver(SXBans plugin, DatabaseManager.DatabaseType type) {
        try {
            String fileName;
            String downloadUrl;
            String driverClass;

            switch (type) {
                case MYSQL:
                    fileName = "mysql-connector-java-8.0.33.jar";
                    downloadUrl = "https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.33/mysql-connector-java-8.0.33.jar";
                    driverClass = "com.mysql.cj.jdbc.Driver";
                    break;
                case SQLITE:
                    fileName = "sqlite-jdbc-3.42.0.0.jar";
                    downloadUrl = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar";
                    driverClass = "org.sqlite.JDBC";
                    break;
                case POSTGRESQL:
                    fileName = "postgresql-42.6.0.jar";
                    downloadUrl = "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.6.0/postgresql-42.6.0.jar";
                    driverClass = "org.postgresql.Driver";
                    break;
                case H2:
                    fileName = "h2-2.2.220.jar";
                    downloadUrl = "https://repo1.maven.org/maven2/com/h2database/h2/2.2.220/h2-2.2.220.jar";
                    driverClass = "org.h2.Driver";
                    break;
                default:
                    return false;
            }

            File libsDir = new File(plugin.getDataFolder(), "libs");
            if (!libsDir.exists()) libsDir.mkdirs();

            File jarFile = new File(libsDir, fileName);

            if (!jarFile.exists()) {
                plugin.getSXBansLogger().info("Downloading " + fileName + " (only on first run)...");
                download(downloadUrl, jarFile);
                plugin.getSXBansLogger().info("Downloaded " + fileName + " (" + (jarFile.length() / 1024) + " KB)");
            }

            // تمام jar های داخل پوشه libs رو لود کن
            File[] jars = libsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            int count = (jars != null) ? jars.length : 0;
            URL[] urls = new URL[count];
            for (int i = 0; i < count; i++) {
                urls[i] = jars[i].toURI().toURL();
            }

            URLClassLoader classLoader = new URLClassLoader(
                    urls, SXBans.class.getClassLoader());

            Class.forName(driverClass, true, classLoader);
            return true;

        } catch (Exception e) {
            plugin.getSXBansLogger().severe("Failed to prepare JDBC driver for " + type.name() + ": " + e.getMessage());
            plugin.getSXBansLogger().severe("Manual fix: download the driver jar yourself and place it inside:");
            plugin.getSXBansLogger().severe("  " + new File(plugin.getDataFolder(), "libs").getAbsolutePath());
            return false;
        }
    }

    private static void download(String urlStr, File target) throws Exception {
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "SXBans-DriverLoader");

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IllegalStateException("HTTP " + code + " while downloading " + urlStr);
        }

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
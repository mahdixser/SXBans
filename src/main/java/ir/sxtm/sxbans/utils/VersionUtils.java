package ir.sxtm.sxbans.utils;

import org.bukkit.Bukkit;

/**
 * Utility class for version handling and compatibility.
 */
public class VersionUtils {
    private static String serverVersion;
    private static int majorVersion;
    private static int minorVersion;
    private static String versionString;
    private static boolean isPaper;
    private static boolean isSpigot;
    private static boolean isCraftBukkit;

    static {
        try {
            versionString = Bukkit.getServer().getClass().getPackage().getName();
            String[] parts = versionString.split("\\.");
            if (parts.length >= 4) {
                serverVersion = parts[3];
                // Parse version like "v1_19_R3"
                String[] versionParts = serverVersion.substring(1).split("_");
                if (versionParts.length >= 2) {
                    majorVersion = Integer.parseInt(versionParts[0]);
                    minorVersion = Integer.parseInt(versionParts[1]);
                }
            }

            // Detect server type
            try {
                Class.forName("com.destroystokyo.paper.PaperConfig");
                isPaper = true;
            } catch (ClassNotFoundException e) {
                isPaper = false;
            }

            try {
                Class.forName("org.spigotmc.SpigotConfig");
                isSpigot = true;
            } catch (ClassNotFoundException e) {
                isSpigot = false;
            }

            isCraftBukkit = !isPaper && !isSpigot;

        } catch (Exception e) {
            serverVersion = "Unknown";
            majorVersion = 0;
            minorVersion = 0;
        }
    }

    /**
     * Get the server version string.
     *
     * @return The server version (e.g., "v1_19_R3")
     */
    public static String getServerVersion() {
        return serverVersion;
    }

    /**
     * Get the major version number.
     *
     * @return The major version (e.g., 1 for 1.19)
     */
    public static int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Get the minor version number.
     *
     * @return The minor version (e.g., 19 for 1.19)
     */
    public static int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Check if the server is running Paper.
     *
     * @return true if Paper
     */
    public static boolean isPaper() {
        return isPaper;
    }

    /**
     * Check if the server is running Spigot.
     *
     * @return true if Spigot
     */
    public static boolean isSpigot() {
        return isSpigot;
    }

    /**
     * Check if the server is running CraftBukkit.
     *
     * @return true if CraftBukkit
     */
    public static boolean isCraftBukkit() {
        return isCraftBukkit;
    }

    /**
     * Check if the server version is at least a specific version.
     *
     * @param major The major version
     * @param minor The minor version
     * @return true if the server version is >= the specified version
     */
    public static boolean isAtLeastVersion(int major, int minor) {
        if (majorVersion > major) return true;
        if (majorVersion < major) return false;
        return minorVersion >= minor;
    }

    /**
     * Check if the server version is between two versions.
     *
     * @param majorMin The minimum major version
     * @param minorMin The minimum minor version
     * @param majorMax The maximum major version
     * @param minorMax The maximum minor version
     * @return true if the server version is between the specified versions
     */
    public static boolean isBetweenVersions(int majorMin, int minorMin, int majorMax, int minorMax) {
        if (majorVersion < majorMin || majorVersion > majorMax) return false;
        if (majorVersion == majorMin && minorVersion < minorMin) return false;
        if (majorVersion == majorMax && minorVersion > minorMax) return false;
        return true;
    }

    /**
     * Check if the server is running Minecraft 1.13 or higher.
     *
     * @return true if 1.13+
     */
    public static boolean isAtLeast13() {
        return isAtLeastVersion(1, 13);
    }

    /**
     * Check if the server is running Minecraft 1.14 or higher.
     *
     * @return true if 1.14+
     */
    public static boolean isAtLeast14() {
        return isAtLeastVersion(1, 14);
    }

    /**
     * Check if the server is running Minecraft 1.16 or higher.
     *
     * @return true if 1.16+
     */
    public static boolean isAtLeast16() {
        return isAtLeastVersion(1, 16);
    }

    /**
     * Check if the server is running Minecraft 1.17 or higher.
     *
     * @return true if 1.17+
     */
    public static boolean isAtLeast17() {
        return isAtLeastVersion(1, 17);
    }

    /**
     * Check if the server is running Minecraft 1.18 or higher.
     *
     * @return true if 1.18+
     */
    public static boolean isAtLeast18() {
        return isAtLeastVersion(1, 18);
    }

    /**
     * Check if the server is running Minecraft 1.19 or higher.
     *
     * @return true if 1.19+
     */
    public static boolean isAtLeast19() {
        return isAtLeastVersion(1, 19);
    }

    /**
     * Check if the server is running Minecraft 1.20 or higher.
     *
     * @return true if 1.20+
     */
    public static boolean isAtLeast20() {
        return isAtLeastVersion(1, 20);
    }

    /**
     * Get the NMS version for reflection.
     *
     * @return The NMS version string
     */
    public static String getNMSVersion() {
        return serverVersion;
    }

    /**
     * Get the full server info.
     *
     * @return Server info string
     */
    public static String getServerInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Version: ").append(serverVersion);
        info.append(" (1.").append(minorVersion).append(")");
        if (isPaper) info.append(" [Paper]");
        else if (isSpigot) info.append(" [Spigot]");
        else if (isCraftBukkit) info.append(" [CraftBukkit]");
        return info.toString();
    }
}
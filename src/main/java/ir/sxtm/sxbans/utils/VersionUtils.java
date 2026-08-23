package ir.sxtm.sxbans.utils;

import org.bukkit.Bukkit;

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

                String[] versionParts = serverVersion.substring(1).split("_");
                if (versionParts.length >= 2) {
                    majorVersion = Integer.parseInt(versionParts[0]);
                    minorVersion = Integer.parseInt(versionParts[1]);
                }
            }

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

    public static String getServerVersion() {
        return serverVersion;
    }

    public static int getMajorVersion() {
        return majorVersion;
    }

    public static int getMinorVersion() {
        return minorVersion;
    }

    public static boolean isPaper() {
        return isPaper;
    }

    public static boolean isSpigot() {
        return isSpigot;
    }

    public static boolean isCraftBukkit() {
        return isCraftBukkit;
    }

    public static boolean isAtLeastVersion(int major, int minor) {
        if (majorVersion > major) return true;
        if (majorVersion < major) return false;
        return minorVersion >= minor;
    }

    public static boolean isBetweenVersions(int majorMin, int minorMin, int majorMax, int minorMax) {
        if (majorVersion < majorMin || majorVersion > majorMax) return false;
        if (majorVersion == majorMin && minorVersion < minorMin) return false;
        if (majorVersion == majorMax && minorVersion > minorMax) return false;
        return true;
    }

    public static boolean isAtLeast13() {
        return isAtLeastVersion(1, 13);
    }

    public static boolean isAtLeast14() {
        return isAtLeastVersion(1, 14);
    }

    public static boolean isAtLeast16() {
        return isAtLeastVersion(1, 16);
    }

    public static boolean isAtLeast17() {
        return isAtLeastVersion(1, 17);
    }

    public static boolean isAtLeast18() {
        return isAtLeastVersion(1, 18);
    }

    public static boolean isAtLeast19() {
        return isAtLeastVersion(1, 19);
    }

    public static boolean isAtLeast20() {
        return isAtLeastVersion(1, 20);
    }

    public static String getNMSVersion() {
        return serverVersion;
    }

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
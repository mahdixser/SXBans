package ir.sxtm.sxbans.utils;

import java.util.UUID;
import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        return USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidIP(String ip) {
        if (ip == null) return false;
        return IP_PATTERN.matcher(ip).matches();
    }

    public static boolean isValidUUID(String uuid) {
        if (uuid == null) return false;
        return UUID_PATTERN.matcher(uuid).matches();
    }

    public static boolean isValidUUID(UUID uuid) {
        return uuid != null;
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidHexColor(String hex) {
        if (hex == null) return false;
        return HEX_COLOR_PATTERN.matcher(hex).matches();
    }

    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    public static boolean isInRange(long value, long min, long max) {
        return value >= min && value <= max;
    }

    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    public static boolean isValidReason(String reason) {
        return isNotEmpty(reason) && reason.length() <= 255;
    }

    public static boolean isValidDuration(long duration) {
        return duration >= -1;
    }

    public static boolean isValidPermission(String permission) {
        if (permission == null) return false;
        return permission.matches("^[a-zA-Z0-9.\\-*]+$");
    }

    public static boolean isValidChannel(String channel) {
        if (channel == null) return false;
        return channel.matches("^[a-zA-Z0-9:._-]+$");
    }

    public static String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("[<>\"'%;()&+]", "");
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null) return null;
        return filename.replaceAll("[^a-zA-Z0-9._-]", "");
    }
}
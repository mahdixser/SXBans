package ir.sxtm.sxbans.utils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?:(\\d+)y)?(?:(\\d+)M)?(?:(\\d+)w)?(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?"
    );

    public static long parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return -1;
        }

        if (timeStr.equalsIgnoreCase("permanent") || timeStr.equals("-1")) {
            return -1;
        }

        if (timeStr.equalsIgnoreCase("0") || timeStr.equalsIgnoreCase("0s")) {
            return 0;
        }

        try {

            if (timeStr.matches("\\d+")) {
                return Long.parseLong(timeStr) * 1000;
            }

            Matcher matcher = TIME_PATTERN.matcher(timeStr);
            if (!matcher.matches()) {
                return -1;
            }

            long totalMillis = 0;

            String years = matcher.group(1);
            String months = matcher.group(2);
            String weeks = matcher.group(3);
            String days = matcher.group(4);
            String hours = matcher.group(5);
            String minutes = matcher.group(6);
            String seconds = matcher.group(7);

            if (years != null) {
                totalMillis += Long.parseLong(years) * 365L * 24 * 60 * 60 * 1000;
            }
            if (months != null) {
                totalMillis += Long.parseLong(months) * 30L * 24 * 60 * 60 * 1000;
            }
            if (weeks != null) {
                totalMillis += Long.parseLong(weeks) * 7L * 24 * 60 * 60 * 1000;
            }
            if (days != null) {
                totalMillis += Long.parseLong(days) * 24 * 60 * 60 * 1000;
            }
            if (hours != null) {
                totalMillis += Long.parseLong(hours) * 60 * 60 * 1000;
            }
            if (minutes != null) {
                totalMillis += Long.parseLong(minutes) * 60 * 1000;
            }
            if (seconds != null) {
                totalMillis += Long.parseLong(seconds) * 1000;
            }

            return totalMillis > 0 ? totalMillis : -1;

        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String formatTime(long millis) {
        if (millis < 0) {
            return "Permanent";
        }

        if (millis == 0) {
            return "0s";
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append("d");
        }
        if (hours > 0) {
            sb.append(hours).append("h");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("s");
        }

        return sb.toString();
    }

    public static String formatTimeDetailed(long millis) {
        if (millis < 0) {
            return "Permanent";
        }

        if (millis == 0) {
            return "0 seconds";
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        if (days > 0) {
            sb.append(days).append(" day").append(days > 1 ? "s" : "");
            first = false;
        }
        if (hours > 0) {
            if (!first) sb.append(", ");
            sb.append(hours).append(" hour").append(hours > 1 ? "s" : "");
            first = false;
        }
        if (minutes > 0) {
            if (!first) sb.append(", ");
            sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
            first = false;
        }
        if (seconds > 0 || (days == 0 && hours == 0 && minutes == 0)) {
            if (!first) sb.append(", ");
            sb.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
        }

        return sb.toString();
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static long nowSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    public static boolean isExpired(long startTime, long duration) {
        if (duration < 0) return false;
        return System.currentTimeMillis() > (startTime + duration);
    }

    public static long getRemaining(long startTime, long duration) {
        if (duration < 0) return -1;
        long remaining = (startTime + duration) - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public static long secondsToTicks(long seconds) {
        return seconds * 20;
    }

    public static long millisToTicks(long millis) {
        return (millis / 50);
    }

    public static long ticksToMillis(long ticks) {
        return ticks * 50;
    }

    public static boolean isValidTimeFormat(String timeStr) {
        if (timeStr == null) return false;
        if (timeStr.equalsIgnoreCase("permanent") || timeStr.equals("-1")) return true;
        return parseTime(timeStr) >= 0;
    }

    public static String getFormattedDateTime(String format) {
        return new java.text.SimpleDateFormat(format).format(new java.util.Date());
    }

    public static String getISODateTime() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new java.util.Date());
    }
}
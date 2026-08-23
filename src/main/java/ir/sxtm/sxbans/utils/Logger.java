package ir.sxtm.sxbans.utils;

import ir.sxtm.sxbans.SXBans;
import org.bukkit.Bukkit;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Logger {
    private final SXBans plugin;
    private final java.util.logging.Logger bukkitLogger;
    private final File logFile;
    private final SimpleDateFormat dateFormat;
    private final ConcurrentLinkedQueue<String> logQueue;
    private boolean fileLogging;
    private boolean consoleLogging;
    private boolean webLogging;
    private Thread logThread;
    private volatile boolean running;

    public Logger(SXBans plugin) {
        this.plugin = plugin;
        this.bukkitLogger = plugin.getLogger();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.logQueue = new ConcurrentLinkedQueue<>();
        this.logFile = new File(plugin.getDataFolder(), "logs" + File.separator + "sxbans.log");
        this.running = true;

        this.consoleLogging = plugin.getConfigManager().isConsoleLogging();
        this.fileLogging = plugin.getConfigManager().isFileLogging();
        this.webLogging = plugin.getConfigManager().isWebLogging();

        logFile.getParentFile().mkdirs();

        startLogProcessor();
    }

    private void startLogProcessor() {
        logThread = new Thread(() -> {
            while (running) {
                try {
                    String log = logQueue.poll();
                    if (log != null) {
                        processLog(log);
                    } else {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    bukkitLogger.warning("Error processing log: " + e.getMessage());
                }
            }
        }, "SXBans-Logger");
        logThread.setDaemon(true);
        logThread.start();
    }

    private void processLog(String log) {

        if (consoleLogging) {
            bukkitLogger.info(log);
        }

        if (fileLogging) {
            writeToFile(log);
        }

        if (webLogging) {
            sendToWeb(log);
        }
    }

    private void writeToFile(String log) {
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(log);
        } catch (IOException e) {
            bukkitLogger.warning("Failed to write to log file: " + e.getMessage());
        }
    }

    private void sendToWeb(String log) {

        if (plugin.getWebServer() != null) {

        }
    }

    public void info(String message) {
        String log = formatLog("INFO", message);
        logQueue.offer(log);
    }

    public void warning(String message) {
        String log = formatLog("WARN", message);
        logQueue.offer(log);
    }

    public void severe(String message) {
        String log = formatLog("ERROR", message);
        logQueue.offer(log);
    }

    public void debug(String message) {
        if (plugin.getConfigManager().getBoolean("debug", false)) {
            String log = formatLog("DEBUG", message);
            logQueue.offer(log);
        }
    }

    public void error(String message, Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String log = formatLog("ERROR", message + "\n" + sw.toString());
        logQueue.offer(log);
    }

    public void logPunishment(String type, String details) {
        String log = formatLog("PUNISHMENT", type + " - " + details);
        logQueue.offer(log);
    }

    public void logWeb(String type, String details) {
        String log = formatLog("WEB", type + " - " + details);
        logQueue.offer(log);
    }

    private String formatLog(String level, String message) {
        String timestamp = dateFormat.format(new Date());
        return String.format("[%s] [SXBans/%s] %s", timestamp, level, message);
    }

    public File getLogFile() {
        return logFile;
    }

    public java.util.List<String> getRecentLogs(int lines) {
        java.util.List<String> recent = new java.util.ArrayList<>();

        if (!logFile.exists()) return recent;

        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) return recent;

            long pos = fileLength - 1;
            int linesRead = 0;
            StringBuilder line = new StringBuilder();

            while (pos >= 0 && linesRead < lines) {
                raf.seek(pos);
                char c = (char) raf.readByte();

                if (c == '\n') {
                    if (line.length() > 0) {
                        recent.add(0, line.reverse().toString());
                        line.setLength(0);
                        linesRead++;
                    }
                } else if (c != '\r') {
                    line.append(c);
                }
                pos--;
            }

            if (line.length() > 0 && linesRead < lines) {
                recent.add(0, line.reverse().toString());
            }

        } catch (IOException e) {
            bukkitLogger.warning("Failed to read recent logs: " + e.getMessage());
        }

        return recent;
    }

    public void clearLogs() {
        try {
            new FileWriter(logFile, false).close();
            info("Log file cleared");
        } catch (IOException e) {
            warning("Failed to clear log file: " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        if (logThread != null) {
            logThread.interrupt();
            try {
                logThread.join(5000);
            } catch (InterruptedException ignored) {}
        }

        String log;
        while ((log = logQueue.poll()) != null) {
            if (consoleLogging) {
                bukkitLogger.info(log);
            }
            if (fileLogging) {
                writeToFile(log);
            }
        }
    }
}
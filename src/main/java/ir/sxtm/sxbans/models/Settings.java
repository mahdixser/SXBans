package ir.sxtm.sxbans.models;

import java.util.HashMap;
import java.util.Map;

public class Settings {
    private final Map<String, Object> settings;
    private long lastUpdated;

    public Settings() {
        this.settings = new HashMap<>();
        this.lastUpdated = System.currentTimeMillis();
    }

    public void set(String key, Object value) {
        settings.put(key, value);
        lastUpdated = System.currentTimeMillis();
    }

    public Object get(String key) {
        return settings.get(key);
    }

    public String getString(String key) {
        Object value = settings.get(key);
        return value != null ? value.toString() : null;
    }

    public int getInt(String key) {
        Object value = settings.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    public boolean getBoolean(String key) {
        Object value = settings.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    public long getLong(String key) {
        Object value = settings.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0;
    }

    public Map<String, Object> getAll() {
        return new HashMap<>(settings);
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "Settings{" +
                "settings=" + settings +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
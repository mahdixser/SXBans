package ir.sxtm.sxbans.models;

import java.util.HashSet;
import java.util.Set;

public class WebUser {
    private String username;
    private String passwordHash;
    private int level;
    private Set<String> permissions;
    private long lastLogin;
    private long createdAt;
    private boolean enabled;
    private String sessionToken;
    private long sessionExpiry;

    public WebUser(String username, String passwordHash, int level) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.level = level;
        this.permissions = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
        this.enabled = true;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
    public void addPermission(String permission) { permissions.add(permission); }
    public void removePermission(String permission) { permissions.remove(permission); }
    public boolean hasPermission(String permission) {
        return permissions.contains("*") || permissions.contains(permission);
    }
    public long getLastLogin() { return lastLogin; }
    public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }
    public long getCreatedAt() { return createdAt; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public long getSessionExpiry() { return sessionExpiry; }
    public void setSessionExpiry(long sessionExpiry) { this.sessionExpiry = sessionExpiry; }
    public boolean isSessionValid() {
        return sessionToken != null && System.currentTimeMillis() < sessionExpiry;
    }

    @Override
    public String toString() {
        return "WebUser{" +
                "username='" + username + '\'' +
                ", level=" + level +
                ", enabled=" + enabled +
                '}';
    }
}
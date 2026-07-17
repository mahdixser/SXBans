package ir.sxtm.sxbans.web.services;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.WebUser;
import ir.sxtm.sxbans.web.models.WebResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

public class AuthService {
    private final SXBans plugin;

    public AuthService(SXBans plugin) {
        this.plugin = plugin;
    }

    public WebResponse login(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return new WebResponse(false, "Username and password required");
        }

        WebUser user = plugin.getWebUsersManager().getUser(username);
        if (user == null || !user.isEnabled()) {
            return new WebResponse(false, "Invalid credentials");
        }

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            return new WebResponse(false, "Invalid credentials");
        }

        // Update last login
        user.setLastLogin(System.currentTimeMillis());
        plugin.getWebUsersManager().saveUsers();

        // Create session
        String token = plugin.getWebUsersManager().createSession(username);
        if (token == null) {
            return new WebResponse(false, "Failed to create session");
        }

        plugin.getWebServer().createSession(token, username);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("token", token);
        data.put("username", username);
        data.put("level", user.getLevel());
        data.put("permissions", user.getPermissions());

        return new WebResponse(true, "Login successful", data);
    }

    public WebResponse logout(String token) {
        if (token == null) {
            return new WebResponse(false, "No session token provided");
        }

        plugin.getWebUsersManager().invalidateSession(token);
        plugin.getWebServer().invalidateSession(token);

        return new WebResponse(true, "Logout successful");
    }

    public WebResponse validateSession(String token) {
        if (token == null) {
            return new WebResponse(false, "No session token provided");
        }

        boolean valid = plugin.getWebServer().isValidSession(token);
        if (!valid) {
            return new WebResponse(false, "Invalid session");
        }

        String username = plugin.getWebServer().getUsernameFromSession(token);
        WebUser user = plugin.getWebUsersManager().getUser(username);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("valid", true);
        data.put("username", username);
        data.put("level", user != null ? user.getLevel() : 0);
        data.put("permissions", user != null ? user.getPermissions() : new java.util.HashSet<>());

        return new WebResponse(true, "Session valid", data);
    }

    public WebResponse changePassword(String token, String oldPassword, String newPassword) {
        if (token == null) {
            return new WebResponse(false, "Not authenticated");
        }

        String username = plugin.getWebServer().getUsernameFromSession(token);
        if (username == null) {
            return new WebResponse(false, "Not authenticated");
        }

        WebUser user = plugin.getWebUsersManager().getUser(username);
        if (user == null) {
            return new WebResponse(false, "User not found");
        }

        if (!BCrypt.checkpw(oldPassword, user.getPasswordHash())) {
            return new WebResponse(false, "Current password is incorrect");
        }

        if (newPassword == null || newPassword.length() < 6) {
            return new WebResponse(false, "New password must be at least 6 characters");
        }

        plugin.getWebUsersManager().updateUserPassword(username, newPassword);
        return new WebResponse(true, "Password changed successfully");
    }
}
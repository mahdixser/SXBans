package ir.sxtm.sxbans.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.WebUser;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebUsersManager {
    private final SXBans plugin;
    private final File usersFile;
    private final Map<String, WebUser> users;
    private final Map<String, String> sessionTokens;
    private final Gson gson;
    private long lastLoad;

    public WebUsersManager(SXBans plugin) {
        this.plugin = plugin;
        this.usersFile = new File(plugin.getDataFolder(), "webadminusers.json");
        this.users = new ConcurrentHashMap<>();
        this.sessionTokens = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.lastLoad = 0;
    }

    public void loadUsers() {
        if (!usersFile.exists()) {
            createDefaultUsers();
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(usersFile), java.nio.charset.StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, WebUser>>(){}.getType();
            Map<String, WebUser> loadedUsers = gson.fromJson(reader, type);

            if (loadedUsers != null) {
                users.clear();
                users.putAll(loadedUsers);
            }

            lastLoad = System.currentTimeMillis();
            plugin.getLogger().info("Loaded " + users.size() + " web users");

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load web users: " + e.getMessage());
            createDefaultUsers();
        }
    }

    private void createDefaultUsers() {

        String randomPassword = generateSecureRandomPassword();
        WebUser admin = new WebUser("admin", BCrypt.hashpw(randomPassword, BCrypt.gensalt()), 100);
        admin.addPermission("*");
        users.put("admin", admin);
        saveUsers();

        plugin.getLogger().warning("=================================================");
        plugin.getLogger().warning(" SXBans Web Panel - Initial admin account password generated:");
        plugin.getLogger().warning(" Username: admin");
        plugin.getLogger().warning(" Password: " + randomPassword);
        plugin.getLogger().warning(" Please log in now and change this password from Settings.");
        plugin.getLogger().warning("=================================================");
    }

    private String generateSecureRandomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public void saveUsers() {

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(usersFile), java.nio.charset.StandardCharsets.UTF_8)) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save web users: " + e.getMessage());
        }
    }

    public boolean authenticate(String username, String password) {
        WebUser user = users.get(username);
        if (user == null || !user.isEnabled()) return false;

        boolean authenticated = BCrypt.checkpw(password, user.getPasswordHash());
        if (authenticated) {
            user.setLastLogin(System.currentTimeMillis());
            saveUsers();
        }

        return authenticated;
    }

    public String createSession(String username) {
        WebUser user = users.get(username);
        if (user == null) return null;

        String token = UUID.randomUUID().toString() + System.currentTimeMillis();
        String hashedToken = BCrypt.hashpw(token, BCrypt.gensalt());

        user.setSessionToken(hashedToken);
        user.setSessionExpiry(System.currentTimeMillis() + 86400000);
        sessionTokens.put(token, username);
        saveUsers();

        return token;
    }

    public boolean validateSession(String token) {
        if (token == null) return false;

        String username = sessionTokens.get(token);
        if (username == null) return false;

        WebUser user = users.get(username);
        if (user == null || !user.isEnabled()) {
            sessionTokens.remove(token);
            return false;
        }

        if (!user.isSessionValid()) {
            sessionTokens.remove(token);
            return false;
        }

        return true;
    }

    public WebUser getUserBySession(String token) {
        if (!validateSession(token)) return null;

        String username = sessionTokens.get(token);
        if (username == null) return null;

        return users.get(username);
    }

    public void invalidateSession(String token) {
        sessionTokens.remove(token);
    }

    public WebUser getUser(String username) {
        return users.get(username);
    }

    public boolean createUser(String username, String password, int level) {
        if (users.containsKey(username)) return false;

        WebUser user = new WebUser(username, BCrypt.hashpw(password, BCrypt.gensalt()), level);
        users.put(username, user);
        saveUsers();
        return true;
    }

    public boolean deleteUser(String username) {
        if ("admin".equals(username)) return false;

        WebUser removed = users.remove(username);
        if (removed != null) {
            saveUsers();
            return true;
        }
        return false;
    }

    public boolean updateUserLevel(String username, int level) {
        WebUser user = users.get(username);
        if (user == null) return false;

        user.setLevel(level);
        saveUsers();
        return true;
    }

    public boolean updateUserPassword(String username, String newPassword) {
        WebUser user = users.get(username);
        if (user == null) return false;

        user.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        saveUsers();
        return true;
    }

    public boolean toggleUserEnabled(String username) {
        WebUser user = users.get(username);
        if (user == null) return false;

        user.setEnabled(!user.isEnabled());
        saveUsers();
        return true;
    }

    public boolean addPermission(String username, String permission) {
        WebUser user = users.get(username);
        if (user == null) return false;

        user.addPermission(permission);
        saveUsers();
        return true;
    }

    public boolean removePermission(String username, String permission) {
        WebUser user = users.get(username);
        if (user == null) return false;

        user.removePermission(permission);
        saveUsers();
        return true;
    }

    public List<WebUser> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public int getUserCount() {
        return users.size();
    }

    public long getLastLoad() {
        return lastLoad;
    }
}
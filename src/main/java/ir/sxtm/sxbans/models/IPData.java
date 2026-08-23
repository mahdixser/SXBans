package ir.sxtm.sxbans.models;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class IPData {
    private final String ipAddress;
    private final Set<UUID> playerUUIDs;
    private final Set<String> playerNames;
    private String country;
    private String city;
    private String isp;
    private String hostname;
    private long firstSeen;
    private long lastSeen;
    private int totalLogins;
    private int totalPunishments;
    private boolean isBlacklisted;

    public IPData() {
        this.ipAddress = null;
        this.playerUUIDs = new HashSet<>();
        this.playerNames = new HashSet<>();
    }

    public IPData(String ipAddress) {
        this.ipAddress = ipAddress;
        this.playerUUIDs = new HashSet<>();
        this.playerNames = new HashSet<>();
        this.firstSeen = Instant.now().toEpochMilli();
        this.lastSeen = firstSeen;
        this.totalLogins = 1;
        this.totalPunishments = 0;
        this.isBlacklisted = false;
    }

    public String getIpAddress() { return ipAddress; }
    public Set<UUID> getPlayerUUIDs() { return playerUUIDs; }
    public Set<String> getPlayerNames() { return playerNames; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getIsp() { return isp; }
    public void setIsp(String isp) { this.isp = isp; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public long getFirstSeen() { return firstSeen; }

    public void setFirstSeen(long firstSeen) { this.firstSeen = firstSeen; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    public int getTotalLogins() { return totalLogins; }
    public void incrementLogins() { this.totalLogins++; }
    public int getTotalPunishments() { return totalPunishments; }
    public void incrementPunishments() { this.totalPunishments++; }
    public boolean isBlacklisted() { return isBlacklisted; }
    public void setBlacklisted(boolean blacklisted) { isBlacklisted = blacklisted; }

    public void addPlayer(UUID uuid, String name) {
        playerUUIDs.add(uuid);
        playerNames.add(name);
    }

    public int getPlayerCount() {
        return playerUUIDs.size();
    }

    public String getFormattedFirstSeen() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(firstSeen));
    }

    public String getFormattedLastSeen() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(lastSeen));
    }
}
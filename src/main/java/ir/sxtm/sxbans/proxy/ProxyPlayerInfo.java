package ir.sxtm.sxbans.proxy;

import java.util.UUID;

/**
 * Information about a player on the network.
 */
public class ProxyPlayerInfo {
    private String name;
    private UUID uuid;
    private String server;
    private boolean online;
    private long lastSeen;
    private String ip;
    private String displayName;
    private String group;

    public ProxyPlayerInfo() {
        this.lastSeen = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
        if (online) {
            this.lastSeen = System.currentTimeMillis();
        }
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "ProxyPlayerInfo{" +
                "name='" + name + '\'' +
                ", uuid=" + uuid +
                ", server='" + server + '\'' +
                ", online=" + online +
                '}';
    }
}
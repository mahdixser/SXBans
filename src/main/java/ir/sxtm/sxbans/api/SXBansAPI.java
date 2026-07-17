package ir.sxtm.sxbans.api;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import ir.sxtm.sxbans.models.Punishment.PunishmentType;
import ir.sxtm.sxbans.models.HistoryEntry;
import ir.sxtm.sxbans.models.IPData;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main API class for SX Bans plugin.
 * Provides access to all plugin functionality for other plugins.
 */
public class SXBansAPI {
    private static SXBansAPI instance;
    private final SXBans plugin;
    private final PunishmentAPI punishmentAPI;
    private final WebAPI webAPI;

    public SXBansAPI(SXBans plugin) {
        this.plugin = plugin;
        this.punishmentAPI = new PunishmentAPI(plugin);
        this.webAPI = new WebAPI(plugin);
    }

    /**
     * Get the singleton instance of the API.
     *
     * @return The API instance
     */
    public static SXBansAPI getInstance() {
        if (instance == null) {
            instance = new SXBansAPI(SXBans.getInstance());
        }
        return instance;
    }

    /**
     * Get the Punishment API instance.
     *
     * @return PunishmentAPI instance
     */
    public PunishmentAPI getPunishmentAPI() {
        return punishmentAPI;
    }

    /**
     * Get the Web API instance.
     *
     * @return WebAPI instance
     */
    public WebAPI getWebAPI() {
        return webAPI;
    }

    /**
     * Check if a player is banned.
     *
     * @param uuid The UUID of the player
     * @return true if banned
     */
    public boolean isPlayerBanned(UUID uuid) {
        return plugin.getPunishmentManager().isPlayerBanned(uuid);
    }

    /**
     * Check if a player is muted.
     *
     * @param uuid The UUID of the player
     * @return true if muted
     */
    public boolean isPlayerMuted(UUID uuid) {
        return plugin.getPunishmentManager().isPlayerMuted(uuid);
    }

    /**
     * Check if an IP is banned.
     *
     * @param ip The IP address
     * @return true if banned
     */
    public boolean isIpBanned(String ip) {
        return plugin.getPunishmentManager().isIpBanned(ip);
    }

    /**
     * Check if an IP is muted.
     *
     * @param ip The IP address
     * @return true if muted
     */
    public boolean isIpMuted(String ip) {
        return plugin.getPunishmentManager().isIpMuted(ip);
    }

    /**
     * Get active ban for a player.
     *
     * @param uuid The UUID of the player
     * @return The active ban punishment, or null if none
     */
    public Punishment getActiveBan(UUID uuid) {
        return plugin.getPunishmentManager().getActiveBan(uuid);
    }

    /**
     * Get active mute for a player.
     *
     * @param uuid The UUID of the player
     * @return The active mute punishment, or null if none
     */
    public Punishment getActiveMute(UUID uuid) {
        return plugin.getPunishmentManager().getActiveMute(uuid);
    }

    /**
     * Get all punishments for a player.
     *
     * @param uuid The UUID of the player
     * @return List of punishments
     */
    public List<Punishment> getPlayerPunishments(UUID uuid) {
        return plugin.getPunishmentManager().getPlayerPunishments(uuid);
    }

    /**
     * Get warning count for a player.
     *
     * @param uuid The UUID of the player
     * @return Number of warnings
     */
    public int getWarningCount(UUID uuid) {
        return plugin.getPunishmentManager().getWarningCount(uuid);
    }

    /**
     * Get the plugin instance.
     *
     * @return The plugin instance
     */
    public SXBans getPlugin() {
        return plugin;
    }
}
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

    public static SXBansAPI getInstance() {
        if (instance == null) {
            instance = new SXBansAPI(SXBans.getInstance());
        }
        return instance;
    }

    public PunishmentAPI getPunishmentAPI() {
        return punishmentAPI;
    }

    public WebAPI getWebAPI() {
        return webAPI;
    }

    public boolean isPlayerBanned(UUID uuid) {
        return plugin.getPunishmentManager().isPlayerBanned(uuid);
    }

    public boolean isPlayerMuted(UUID uuid) {
        return plugin.getPunishmentManager().isPlayerMuted(uuid);
    }

    public boolean isIpBanned(String ip) {
        return plugin.getPunishmentManager().isIpBanned(ip);
    }

    public boolean isIpMuted(String ip) {
        return plugin.getPunishmentManager().isIpMuted(ip);
    }

    public Punishment getActiveBan(UUID uuid) {
        return plugin.getPunishmentManager().getActiveBan(uuid);
    }

    public Punishment getActiveMute(UUID uuid) {
        return plugin.getPunishmentManager().getActiveMute(uuid);
    }

    public List<Punishment> getPlayerPunishments(UUID uuid) {
        return plugin.getPunishmentManager().getPlayerPunishments(uuid);
    }

    public int getWarningCount(UUID uuid) {
        return plugin.getPunishmentManager().getWarningCount(uuid);
    }

    public SXBans getPlugin() {
        return plugin;
    }
}
package ir.sxtm.sxbans.punishment;

import ir.sxtm.sxbans.SXBans;
import ir.sxtm.sxbans.models.Punishment;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AltAccountDetector {
    private final SXBans plugin;
    private final IPTracker ipTracker;
    private final Map<UUID, Set<UUID>> altCache;
    private final Map<UUID, Long> lastDetection;

    public AltAccountDetector(SXBans plugin) {
        this.plugin = plugin;
        this.ipTracker = plugin.getIPTracker();
        this.altCache = new ConcurrentHashMap<>();
        this.lastDetection = new ConcurrentHashMap<>();
    }

    public Set<UUID> detectAlts(Player player) {
        UUID uuid = player.getUniqueId();

        if (altCache.containsKey(uuid)) {
            long last = lastDetection.getOrDefault(uuid, 0L);
            if (System.currentTimeMillis() - last < 60000) {
                return altCache.get(uuid);
            }
        }

        List<UUID> altList = ipTracker.findAltAccounts(uuid);
        Set<UUID> alts = new HashSet<>(altList);

        altCache.put(uuid, alts);
        lastDetection.put(uuid, System.currentTimeMillis());

        if (!alts.isEmpty()) {
            plugin.getSXBansLogger().info("Detected " + alts.size() + " alt accounts for " + player.getName());
        }

        return alts;
    }

    public boolean isAltAccount(UUID playerUUID, UUID potentialAltUUID) {
        Player player = plugin.getServer().getPlayer(playerUUID);
        if (player == null) return false;
        Set<UUID> alts = detectAlts(player);
        return alts != null && alts.contains(potentialAltUUID);
    }

    public Map<UUID, Set<UUID>> getAltNetwork(UUID playerUUID) {
        Map<UUID, Set<UUID>> network = new HashMap<>();
        Set<UUID> toProcess = new HashSet<>();
        toProcess.add(playerUUID);
        Set<UUID> processed = new HashSet<>();

        while (!toProcess.isEmpty()) {
            UUID current = toProcess.iterator().next();
            toProcess.remove(current);
            processed.add(current);

            List<UUID> altList = ipTracker.findAltAccounts(current);
            Set<UUID> alts = new HashSet<>(altList);
            network.put(current, alts);

            for (UUID alt : alts) {
                if (!processed.contains(alt) && !toProcess.contains(alt)) {
                    toProcess.add(alt);
                }
            }
        }

        return network;
    }

    public boolean shouldFlagAltAccount(Player player) {
        Set<UUID> alts = detectAlts(player);

        for (UUID altUUID : alts) {
            if (plugin.getPunishmentManager().isPlayerBanned(altUUID)) {
                return true;
            }
            if (plugin.getPunishmentManager().isPlayerMuted(altUUID)) {
                return true;
            }
        }

        return false;
    }

    public List<UUID> getAltAccountsWithPunishments(UUID playerUUID) {
        Player player = plugin.getServer().getPlayer(playerUUID);
        if (player == null) return Collections.emptyList();

        Set<UUID> alts = detectAlts(player);
        if (alts == null || alts.isEmpty()) return Collections.emptyList();

        return alts.stream()
                .filter(alt -> {
                    List<Punishment> punishments = plugin.getPunishmentManager().getActivePunishments(alt);
                    return !punishments.isEmpty();
                })
                .collect(Collectors.toList());
    }

    public void clearCache() {
        altCache.clear();
        lastDetection.clear();
    }

    public Map<UUID, Set<UUID>> getAltCache() {
        return altCache;
    }
}
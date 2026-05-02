package tw.sac.passingcore.effect;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlightEffectService implements Listener, Runnable {

    public static final Duration FLY_POTION_DURATION = Duration.ofMinutes(3);

    private final JavaPlugin plugin;
    private final NamespacedKey flightUntilKey;
    private final NamespacedKey originalAllowFlightKey;
    private final Map<UUID, FlightSession> sessions = new HashMap<>();

    public FlightEffectService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.flightUntilKey = new NamespacedKey(plugin, "flight_until");
        this.originalAllowFlightKey = new NamespacedKey(plugin, "flight_original_allow");
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this, 20L, 20L);
    }

    public void grant(Player player, Duration duration) {
        long expiresAtMillis = System.currentTimeMillis() + duration.toMillis();
        FlightSession previousSession = sessions.get(player.getUniqueId());
        boolean originalAllowFlight = previousSession != null
                ? previousSession.originalAllowFlight()
                : player.getAllowFlight();

        sessions.put(player.getUniqueId(), new FlightSession(expiresAtMillis, originalAllowFlight));
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(flightUntilKey, PersistentDataType.LONG, expiresAtMillis);
        data.set(originalAllowFlightKey, PersistentDataType.BYTE, originalAllowFlight ? (byte) 1 : (byte) 0);

        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage(Component.text("你獲得了 3 分鐘飛行能力。", NamedTextColor.AQUA));
        sendHud(player, expiresAtMillis);
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, FlightSession>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, FlightSession> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            FlightSession session = entry.getValue();

            if (player == null) {
                if (session.expiresAtMillis() <= now) {
                    iterator.remove();
                }
                continue;
            }

            if (session.expiresAtMillis() <= now) {
                iterator.remove();
                expire(player, session);
                continue;
            }

            sendHud(player, session.expiresAtMillis());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer data = player.getPersistentDataContainer();
        Long expiresAtMillis = data.get(flightUntilKey, PersistentDataType.LONG);
        if (expiresAtMillis == null) {
            return;
        }

        byte originalAllowFlightValue = data.getOrDefault(originalAllowFlightKey, PersistentDataType.BYTE, (byte) 0);
        FlightSession session = new FlightSession(expiresAtMillis, originalAllowFlightValue != 0);
        if (expiresAtMillis <= System.currentTimeMillis()) {
            expire(player, session);
            return;
        }

        sessions.put(player.getUniqueId(), session);
        player.setAllowFlight(true);
        sendHud(player, expiresAtMillis);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        FlightSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            clearPersistentState(player);
            return;
        }

        clearPersistentState(player);
        if (!hasNativeFlight(player) && !session.originalAllowFlight()) {
            player.setAllowFlight(false);
        }
        player.sendActionBar(Component.empty());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (!event.isFlying()) {
            return;
        }

        Player player = event.getPlayer();
        if (hasNativeFlight(player) || sessions.containsKey(player.getUniqueId())) {
            return;
        }

        Long expiresAtMillis = player.getPersistentDataContainer().get(flightUntilKey, PersistentDataType.LONG);
        if (expiresAtMillis == null || expiresAtMillis > System.currentTimeMillis()) {
            return;
        }

        event.setCancelled(true);
        clearTemporaryFlight(player);
        clearPersistentState(player);
    }

    private void expire(Player player, FlightSession session) {
        clearPersistentState(player);
        if (!hasNativeFlight(player)) {
            clearTemporaryFlight(player);
        }

        player.sendActionBar(Component.empty());
        player.sendMessage(Component.text("飛行藥水效果已結束。", NamedTextColor.GRAY));
    }

    private void clearPersistentState(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.remove(flightUntilKey);
        data.remove(originalAllowFlightKey);
    }

    private boolean hasNativeFlight(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }

    private void clearTemporaryFlight(Player player) {
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    private void sendHud(Player player, long expiresAtMillis) {
        long remainingSeconds = Math.max(0L, (expiresAtMillis - System.currentTimeMillis() + 999L) / 1000L);
        player.sendActionBar(Component.text("飛行時間" + formatTime(remainingSeconds), NamedTextColor.AQUA));
    }

    private String formatTime(long remainingSeconds) {
        long minutes = remainingSeconds / 60L;
        long seconds = remainingSeconds % 60L;
        return "%02d:%02d".formatted(minutes, seconds);
    }

    private record FlightSession(long expiresAtMillis, boolean originalAllowFlight) {
    }
}

package tw.sac.passingcore.resourcepack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResourcePackService implements Listener, AutoCloseable {

    private static final String EMBEDDED_PACK_NAME = "PassingCore-resourcepack.zip";
    private static final String DOWNLOAD_PATH = "/PassingCore-resourcepack.zip";

    private final JavaPlugin plugin;
    private HttpServer server;
    private ExecutorService executor;
    private Path resourcePackPath;
    private byte[] resourcePackHash;
    private String resourcePackHashHex;
    private UUID resourcePackId;
    private String publicUrl;
    private int port;
    private boolean required;
    private Component prompt;

    public ResourcePackService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)) {
            plugin.getLogger().info("Server resource pack is disabled in config.yml.");
            return;
        }

        try {
            extractResourcePack();
            calculateHash();
            loadConfiguration();
            startHttpServer();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start the PassingCore resource pack server", exception);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (server == null) {
            return;
        }

        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> sendResourcePack(player), 20L);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (resourcePackId == null || !resourcePackId.equals(event.getID())) {
            return;
        }

        switch (event.getStatus()) {
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> plugin.getLogger().warning(
                    "Player " + event.getPlayer().getName() + " could not load the resource pack: "
                            + event.getStatus());
            default -> {
            }
        }
    }

    private void extractResourcePack() throws IOException {
        Files.createDirectories(plugin.getDataFolder().toPath());
        resourcePackPath = plugin.getDataFolder().toPath().resolve("resourcepack.zip");
        try (InputStream resource = plugin.getResource(EMBEDDED_PACK_NAME)) {
            if (resource == null) {
                throw new IOException("Embedded resource pack is missing from the plugin jar");
            }
            Files.copy(resource, resourcePackPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void calculateHash() throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = Files.newInputStream(resourcePackPath)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, length);
                }
            }
            resourcePackHash = digest.digest();
            resourcePackHashHex = HexFormat.of().formatHex(resourcePackHash);
            resourcePackId = UUID.nameUUIDFromBytes(resourcePackHash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is not available", exception);
        }
    }

    private void loadConfiguration() {
        port = plugin.getConfig().getInt("resource-pack.port", 8123);
        publicUrl = plugin.getConfig().getString("resource-pack.public-url", "").trim();
        required = plugin.getConfig().getBoolean("resource-pack.required", true);
        String promptText = plugin.getConfig().getString(
                "resource-pack.prompt",
                "此伺服器需要 PassingCore 資源包以顯示自訂物品。");
        prompt = Component.text(promptText, NamedTextColor.AQUA);
    }

    private void startHttpServer() throws IOException {
        String bindAddress = plugin.getConfig().getString("resource-pack.bind-address", "0.0.0.0");
        server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        server.createContext(DOWNLOAD_PATH, this::handleResourcePackRequest);
        executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "PassingCore-resource-pack");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.start();
        plugin.getLogger().info("Serving resource pack on port " + port + " (SHA-1: "
                + resourcePackHashHex + ")");
    }

    private void handleResourcePackRequest(HttpExchange exchange) throws IOException {
        try (exchange) {
            String method = exchange.getRequestMethod();
            if (!method.equals("GET") && !method.equals("HEAD")) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                exchange.sendResponseHeaders(405, -1L);
                return;
            }

            long size = Files.size(resourcePackPath);
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("ETag", '"' + resourcePackHashHex + '"');
            exchange.sendResponseHeaders(200, method.equals("HEAD") ? -1L : size);
            if (method.equals("GET")) {
                try (OutputStream response = exchange.getResponseBody()) {
                    Files.copy(resourcePackPath, response);
                }
            }
        }
    }

    private void sendResourcePack(Player player) {
        if (!player.isOnline()) {
            return;
        }

        String url;
        try {
            url = createDownloadUrl(player);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Could not create resource pack URL for " + player.getName() + ": "
                    + exception.getMessage());
            return;
        }

        player.setResourcePack(resourcePackId, url, resourcePackHash, prompt, required);
    }

    private String createDownloadUrl(Player player) {
        String baseUrl = publicUrl;
        if (baseUrl.isBlank()) {
            InetSocketAddress virtualHost = player.getVirtualHost();
            if (virtualHost == null || virtualHost.getHostString().isBlank()) {
                throw new IllegalArgumentException("player virtual host is unavailable; set resource-pack.public-url");
            }
            try {
                return new URI("http", null, virtualHost.getHostString(), port, DOWNLOAD_PATH,
                        "sha1=" + resourcePackHashHex, null).toASCIIString();
            } catch (URISyntaxException exception) {
                throw new IllegalArgumentException("invalid player virtual host", exception);
            }
        }

        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "sha1=" + resourcePackHashHex;
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}

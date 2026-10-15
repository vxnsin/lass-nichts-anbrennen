package io.github.vxnsin.lna.plugin.pack;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

/** Sends the resource pack to players on join and on demand. */
public final class PackDistributor implements Listener {

    /** Stable pack id, so a resend replaces the previous version instead of stacking a second pack. */
    private static final UUID PACK_ID = UUID.nameUUIDFromBytes("lass-nichts-anbrennen:pack".getBytes(StandardCharsets.UTF_8));

    private final ResourcePack pack;
    private final Logger logger;
    private final String publicUrl;
    private final String host;
    private final int port;
    private final boolean required;
    private final Component prompt;

    public PackDistributor(ResourcePack pack, ConfigurationSection config, Logger logger) {
        this.pack = pack;
        this.logger = logger;
        this.publicUrl = config.getString("public-url", "");
        this.host = config.getString("host", "");
        this.port = config.getInt("port", 8164);
        this.required = config.getBoolean("required", true);
        this.prompt = MiniMessage.miniMessage().deserialize(config.getString("prompt", ""));
    }

    public boolean usesBuiltInServer() {
        return publicUrl.isBlank();
    }

    public int port() {
        return port;
    }

    /** Download URL including the hash as query parameter, which bypasses stale HTTP caches. */
    public String url() {
        if (!publicUrl.isBlank()) {
            return publicUrl;
        }
        return "http://" + resolveHost() + ":" + port + "/pack.zip?sha1=" + pack.sha1();
    }

    public void send(Player player) {
        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(PACK_ID, URI.create(url()), pack.sha1());
        player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .required(required)
                .replace(true)
                .prompt(prompt));
    }

    public void sendToAll() {
        Bukkit.getOnlinePlayers().forEach(this::send);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        send(event.getPlayer());
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        switch (event.getStatus()) {
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED, DECLINED ->
                    logger.warning(event.getPlayer().getName() + ": resource pack status " + event.getStatus()
                            + " (URL: " + url() + ")");
            default -> {
                // ACCEPTED, DOWNLOADED, SUCCESSFULLY_LOADED: nothing to do
            }
        }
    }

    private String resolveHost() {
        if (!host.isBlank()) {
            return host;
        }
        String serverIp = Bukkit.getIp();
        if (!serverIp.isBlank() && !serverIp.equals("0.0.0.0")) {
            return serverIp;
        }
        return lanAddress();
    }

    /** First site-local IPv4 address of an active network interface, falling back to loopback. */
    private static String lanAddress() {
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
            // fall through to loopback
        }
        return "127.0.0.1";
    }
}

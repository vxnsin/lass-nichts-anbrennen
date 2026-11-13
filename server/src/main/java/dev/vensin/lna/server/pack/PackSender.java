package dev.vensin.lna.server.pack;

import dev.vensin.lna.server.Settings;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;

public final class PackSender {

    /** A stable id makes a resend replace the previous pack instead of stacking a second one. */
    private static final UUID PACK_ID = UUID.nameUUIDFromBytes("lass-nichts-anbrennen:pack".getBytes(StandardCharsets.UTF_8));

    private final ResourcePack pack;
    private final Settings.Pack settings;

    public PackSender(ResourcePack pack, Settings.Pack settings) {
        this.pack = pack;
        this.settings = settings;
    }

    public boolean usesBuiltInServer() {
        return settings.publicUrl().isBlank();
    }

    /** The hash in the query string gets past stale HTTP caches. */
    public String url() {
        if (!usesBuiltInServer()) {
            return settings.publicUrl();
        }
        String host = settings.host().isBlank() ? lanAddress() : settings.host();
        return "http://" + host + ":" + settings.port() + "/pack.zip?sha1=" + pack.sha1();
    }

    public void send(Player player) {
        ResourcePackRequest.Builder request = ResourcePackRequest.resourcePackRequest()
                .packs(ResourcePackInfo.resourcePackInfo(PACK_ID, URI.create(url()), pack.sha1()))
                .required(settings.required())
                .replace(true);
        if (!settings.prompt().isBlank()) {
            request.prompt(MiniMessage.miniMessage().deserialize(settings.prompt()));
        }
        player.sendResourcePacks(request.build());
    }

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

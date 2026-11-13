package dev.vensin.lna.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.vensin.lna.server.command.CreditsCommand;
import dev.vensin.lna.server.command.GameCommands;
import dev.vensin.lna.server.command.Operators;
import dev.vensin.lna.server.command.PlayerCommands;
import dev.vensin.lna.server.command.ServerCommands;
import dev.vensin.lna.server.console.Ansi;
import dev.vensin.lna.server.console.ConsoleInput;
import dev.vensin.lna.server.console.StartReport;
import dev.vensin.lna.server.extinguisher.Extinguishers;
import dev.vensin.lna.server.fire.FireService;
import dev.vensin.lna.server.furniture.Furniture;
import dev.vensin.lna.server.hud.Glyphs;
import dev.vensin.lna.server.hud.Hud;
import dev.vensin.lna.server.item.Items;
import dev.vensin.lna.server.lang.Messages;
import dev.vensin.lna.server.lobby.Lobby;
import dev.vensin.lna.server.map.MapBuilder;
import dev.vensin.lna.server.map.MapDefinition;
import dev.vensin.lna.server.map.MapLoader;
import dev.vensin.lna.server.mission.MissionInfo;
import dev.vensin.lna.server.mission.Missions;
import dev.vensin.lna.server.mission.Parties;
import dev.vensin.lna.server.mission.Passes;
import dev.vensin.lna.server.pack.PackSender;
import dev.vensin.lna.server.pack.PackServer;
import dev.vensin.lna.server.pack.ResourcePack;
import dev.vensin.lna.server.world.Doors;
import dev.vensin.lna.server.world.GameWorld;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.ping.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Server {

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private static final int VOID_DEPTH = 24;

    private Server() {}

    static void run() {
        StartReport report = new StartReport(System.out);
        Properties build = buildInfo();
        report.header(build.getProperty("version", "dev"), build.getProperty("minecraft", "?"));
        try {
            start(report);
        } catch (IOException | RuntimeException e) {
            report.fail("Stopped", e.getMessage() == null ? e.toString() : e.getMessage());
            report.done();
            LOG.error("Startup failed", e);
            System.exit(1);
        }
    }

    private static void start(StartReport report) throws IOException {
        StartReport.Step engine = report.begin("Engine", "starting Minestom");
        Settings settings = Settings.load(Path.of("server.yml"));
        MinecraftServer server = MinecraftServer.init(settings.onlineMode() ? new Auth.Online() : new Auth.Offline());
        MinecraftServer.setBrandName("Lass nichts anbrennen");
        engine.ok(settings.onlineMode() ? "online mode" : "offline mode, accounts are not verified");

        StartReport.Step loading = report.begin("Content", "reading maps, items and texts");
        Content content = new Content(Path.of("content"));
        Map<String, MapDefinition> maps = new MapLoader(LOG).load(content);
        JsonObject registry = JsonParser.parseString(new String(content.read("lna/registry.json"), StandardCharsets.UTF_8))
                .getAsJsonObject();
        Items items = Items.load(registry, LOG);
        Messages messages = Messages.load(content);
        Glyphs glyphs = Glyphs.load(registry);
        Hud hud = new Hud();
        FireService fires = new FireService(messages);
        Extinguishers extinguishers = new Extinguishers(items, glyphs, hud, fires, messages);
        Map<String, MissionInfo> missions;
        try (InputStream in = content.open("missions.yml")) {
            missions = MissionInfo.load(in);
        }
        loading.ok(maps.size() + " maps " + Ansi.DOT + " " + items.all().size() + " items " + Ansi.DOT + " "
                + missions.size() + " missions");

        StartReport.Step building = report.begin("World", "building the maps");
        Furniture furniture = new Furniture(items);
        MapBuilder builder = new MapBuilder(furniture, items, messages, missions);
        GameWorld world = GameWorld.build(maps, builder);
        building.ok(String.join(", ", world.maps().keySet()) + " " + Ansi.DOT + " " + building.millis() + " ms");
        builder.warnings().forEach(LOG::warn);

        StartReport.Step packing = report.begin("Pack", "preparing the resource pack");
        ResourcePack pack = ResourcePack.of(content.read("lna/pack.zip"));
        PackSender packs = new PackSender(pack, settings.pack());
        if (packs.usesBuiltInServer()) {
            try {
                new PackServer(pack).start(settings.pack().port());
                packing.ok(packs.url());
            } catch (IOException e) {
                packing.warn("port " + settings.pack().port() + " is taken, players will not get the resource pack");
            }
        } else {
            packing.ok(packs.url());
        }

        StartReport.Step opening = report.begin("Ready", "opening port " + settings.port());
        if (!portFree(settings.port())) {
            opening.fail("port " + settings.port() + " is already in use, is another server still running?");
            report.done();
            System.exit(1);
        }
        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        listen(events, settings, world, packs);
        furniture.register(events);
        hud.register(events);
        fires.start();
        extinguishers.register(events);
        Passes passes = new Passes(Path.of("passes.yml"));
        Parties parties = new Parties();
        Missions sessions = new Missions(messages, parties, passes, missions, maps, world, furniture, fires, items);
        new Lobby(items, messages, missions, passes, maps, builder.labels()).register(events, furniture);
        sessions.register(events);
        Operators operators = new Operators(Path.of("ops.txt"));
        ServerCommands.register(MinecraftServer.getCommandManager(), operators);
        GameCommands.register(MinecraftServer.getCommandManager(), operators, items, fires, extinguishers);
        PlayerCommands.register(MinecraftServer.getCommandManager(), messages, sessions, parties);
        CreditsCommand.register(MinecraftServer.getCommandManager(), JsonParser.parseString(
                new String(content.read("lna/credits.json"), StandardCharsets.UTF_8)).getAsJsonArray(), messages);
        server.start("0.0.0.0", settings.port());
        opening.ok("port " + settings.port() + " " + Ansi.DOT + " " + report.elapsed() + " " + Ansi.DOT
                + " type help for commands");
        report.done();
        ConsoleInput.start();
    }

    private static void listen(GlobalEventHandler events, Settings settings, GameWorld world, PackSender packs) {
        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(world.instance());
            event.getPlayer().setRespawnPoint(world.spawn());
            packs.send(event.getPlayer());
        });
        events.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) {
                event.getPlayer().setGameMode(GameMode.ADVENTURE);
                LOG.info("{} {} joined {}", Ansi.green(Ansi.JOIN), event.getPlayer().getUsername(), online(0));
            }
        });
        events.addListener(PlayerDisconnectEvent.class, event ->
                LOG.info("{} {} left {}", Ansi.dim(Ansi.LEAVE), event.getPlayer().getUsername(), online(-1)));
        events.addListener(PlayerMoveEvent.class, event -> {
            if (event.getNewPosition().y() < world.floorY() - VOID_DEPTH) {
                event.setNewPosition(world.spawn());
            }
        });
        events.addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true));
        events.addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true));
        events.addListener(PlayerBlockInteractEvent.class, Doors::onInteract);
        events.addListener(ServerListPingEvent.class, event -> event.setStatus(Status.builder(event.getStatus())
                .description(MiniMessage.miniMessage().deserialize(settings.motd())).build()));
    }

    private static boolean portFree(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException taken) {
            return false;
        }
    }

    private static String online(int offset) {
        int count = MinecraftServer.getConnectionManager().getOnlinePlayers().size() + offset;
        return Ansi.dim("(" + count + " online)");
    }

    private static Properties buildInfo() {
        Properties properties = new Properties();
        try (InputStream in = Server.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException ignored) {
            // the header falls back to placeholders
        }
        return properties;
    }
}

package io.github.vxnsin.lna.plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.vxnsin.lna.plugin.command.CreditsList;
import io.github.vxnsin.lna.plugin.command.LnaCommands;
import io.github.vxnsin.lna.plugin.extinguisher.Extinguishers;
import io.github.vxnsin.lna.plugin.fire.FireService;
import io.github.vxnsin.lna.plugin.furniture.FurnitureListener;
import io.github.vxnsin.lna.plugin.furniture.FurnitureService;
import io.github.vxnsin.lna.plugin.world.WorldRules;
import io.github.vxnsin.lna.plugin.hud.Glyphs;
import io.github.vxnsin.lna.plugin.hud.HudService;
import io.github.vxnsin.lna.plugin.item.CraftingGuard;
import io.github.vxnsin.lna.plugin.item.ItemRegistry;
import io.github.vxnsin.lna.plugin.map.MapBuilder;
import io.github.vxnsin.lna.plugin.map.MapDefinition;
import io.github.vxnsin.lna.plugin.map.MapLoader;
import io.github.vxnsin.lna.plugin.map.PlacedMaps;
import java.io.File;
import java.util.Map;
import io.github.vxnsin.lna.plugin.pack.PackDistributor;
import io.github.vxnsin.lna.plugin.pack.PackServer;
import io.github.vxnsin.lna.plugin.pack.ResourcePack;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/** Plugin entry point: loads the generated registry, serves the resource pack and wires all features. */
public final class LnaPlugin extends JavaPlugin {

    private ItemRegistry items;
    private Glyphs glyphs;
    private HudService hud;
    private Extinguishers extinguishers;
    private FurnitureService furniture;
    private FireService fires;
    private Map<String, MapDefinition> maps;
    private PlacedMaps placedMaps;
    private MapBuilder mapBuilder;
    private CreditsList credits;
    private ResourcePack pack;
    private PackServer packServer;
    private PackDistributor packDistributor;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            var registry = readEmbeddedJson("lna/registry.json").getAsJsonObject();
            items = ItemRegistry.load(registry, getLogger());
            glyphs = Glyphs.load(registry);
            credits = CreditsList.load(readEmbeddedJson("lna/credits.json").getAsJsonArray());
            pack = ResourcePack.extract(this);
        } catch (IOException | RuntimeException e) {
            getLogger().log(Level.SEVERE, "Embedded content is missing or invalid. Build the plugin with Gradle.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        ConfigurationSection packConfig = getConfig().getConfigurationSection("pack");
        packDistributor = new PackDistributor(pack, packConfig, getLogger());
        if (packDistributor.usesBuiltInServer()) {
            packServer = new PackServer(pack, getLogger());
            try {
                packServer.start(packDistributor.port());
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Pack server could not bind port " + packDistributor.port()
                        + ". Players will not receive the resource pack.", e);
                packServer = null;
            }
        }

        WorldRules worldRules = new WorldRules(getConfig().getConfigurationSection("world-rules"));
        worldRules.applyToLoadedWorlds();

        hud = new HudService();
        fires = new FireService();
        fires.start(this);
        extinguishers = new Extinguishers(items, glyphs, hud, fires);
        extinguishers.start(this);
        furniture = new FurnitureService(items, (float) getConfig().getDouble("furniture.rotation-offset", 0));
        registerListeners(packDistributor, hud, extinguishers, new CraftingGuard(items), worldRules,
                new FurnitureListener(furniture));

        maps = new MapLoader(getLogger()).load(getFile(), getDataFolder());
        placedMaps = new PlacedMaps(new File(getDataFolder(), "placed-maps.yml"));
        placedMaps.load();
        mapBuilder = new MapBuilder(furniture, items, glyphs);
        LnaCommands.register(this);

        getLogger().info(String.format("Loaded %d items and %d glyphs. Resource pack %s served at %s",
                items.all().size(), glyphs.size(), pack.sha1().substring(0, 10), packDistributor.url()));
    }

    @Override
    public void onDisable() {
        if (fires != null) {
            fires.clear();
        }
        if (packServer != null) {
            packServer.stop();
        }
        if (hud != null) {
            hud.clear();
        }
    }

    /** Sends a MiniMessage-formatted message; {@code <glyph:…>} and {@code <shift:…>} are always available. */
    public void message(Audience audience, String miniMessage, TagResolver... placeholders) {
        TagResolver resolver = TagResolver.resolver(TagResolver.resolver(placeholders), glyphs.tags());
        audience.sendMessage(MiniMessage.miniMessage().deserialize(miniMessage, resolver));
    }

    public ItemRegistry items() {
        return items;
    }

    public Glyphs glyphs() {
        return glyphs;
    }

    public HudService hud() {
        return hud;
    }

    public Extinguishers extinguishers() {
        return extinguishers;
    }

    public FurnitureService furniture() {
        return furniture;
    }

    public FireService fires() {
        return fires;
    }

    public Map<String, MapDefinition> maps() {
        return maps;
    }

    public PlacedMaps placedMaps() {
        return placedMaps;
    }

    public MapBuilder mapBuilder() {
        return mapBuilder;
    }

    public CreditsList credits() {
        return credits;
    }

    public ResourcePack pack() {
        return pack;
    }

    public PackDistributor packDistributor() {
        return packDistributor;
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    private JsonElement readEmbeddedJson(String path) throws IOException {
        try (InputStream in = getResource(path)) {
            if (in == null) {
                throw new IOException(path + " is missing from the plugin jar");
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}

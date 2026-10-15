package io.github.vxnsin.lna.plugin.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

/** In-game list of all assets and their licenses, generated from CREDITS.md. */
public final class CreditsList {

    private record Entry(String title, String author, String source, String license) {}

    private final List<Entry> entries;

    private CreditsList(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    public static CreditsList load(JsonArray json) {
        List<Entry> entries = new ArrayList<>();
        for (JsonElement element : json) {
            JsonObject entry = element.getAsJsonObject();
            entries.add(new Entry(
                    entry.get("title").getAsString(),
                    entry.get("author").getAsString(),
                    entry.get("source").getAsString(),
                    entry.get("license").getAsString()));
        }
        return new CreditsList(entries);
    }

    public void send(CommandSender sender) {
        MiniMessage mini = MiniMessage.miniMessage();
        sender.sendMessage(mini.deserialize("<gold><bold>Quellen & Lizenzen</bold></gold> <gray>(" + entries.size() + " Dateien)</gray>"));
        for (Entry entry : entries) {
            Component line = mini.deserialize("<gray>•</gray> <white><title></white> <gray>von</gray> <author> <dark_gray>(<license>)</dark_gray>",
                    Placeholder.unparsed("title", entry.title()),
                    Placeholder.unparsed("author", entry.author()),
                    Placeholder.unparsed("license", entry.license()));
            if (entry.source().startsWith("http://") || entry.source().startsWith("https://")) {
                line = line.append(Component.text(" [Quelle]", NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.openUrl(entry.source())));
            }
            sender.sendMessage(line);
        }
    }
}

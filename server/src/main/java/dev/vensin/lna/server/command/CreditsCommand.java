package dev.vensin.lna.server.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.vensin.lna.server.lang.Messages;
import java.util.LinkedHashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

/** Lists who made the assets; files by the same author under the same license share one line. */
public final class CreditsCommand {

    private record Source(String author, String license, String url) {}

    private CreditsCommand() {}

    public static void register(CommandManager commands, JsonArray credits, Messages messages) {
        Map<Source, Integer> sources = new LinkedHashMap<>();
        for (JsonElement element : credits) {
            JsonObject entry = element.getAsJsonObject();
            String url = entry.get("source").getAsString();
            sources.merge(new Source(entry.get("author").getAsString(), entry.get("license").getAsString(),
                    url.startsWith("https://") || url.startsWith("http://") ? url : ""), 1, Integer::sum);
        }
        Command command = new Command("credits", "quellen");
        command.setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                ServerCommands.reply(sender, "<gray>" + credits.size() + " asset files, see CREDITS.md.</gray>");
                return;
            }
            player.sendMessage(messages.text(player, "credits.header",
                    Placeholder.unparsed("count", String.valueOf(credits.size()))));
            sources.forEach((source, count) -> {
                Component line = messages.text(player, "credits.line",
                        Placeholder.unparsed("count", String.valueOf(count)),
                        Placeholder.unparsed("author", source.author()),
                        Placeholder.unparsed("license", source.license()));
                if (!source.url().isEmpty()) {
                    line = line.append(Component.space()).append(messages.text(player, "credits.source")
                            .clickEvent(ClickEvent.openUrl(source.url())));
                }
                player.sendMessage(line);
            });
        });
        commands.register(command);
    }
}

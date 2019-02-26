package bot.groovy.helper;

import com.mewna.catnip.Catnip;
import com.mewna.catnip.CatnipOptions;
import com.mewna.catnip.cache.NoopEntityCache;
import com.mewna.catnip.shard.DiscordEvent;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {

    private static final String[] NUMBERS = {
        "zero",
        "one",
        "two",
        "three",
        "four",
        "five",
        "six",
        "seven",
        "eight",
        "nine"
    };

    // The ratelimit for button presses in milliseconds
    private static final int RATELIMIT = 30_000;

    private static final String TOKEN = System.getenv("DISCORD_TOKEN");

    private static final String GUILD_ID = System.getenv("GUILD_ID");
    private static final String REACTIONS_CHANNEL_ID = System.getenv("REACTIONS_CHANNEL_ID");
    private static final String SUPPORT_CHANNEL_ID = System.getenv("SUPPORT_CHANNEL_ID");
    private static final String SUPPORT_ROLE_ID = System.getenv("SUPPORT_ROLE_ID");

    public static void main(String[] args) throws IOException {
        var catnipOptions = new CatnipOptions(TOKEN)
            .chunkMembers(false)
            .cacheWorker(new NoopEntityCache());

        var catnip = Catnip.catnip(catnipOptions);

        var options = createOptions();

        catnip.on(DiscordEvent.GUILD_AVAILABLE, g -> {
            if(!g.id().equals(GUILD_ID))
                return;
            catnip.rest().channel().getChannelMessages(REACTIONS_CHANNEL_ID)
                .forEach(m -> {
                    catnip.rest().channel().deleteMessage(m.channelId(), m.id());
                });

            catnip.rest().channel().sendMessage(REACTIONS_CHANNEL_ID, options.getEmbed())
                .thenAccept(m -> {
                    for(String trigger : options.getTriggers()) {
                        catnip.rest().channel().addReaction(m.channelId(), m.id(), trigger);
                    }
                });
        });

        var lastReactionTimes = new HashMap<String, Map<String, Long>>();
        catnip.on(DiscordEvent.MESSAGE_REACTION_ADD, r -> {
            if(!r.channelId().equals(REACTIONS_CHANNEL_ID))
                return;

            if(!r.emoji().unicode())
                return;

            if(r.userId().equals(catnip.clientId()))
                return;

            catnip.rest().channel().deleteUserReaction(r.channelId(), r.messageId(), r.userId(), r.emoji());

            var now = System.currentTimeMillis();

            var lastUserReactionTimes = lastReactionTimes.computeIfAbsent(r.userId(), __ -> new HashMap<>());
            var last = lastUserReactionTimes.getOrDefault(r.emoji().name(), 0L);

            if(now - last < RATELIMIT)
                return;

            lastUserReactionTimes.put(r.emoji().name(), now);
            options.getConsumer(r.emoji().name()).accept(r);
        });

        catnip.connect();
    }

    private static Options createOptions() throws IOException {
        var options = new Options();

        var configStream = Main.class.getClassLoader().getResourceAsStream("options.json");
        var configString = new String(configStream.readAllBytes());
        var json = new JsonObject(configString);

        var configOptions = json.getJsonArray("options");
        for(int i = 0; i < configOptions.size(); i++) {
            var jsonObject = configOptions.getJsonObject(i);

            var content = jsonObject.getJsonArray("content").stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\n"));

            options.addOption(
                NUMBERS[i + 1],
                jsonObject.getString("title"),
                new PrivateMessageConsumer(content)
            );
        }

        var executor = Executors.newSingleThreadScheduledExecutor();
        options.addOption(
            "sos",
            "**None of the above options answer my question**",
            r -> {
                r.catnip().rest().guild().addGuildMemberRole(r.guildId(), r.userId(), SUPPORT_ROLE_ID);
                var message = "<@" + r.userId() + ">, please ask your question in <#" + SUPPORT_CHANNEL_ID + ">";
                r.catnip().rest().channel().sendMessage(r.channelId(), message)
                    .thenAccept(m -> executor.schedule((Runnable) m::delete, 30, TimeUnit.SECONDS));
            }
        );

        return options;
    }

}

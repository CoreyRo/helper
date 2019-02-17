package bot.groovy.helper;

import com.mewna.catnip.Catnip;
import com.mewna.catnip.CatnipOptions;
import com.mewna.catnip.cache.NoopEntityCache;
import com.mewna.catnip.shard.DiscordEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    // The ratelimit for button presses in milliseconds
    private static final int RATELIMIT = 30_000;

    private static final String TOKEN = System.getenv("DISCORD_TOKEN");
    private static final String USER_ID = TokenUtils.getUserId(TOKEN);

    private static final String GUILD_ID = System.getenv("GUILD_ID");
    private static final String REACTIONS_CHANNEL_ID = System.getenv("REACTIONS_CHANNEL_ID");
    private static final String SUPPORT_CHANNEL_ID = System.getenv("SUPPORT_CHANNEL_ID");
    private static final String SUPPORT_ROLE_ID = System.getenv("SUPPORT_ROLE_ID");

    public static void main(String[] args) {
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

            if(r.userId().equals(USER_ID))
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

    private static Options createOptions() {
        var options = new Options();

        options.addOption(
            "one",
            "I need help adding Groovy to my server",
            new PrivateMessageConsumer(
                "[Click here for a tutorial on inviting Groovy]" +
                "(https://groovy.zendesk.com/hc/en-us/articles/360020979532-Getting-Started)"
            )
        );

        options.addOption(
            "two",
            "Groovy's audio sounds laggy/glitchy/weird",
            new PrivateMessageConsumer(
                "[Click here for a tutorial on fixing laggy/glitchy/weird audio]" +
                "(https://groovy.zendesk.com/hc/en-us/articles/360023031772-Laggy-Glitchy-Distorted-Audio)"
            )
        );

        options.addOption(
            "three",
            "I want to lock the bot to specific text/voice channel",
            new PrivateMessageConsumer(
                "[Click here for a tutorial on locking the bot to specific channels]" +
                "(https://groovy.zendesk.com/hc/en-us/articles/360021276892-Locking-the-Bot-to-Specific-Channels)"
            )
        );

        options.addOption(
            "four",
            "I need help redeeming my Patreon rewards OR my rewards suddenly stopped working",
            new PrivateMessageConsumer(
                "To receive your rewards, you need to make sure your Discord account " +
                "is properly connected to Patreon. " +
                "[Here is a tutorial on that.](https://support.patreon.com/hc/en-us/articles/212052266-How-do-I-receive-my-Discord-role)\n\n" +
                "If you have already connected your Discord account to Patreon and it's still not working, " +
                "try disconnecting and reconnecting it."
            )
        );

        var executor = Executors.newSingleThreadScheduledExecutor();
        options.addOption(
            "sos",
            "**None of the above options answer my question**",
            r -> {
                r.catnip().rest().guild().addGuildMemberRole(r.guildId(), r.userId(), SUPPORT_ROLE_ID);
                var message = "<@" + r.userId() + ">, check out <#" + SUPPORT_CHANNEL_ID + "> for more assistance";
                r.catnip().rest().channel().sendMessage(r.channelId(), message)
                    .thenAccept(m -> executor.schedule((Runnable) m::delete, 30, TimeUnit.SECONDS));
            }
        );

        return options;
    }

}

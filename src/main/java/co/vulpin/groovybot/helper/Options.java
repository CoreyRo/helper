package co.vulpin.groovybot.helper;

import com.mewna.catnip.entity.builder.EmbedBuilder;
import com.mewna.catnip.entity.message.Embed;
import com.mewna.catnip.entity.message.ReactionUpdate;
import com.vdurmont.emoji.EmojiManager;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Options {

    private HashMap<String, Option> options = new LinkedHashMap<>();

    public Consumer<ReactionUpdate> getConsumer(String trigger) {
        return options.get(trigger).getConsumer();
    }

    public void addOption(String triggerAlias, String text, Consumer<ReactionUpdate> consumer) {
        var trigger = EmojiManager.getForAlias(triggerAlias).getUnicode();
        var option = new Option(text, consumer);
        options.put(trigger, option);
    }

    public Set<String> getTriggers() {
        return options.keySet();
    }

    public Embed getEmbed() {
        var text = options.entrySet().stream()
            .map(e -> e.getKey() + " " + e.getValue().getText())
            .collect(Collectors.joining("\n\n"));

        return new EmbedBuilder()
            .description(text)
            .footer("React below for help", null)
            .build();
    }

}

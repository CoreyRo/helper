package co.vulpin.groovybot.helper;

import com.mewna.catnip.entity.message.ReactionUpdate;

import java.util.function.Consumer;

public class Option {

    private String text;
    private Consumer<ReactionUpdate> consumer;

    public Option(String text, Consumer<ReactionUpdate> consumer) {
        this.text = text;
        this.consumer = consumer;
    }

    public String getText() {
        return text;
    }

    public Consumer<ReactionUpdate> getConsumer() {
        return consumer;
    }

}

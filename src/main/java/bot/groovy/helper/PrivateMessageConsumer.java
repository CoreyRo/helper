package bot.groovy.helper;


import com.mewna.catnip.entity.message.ReactionUpdate;

import java.util.function.Consumer;

public class PrivateMessageConsumer implements Consumer<ReactionUpdate> {

    private String message;

    PrivateMessageConsumer(String message) {
        this.message = message;
    }

    @Override
    public void accept(ReactionUpdate reactionUpdate) {
        reactionUpdate.catnip().rest().user()
            .createDM(reactionUpdate.userId())
            .thenCompose(dm -> dm.sendMessage(message));
    }

}

package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.chatcommands.ChatCommand;
import com.templeosrs.util.collections.chatcommands.CollectionLogChatCommandChatMessageSubscriber;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.QueuedMessage;

import java.util.Map;

public class ListAllHelpCommandsChatCommand extends ChatCommand {
    public ListAllHelpCommandsChatCommand()
    {
        super("!col help", "Lists all available help commands", true);
    }

    @Override
    public void handleCommand(ChatMessage event)
    {
        scheduledExecutorService.execute(() -> {
            chatMessageManager.queue(
                QueuedMessage.builder()
                    .type(ChatMessageType.GAMEMESSAGE)
                    .runeLiteFormattedMessage("Available help commands (this is only visible to you):")
                    .build()
            );

            for (Map.Entry<String, ChatCommand> helpCommandMessage : CollectionLogChatCommandChatMessageSubscriber.chatCommands.entrySet())
            {
                // Output a list of all available help commands, excluding the current one
                // (the player must already know the command to even trigger this)
                if (!helpCommandMessage.getKey().equals(this.trigger)) {
                    chatMessageManager.queue(
                        QueuedMessage.builder()
                            .type(ChatMessageType.GAMEMESSAGE)
                            .runeLiteFormattedMessage(helpCommandMessage.getKey() + ": " + helpCommandMessage.getValue().description)
                            .build()
                    );
                }
            }

            client.refreshChat();
        });
    }
}

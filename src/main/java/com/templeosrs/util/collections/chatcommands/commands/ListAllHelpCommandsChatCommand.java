package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.chatcommands.ChatCommand;
import com.templeosrs.util.collections.chatcommands.CollectionLogChatCommandChatMessageSubscriber;
import com.templeosrs.util.collections.utils.PlayerNameUtils;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.QueuedMessage;

import java.util.Map;

public class ListAllHelpCommandsChatCommand extends ChatCommand {
    public ListAllHelpCommandsChatCommand()
    {
        super("!col help", "Lists all available help commands");
    }

    @Override
    public void execute(ChatMessage event)
    {
        String localName = PlayerNameUtils.normalizePlayerName(client.getLocalPlayer().getName());
        String senderName = PlayerNameUtils.normalizePlayerName(event.getName());

        if (!senderName.equalsIgnoreCase(localName)) {
            return;
        }

        scheduledExecutorService.execute(() -> {
            chatMessageManager.queue(
                QueuedMessage.builder()
                    .type(ChatMessageType.GAMEMESSAGE)
                    .runeLiteFormattedMessage("Available help commands:")
                    .build()
            );

            for (Map.Entry<String, ChatCommand> helpCommandMessage : CollectionLogChatCommandChatMessageSubscriber.chatCommands.entrySet())
            {
                chatMessageManager.queue(
                    QueuedMessage.builder()
                        .type(ChatMessageType.GAMEMESSAGE)
                        .runeLiteFormattedMessage(helpCommandMessage.getKey() + ": " + helpCommandMessage.getValue().description)
                        .build()
                );
            }

            client.refreshChat();
        });
    }
}

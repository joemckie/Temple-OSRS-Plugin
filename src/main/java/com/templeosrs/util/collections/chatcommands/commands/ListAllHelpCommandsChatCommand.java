package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.chatcommands.ChatCommand;
import com.templeosrs.util.collections.chatcommands.CollectionLogChatCommandChatMessageSubscriber;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.util.Text;

import java.util.Map;

public class ListAllHelpCommandsChatCommand extends ChatCommand {
    public ListAllHelpCommandsChatCommand()
    {
        super("!col help", "Lists all available help commands", true);
    }

    @Override
    public void command(ChatMessage event)
    {
        clientThread.invoke(() -> {
            chatMessageManager.queue(
                QueuedMessage.builder()
                    .type(ChatMessageType.CONSOLE)
                    .runeLiteFormattedMessage("Available help commands:")
                    .build()
            );

            for (Map.Entry<String, ChatCommand> helpCommandMessage : CollectionLogChatCommandChatMessageSubscriber.chatCommands.entrySet())
            {
                // Output a list of all available help commands, excluding the current one
                // (the player must already know the command to even trigger this)
                if (!helpCommandMessage.getKey().equals(this.trigger)) {
                    chatMessageManager.queue(
                        QueuedMessage.builder()
                            .type(ChatMessageType.CONSOLE)
                            .runeLiteFormattedMessage(
                                    new ChatMessageBuilder()
                                        .append(ChatColorType.HIGHLIGHT)
                                        .append(helpCommandMessage.getKey())
                                        .append(": ")
                                        .append(ChatColorType.NORMAL)
                                        .append(Text.escapeJagex(helpCommandMessage.getValue().description))
                                        .build()
                            )
                            .build()
                    );
                }
            }

            client.refreshChat();
        });
    }
}

package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.CollectionLogCategorySlug;
import com.templeosrs.util.collections.chatcommands.ChatCommand;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.QueuedMessage;

import java.util.LinkedHashMap;
import java.util.Map;

public class ListAllCustomCategoriesChatCommand extends ChatCommand {
    public ListAllCustomCategoriesChatCommand()
    {
        super("!col list custom", "Lists all available custom categories", true);
    }

    @Override
    public void command(ChatMessage event)
    {
        clientThread.invoke(() -> {
            chatMessageManager.queue(
                QueuedMessage.builder()
                    .type(ChatMessageType.CONSOLE)
                    .runeLiteFormattedMessage(buildAvailableCategoriesMessage("custom"))
                    .build()
            );

            Map<String, CollectionLogCategorySlug> customCategories = new LinkedHashMap<>();

            customCategories.put("Gilded", CollectionLogCategorySlug.gilded);
            customCategories.put("Third age", CollectionLogCategorySlug.thirdage);

            for (Map.Entry<String, CollectionLogCategorySlug> customCategory : customCategories.entrySet()) {
                chatMessageManager.queue(
                    QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(
                            new ChatMessageBuilder()
                                .append(ChatColorType.NORMAL)
                                .append(customCategory.getKey())
                                .append(": ")
                                .append(ChatColorType.HIGHLIGHT)
                                .append(customCategory.getValue().name())
                                .build()
                        )
                        .build()
                );
            }

            client.refreshChat();
        });
    }
}

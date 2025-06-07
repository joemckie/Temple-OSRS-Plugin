package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.CollectionLogCategorySlug;
import com.templeosrs.util.collections.chatcommands.ChatCommand;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.QueuedMessage;

import java.util.LinkedList;
import java.util.List;

public class ListAllCustomCategoriesChatCommand extends ChatCommand {
    public ListAllCustomCategoriesChatCommand()
    {
        super("!col list custom", "Lists all available custom categories", true);
    }

    @Override
    public void handleCommand(ChatMessage event)
    {
        scheduledExecutorService.execute(() -> {
            chatMessageManager.queue(
                QueuedMessage.builder()
                    .type(ChatMessageType.GAMEMESSAGE)
                    .runeLiteFormattedMessage("Available custom categories (this is only visible to you):")
                    .build()
            );

            List<CollectionLogCategorySlug> customCategories = new LinkedList<>();

            customCategories.add(CollectionLogCategorySlug.gilded);
            customCategories.add(CollectionLogCategorySlug.thirdage);

            for (CollectionLogCategorySlug categorySlug : customCategories) {
                chatMessageManager.queue(
                    QueuedMessage.builder()
                        .type(ChatMessageType.GAMEMESSAGE)
                        .runeLiteFormattedMessage(categorySlug.toString())
                        .build()
                );
            }

            client.refreshChat();
        });
    }
}

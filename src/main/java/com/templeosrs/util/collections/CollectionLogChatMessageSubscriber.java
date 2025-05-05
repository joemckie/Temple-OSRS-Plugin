package com.templeosrs.util.collections;

import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.client.util.Text.removeTags;

public class CollectionLogChatMessageSubscriber
{
    private final Pattern NEW_COLLECTION_LOG_ITEM_PATTERN = Pattern.compile("New item added to your collection log: (.*)");

    @Inject
    private CollectionLogManager collectionLogManager;

    @Inject
    private EventBus eventBus;

    public void startUp()
    {
        eventBus.register(this);
    }

    public void shutDown()
    {
        eventBus.unregister(this);
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage)
    {
        if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        Matcher matcher = NEW_COLLECTION_LOG_ITEM_PATTERN.matcher(chatMessage.getMessage());

        if (matcher.matches()) {
            String itemName = removeTags(matcher.group(1));

            System.out.println(collectionLogManager);

            collectionLogManager.obtainedItemNames.add(itemName);
        }
    }
}

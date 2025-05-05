package com.templeosrs;

import com.google.inject.testing.fieldbinder.Bind;
import com.templeosrs.util.collections.CollectionLogManager;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CollectionLogManagerTest
{
    @Bind
    protected final CollectionLogManager collectionLogManager = spy(CollectionLogManager.class);

    final String newCollectionLogItem = "Twisted bow";
    final String newCollectionLogItemMessage = String.format("New item added to your collection log: %s", newCollectionLogItem);

    @Test()
    void doesNotTriggerForNonGameMessageMessages()
    {
        ChatMessage chatMessageEvent = new ChatMessage(
                null,
                ChatMessageType.CLAN_MESSAGE,
                "",
                newCollectionLogItemMessage,
                "",
                0
        );

        collectionLogManager.onChatMessage(chatMessageEvent);

        assertEquals(new HashSet<>(), collectionLogManager.getObtainedItemNames());
    }
}

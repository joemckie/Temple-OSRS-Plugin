package com.templeosrs.collectionlog.autosync;

import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CollectionLogAutoSyncChatMessageSubscriberTest extends MockedCollectionLogAutoSyncTest
{
    final String newCollectionLogItem = "Twisted bow";
    final String newCollectionLogItemMessage = String.format("New item added to your collection log: %s", newCollectionLogItem);

    @Test
    @DisplayName("Ensure obtained items are not modified for messages that are not ChatMessageType.GAMEMESSAGE")
    void doesNotAddToObtainedItemsForNonGameMessageMessages()
    {
        ChatMessage chatMessage = buildChatMessage(ChatMessageType.CLAN_MESSAGE, newCollectionLogItemMessage);

        eventBus.post(chatMessage);

        assertEquals(new HashSet<>(), collectionLogAutoSyncManager.getObtainedItemNames());
    }

    @Test
    @DisplayName("Ensure obtained items are not modified for messages that do not match the new collection log item pattern")
    void doesNotAddToObtainedItemsForNonMatchingGameMessages()
    {
        ChatMessage chatMessage = buildChatMessage(ChatMessageType.GAMEMESSAGE, "Some message");

        eventBus.post(chatMessage);

        assertEquals(new HashSet<>(), collectionLogAutoSyncManager.getObtainedItemNames());
    }

    @Test
    @DisplayName("Ensure obtained items are modified for game messages that match the new collection log item pattern")
    void addsToObtainedItemsForMatchingGameMessages()
    {
        triggerNewCollectionLogItemChatMessageEvent(newCollectionLogItem);

        HashSet<String> expectedObtainedItems = new HashSet<>();

        expectedObtainedItems.add(newCollectionLogItem);

        assertEquals(expectedObtainedItems, collectionLogAutoSyncManager.getObtainedItemNames());
    }
}

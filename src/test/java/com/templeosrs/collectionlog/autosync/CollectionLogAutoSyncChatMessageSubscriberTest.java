package com.templeosrs.collectionlog.autosync;

import com.google.inject.testing.fieldbinder.Bind;
import com.templeosrs.MockedTest;
import com.templeosrs.util.collections.autosync.CollectionLogAutoSyncChatMessageSubscriber;
import com.templeosrs.util.collections.autosync.CollectionLogAutoSyncManager;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CollectionLogAutoSyncChatMessageSubscriberTest extends MockedTest
{
    @Bind
    protected final CollectionLogAutoSyncManager collectionLogAutoSyncManager = spy(CollectionLogAutoSyncManager.class);

    @Bind
    protected final CollectionLogAutoSyncChatMessageSubscriber collectionLogAutoSyncChatMessageSubscriber = spy(CollectionLogAutoSyncChatMessageSubscriber.class);

    final String newCollectionLogItem = "Twisted bow";
    final String newCollectionLogItemMessage = String.format("New item added to your collection log: %s", newCollectionLogItem);
    
    @BeforeEach
    void registerWithEventBus()
    {
        collectionLogAutoSyncManager.startUp();
        collectionLogAutoSyncChatMessageSubscriber.startUp();
    }
    
    @AfterEach
    void unregisterWithEventBus()
    {
        collectionLogAutoSyncManager.shutDown();
        collectionLogAutoSyncChatMessageSubscriber.shutDown();
    }

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
        ChatMessage chatMessage = buildChatMessage(ChatMessageType.GAMEMESSAGE, newCollectionLogItemMessage);

        eventBus.post(chatMessage);

        HashSet<String> expectedObtainedItems = new HashSet<>();

        expectedObtainedItems.add(newCollectionLogItem);

        assertEquals(expectedObtainedItems, collectionLogAutoSyncManager.getObtainedItemNames());
    }

    private ChatMessage buildChatMessage(ChatMessageType type, String message)
    {
        return new ChatMessage(null, type, "", message, "", 0);
    }
}

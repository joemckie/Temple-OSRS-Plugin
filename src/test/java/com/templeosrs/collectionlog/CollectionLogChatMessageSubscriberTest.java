package com.templeosrs.collectionlog;

import com.google.inject.testing.fieldbinder.Bind;
import com.templeosrs.MockedTest;
import com.templeosrs.util.collections.CollectionLogChatMessageSubscriber;
import com.templeosrs.util.collections.CollectionLogManager;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CollectionLogChatMessageSubscriberTest extends MockedTest
{
    @Bind
    protected final CollectionLogManager collectionLogManager = spy(CollectionLogManager.class);

    @Bind
    protected final CollectionLogChatMessageSubscriber collectionLogChatMessageSubscriber = spy(CollectionLogChatMessageSubscriber.class);

    final String newCollectionLogItem = "Twisted bow";
    final String newCollectionLogItemMessage = String.format("New item added to your collection log: %s", newCollectionLogItem);

    @Test
    @DisplayName("Ensure obtained items are not modified for messages that are not ChatMessageType.GAMEMESSAGE")
    void doesNotAddToObtainedItemsForNonGameMessageMessages()
    {
        ChatMessage chatMessage = buildChatMessage(ChatMessageType.CLAN_MESSAGE, newCollectionLogItemMessage);

        collectionLogChatMessageSubscriber.onChatMessage(chatMessage);

        assertEquals(new HashSet<>(), collectionLogManager.getObtainedItemNames());
    }

    @Test
    @DisplayName("Ensure obtained items are not modified for messages that do not match the new collection log item pattern")
    void doesNotAddToObtainedItemsForNonMatchingGameMessages()
    {
        ChatMessage chatMessage = buildChatMessage(ChatMessageType.GAMEMESSAGE, "Some message");

        collectionLogChatMessageSubscriber.onChatMessage(chatMessage);

        assertEquals(new HashSet<>(), collectionLogManager.getObtainedItemNames());
    }

    @Test
    @DisplayName("Ensure obtained items are modified for game messages that match the new collection log item pattern")
    void addsToObtainedItemsForMatchingGameMessages()
    {
        ChatMessage chatMessage = buildChatMessage(ChatMessageType.GAMEMESSAGE, newCollectionLogItemMessage);

        collectionLogChatMessageSubscriber.onChatMessage(chatMessage);

        HashSet<String> expectedObtainedItems = new HashSet<>();

        expectedObtainedItems.add(newCollectionLogItem);

        assertEquals(expectedObtainedItems, collectionLogManager.getObtainedItemNames());
    }

    private ChatMessage buildChatMessage(ChatMessageType type, String message)
    {
        return new ChatMessage(null, type, "", message, "", 0);
    }
}

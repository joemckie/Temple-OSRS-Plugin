package com.templeosrs.collectionlog.autosync;

import com.google.inject.testing.fieldbinder.Bind;
import com.templeosrs.MockedTest;
import com.templeosrs.util.collections.autosync.CollectionLogAutoSyncManager;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Item;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MockedCollectionLogAutoSyncTest extends MockedTest {
    @Bind
    protected final CollectionLogAutoSyncManager collectionLogAutoSyncManager = spy(CollectionLogAutoSyncManager.class);

    @BeforeEach
    void registerWithEventBus()
    {
        collectionLogAutoSyncManager.startUp();
    }

    @AfterEach
    void unregisterWithEventBus()
    {
        collectionLogAutoSyncManager.shutDown();
    }

    void triggerNewCollectionLogItemChatMessageEvent(String itemName)
    {
        final ChatMessage chatMessage = buildChatMessage(
                ChatMessageType.GAMEMESSAGE,
                String.format("New item added to your collection log: %s", itemName)
        );

        eventBus.post(chatMessage);
    }

    NpcLootReceived buildNpcLootReceivedEvent(ItemStack[] itemStacks)
    {
        for (ItemStack value : itemStacks) {
            when(itemManager.getItemComposition(value.getId())).thenReturn(itemComposition);
        }

        return new NpcLootReceived(npc, Arrays.asList(itemStacks));
    }

    ChatMessage buildChatMessage(ChatMessageType type, String message)
    {
        return new ChatMessage(null, type, "", message, "", 0);
    }

    ItemContainerChanged buildItemContainerChangedEvent(int inventoryID, Item[] items)
    {
        for (Item value : items) {
            when(itemManager.getItemComposition(value.getId())).thenReturn(itemComposition);
        }

        when(itemContainer.getItems()).thenReturn(items);

        return new ItemContainerChanged(inventoryID, itemContainer);
    }
}

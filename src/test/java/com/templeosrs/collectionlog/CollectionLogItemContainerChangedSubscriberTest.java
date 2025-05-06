package com.templeosrs.collectionlog;

import com.google.inject.testing.fieldbinder.Bind;
import com.templeosrs.MockedTest;
import com.templeosrs.util.collections.CollectionLogChatMessageSubscriber;
import com.templeosrs.util.collections.CollectionLogItemContainerChangedSubscriber;
import com.templeosrs.util.collections.CollectionLogManager;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Item;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CollectionLogItemContainerChangedSubscriberTest extends MockedTest
{
    @Bind
    protected final CollectionLogManager collectionLogManager = spy(CollectionLogManager.class);

    @Bind
    protected final CollectionLogItemContainerChangedSubscriber collectionLogItemContainerChangedSubscriber = spy(CollectionLogItemContainerChangedSubscriber.class);

    @Bind
    protected final CollectionLogChatMessageSubscriber collectionLogChatMessageSubscriber = spy(CollectionLogChatMessageSubscriber.class);

    @BeforeEach
    void triggerChatMessageEvent()
    {
        final ChatMessage chatMessage = new ChatMessage(
                null,
                ChatMessageType.GAMEMESSAGE,
                "",
                "New item added to your collection log: Twisted bow",
                "",
                0
        );

        collectionLogChatMessageSubscriber.onChatMessage(chatMessage);
    }

    @Test
    @DisplayName("Ensure the bitset is not modified when the changed container is not the inventory")
    void doesNotAddToBitSetForNonInventoryContainers()
    {
        ItemContainerChanged itemContainerChanged = buildItemContainerChangedEvent(
                InventoryID.DREAM_BANK_INVENTORY,
                new Item[]{}
        );

        collectionLogItemContainerChangedSubscriber.onItemContainerChanged(itemContainerChanged);

        assertEquals(new BitSet(), collectionLogManager.getClogItemsBitSet());
    }

    @Test
    @DisplayName("Ensure the bitset is not modified when no inventory items are found in the obtained items list")
    void doesNotAddToBitSetWhenNoInventoryItemsMatchObtainedItems()
    {
        final Item[] mockItems = {
                new Item(ItemID.BONES, 1)
        };

        ItemContainerChanged itemContainerChanged = buildItemContainerChangedEvent(
                InventoryID.INV,
                mockItems
        );

        collectionLogItemContainerChangedSubscriber.onItemContainerChanged(itemContainerChanged);

        assertEquals(new BitSet(), collectionLogManager.getClogItemsBitSet());
    }

    @Test
    @DisplayName("Ensure the bitset is modified when inventory items are found in the obtained items list")
    void addsToBitSetWhenInventoryItemsMatchObtainedItems()
    {
        final Item[] mockItems = {
                new Item(ItemID.TWISTED_BOW, 1)
        };

        ItemContainerChanged itemContainerChanged = buildItemContainerChangedEvent(
                InventoryID.INV,
                mockItems
        );

        final int expectedIndex = 1;

        collectionLogManager.getCollectionLogItemIdToBitsetIndex().put(ItemID.TWISTED_BOW, expectedIndex);

        when(itemComposition.getName()).thenReturn("Twisted bow");
        when(itemManager.getItemComposition(ItemID.TWISTED_BOW)).thenReturn(itemComposition);

        collectionLogItemContainerChangedSubscriber.onItemContainerChanged(itemContainerChanged);

        BitSet expectedBitset = new BitSet();

        expectedBitset.set(expectedIndex);

        assertEquals(expectedBitset, collectionLogManager.getClogItemsBitSet());
    }

    private ItemContainerChanged buildItemContainerChangedEvent(int inventoryID, Item[] items)
    {
        when(itemContainer.getItems()).thenReturn(items);
        when(itemManager.getItemComposition(ItemID.BONES)).thenReturn(itemComposition);

        return new ItemContainerChanged(inventoryID, itemContainer);
    }
}

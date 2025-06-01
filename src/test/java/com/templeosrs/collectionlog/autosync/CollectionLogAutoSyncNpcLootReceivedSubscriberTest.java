package com.templeosrs.collectionlog.autosync;

import com.templeosrs.util.collections.data.ObtainedCollectionItem;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

public class CollectionLogAutoSyncNpcLootReceivedSubscriberTest extends MockedCollectionLogAutoSyncTest
{
    @BeforeEach
    // In-game, the chat message triggers before the loot is received, so emulate this in the tests
    void setupChatMessageEventAndTrigger()
    {
        triggerNewCollectionLogItemChatMessageEvent("Twisted bow");
    }
    
    @Test
    @DisplayName("Ensure the pending sync items list is not modified when the obtained items list is empty")
    void doesNotAddToPendingItemsWhenObtainedItemsListIsEmpty()
    {
        collectionLogAutoSyncManager.getObtainedItemNames().clear();

        NpcLootReceived npcLootReceived = buildNpcLootReceivedEvent(
                new ItemStack[]{}
        );

        eventBus.post(npcLootReceived);

        assertEquals(new HashSet<>(), collectionLogAutoSyncManager.getPendingSyncItems());
    }

    @Test
    @DisplayName("Ensure the sync countdown timer is not started when the obtained items list is empty")
    void doesNotStartSyncCountdownWhenObtainedItemsListIsEmpty()
    {
        NpcLootReceived npcLootReceived = buildNpcLootReceivedEvent(
                new ItemStack[]{}
        );

        eventBus.post(npcLootReceived);

        assertNull(collectionLogAutoSyncManager.getGameTickToSync());
    }

    @Test
    @DisplayName("Ensure the pending sync items list is not modified when the loot received is not found in the obtained items list")
    void doesNotAddToPendingItemsWhenLootIsNotInObtainedItemsList()
    {
        NpcLootReceived npcLootReceived = buildNpcLootReceivedEvent(
                new ItemStack[]{
                        new ItemStack(ItemID.BONES, 1)
                }
        );

        eventBus.post(npcLootReceived);

        assertEquals(new HashSet<>(), collectionLogAutoSyncManager.getPendingSyncItems());
    }

    @Test
    @DisplayName("Ensure the sync countdown timer is not started when the loot received is not found in the obtained items list")
    void doesNotStartSyncCountdownWhenLootIsNotInObtainedItemsList()
    {
        NpcLootReceived npcLootReceived = buildNpcLootReceivedEvent(
                new ItemStack[]{
                        new ItemStack(ItemID.BONES, 1)
                }
        );

        eventBus.post(npcLootReceived);

        assertNull(collectionLogAutoSyncManager.getGameTickToSync());
    }

    @Test
    @DisplayName("Ensure the pending sync items list is modified when the loot received is found in the obtained items list")
    void addsToPendingItemsWhenLootIsInObtainedItemsList()
    {
        when(itemComposition.getName()).thenReturn("Twisted bow");

        NpcLootReceived npcLootReceived = buildNpcLootReceivedEvent(
                new ItemStack[]{
                        new ItemStack(ItemID.TWISTED_BOW, 42)
                }
        );

        eventBus.post(npcLootReceived);

        final HashSet<ObtainedCollectionItem> expectedHashSet = new HashSet<>();
        expectedHashSet.add(new ObtainedCollectionItem(ItemID.TWISTED_BOW, "Twisted bow", 42));

        assertEquals(expectedHashSet, collectionLogAutoSyncManager.getPendingSyncItems());
    }

    @Test
    @DisplayName("Ensure the sync countdown timer is started when the loot received is found in the obtained items list")
    void startsSyncCountdownTimerWhenLootIsInObtainedItemsList()
    {
        when(itemComposition.getName()).thenReturn("Twisted bow");
        when(client.getTickCount()).thenReturn(100);

        NpcLootReceived npcLootReceived = buildNpcLootReceivedEvent(
                new ItemStack[]{
                        new ItemStack(ItemID.TWISTED_BOW, 1)
                }
        );

        eventBus.post(npcLootReceived);

        assertEquals(117, collectionLogAutoSyncManager.getGameTickToSync());
    }
}

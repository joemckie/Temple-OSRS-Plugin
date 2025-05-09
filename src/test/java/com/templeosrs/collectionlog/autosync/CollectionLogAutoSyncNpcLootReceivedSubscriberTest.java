package com.templeosrs.collectionlog.autosync;

import com.google.inject.testing.fieldbinder.Bind;
import com.templeosrs.MockedTest;
import com.templeosrs.util.collections.autosync.CollectionLogAutoSyncChatMessageSubscriber;
import com.templeosrs.util.collections.autosync.CollectionLogAutoSyncManager;
import com.templeosrs.util.collections.autosync.CollectionLogAutoSyncNpcLootReceivedSubscriber;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class CollectionLogAutoSyncNpcLootReceivedSubscriberTest extends MockedTest
{
    @Bind
    protected final CollectionLogAutoSyncManager collectionLogAutoSyncManager = spy(CollectionLogAutoSyncManager.class);

    @Bind
    protected final CollectionLogAutoSyncNpcLootReceivedSubscriber collectionLogAutoSyncNpcLootReceivedSubscriber = spy(CollectionLogAutoSyncNpcLootReceivedSubscriber.class);

    @Bind
    protected final CollectionLogAutoSyncChatMessageSubscriber collectionLogAutoSyncChatMessageSubscriber = spy(CollectionLogAutoSyncChatMessageSubscriber.class);

    @BeforeEach
    // In-game, the chat message triggers before the loot is received, so emulate this in the tests
    void setupChatMessageEventAndTrigger()
    {
        triggerChatMessageEvent("Twisted bow");
    }
    
    @BeforeEach
    void registerWithEventBus()
    {
        collectionLogAutoSyncChatMessageSubscriber.startUp();
        collectionLogAutoSyncNpcLootReceivedSubscriber.startUp();
    }

    @AfterEach
    void unregisterWithEventBus()
    {
        collectionLogAutoSyncChatMessageSubscriber.shutDown();
        collectionLogAutoSyncNpcLootReceivedSubscriber.shutDown();
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
                        new ItemStack(ItemID.TWISTED_BOW, 1)
                }
        );

        eventBus.post(npcLootReceived);

        final HashSet<Integer> expectedHashSet = new HashSet<>();
        expectedHashSet.add(ItemID.TWISTED_BOW);

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

    private void triggerChatMessageEvent(String itemName)
    {
        final ChatMessage chatMessage = new ChatMessage(
                null,
                ChatMessageType.GAMEMESSAGE,
                "",
                String.format("New item added to your collection log: %s", itemName),
                "",
                0
        );

        eventBus.post(chatMessage);
    }

    private NpcLootReceived buildNpcLootReceivedEvent(ItemStack[] itemStacks)
    {
        for (ItemStack value : itemStacks) {
            when(itemManager.getItemComposition(value.getId())).thenReturn(itemComposition);
        }

        return new NpcLootReceived(npc, Arrays.asList(itemStacks));
    }
}

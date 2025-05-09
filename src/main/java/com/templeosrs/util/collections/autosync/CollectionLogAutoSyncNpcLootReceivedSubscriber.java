package com.templeosrs.util.collections.autosync;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;

@Slf4j
public class CollectionLogAutoSyncNpcLootReceivedSubscriber
{
    @Inject
    private CollectionLogAutoSyncManager collectionLogAutoSyncManager;

    @Inject
    private EventBus eventBus;

    @Inject
    private ItemManager itemManager;

    public void startUp()
    {
        eventBus.register(this);
    }

    public void shutDown()
    {
        eventBus.unregister(this);
    }

    /**
     * This method is called when an NPC loot event is received.
     * 
     * If any items in the loot match the newly obtained collection log item names,
     * they are added to a list of items awaiting a server sync and the sync countdown is started.
     */
    @Subscribe
    private void onNpcLootReceived(NpcLootReceived npcLootReceived)
    {
        if (collectionLogAutoSyncManager.obtainedItemNames.isEmpty()) {
            return;
        }

        boolean isNewCollectionLogFound = false;

        npcLootReceived.getItems().forEach(item -> {
            final int itemId = item.getId();
            final String itemName = itemManager.getItemComposition(itemId).getName();

            if (collectionLogAutoSyncManager.obtainedItemNames.contains(itemName)) {
                collectionLogAutoSyncManager.pendingSyncItems.add(itemId);
                collectionLogAutoSyncManager.obtainedItemNames.remove(itemName);

                isNewCollectionLogFound = true;
            }
        });

        if (isNewCollectionLogFound) {
            collectionLogAutoSyncManager.startSyncCountdown();
        }
    }
}

package com.templeosrs.util.collections.autosync;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class CollectionLogAutoSyncNpcLootReceivedSubscriber
{
    @Inject
    private CollectionLogAutoSyncManager collectionLogAutoSyncManager;

    @Inject
    private ItemManager itemManager;

    /**
     * This method is called when an NPC loot event is received.
     * If any items in the loot match the newly obtained collection log item names,
     * they are added to a list of items awaiting a server sync and the sync countdown is started.
     */
    @Subscribe
    private void onNpcLootReceived(NpcLootReceived npcLootReceived)
    {
        if (collectionLogAutoSyncManager.obtainedItemNames.isEmpty()) {
            return;
        }

        AtomicBoolean isNewCollectionLogFound = new AtomicBoolean(false);

        npcLootReceived.getItems().forEach(item -> {
            final int itemId = item.getId();
            final String itemName = itemManager.getItemComposition(itemId).getName();

            if (collectionLogAutoSyncManager.obtainedItemNames.contains(itemName)) {
                collectionLogAutoSyncManager.pendingSyncItems.add(itemId);
                collectionLogAutoSyncManager.obtainedItemNames.remove(itemName);

                isNewCollectionLogFound.set(true);
            }
        });

        if (isNewCollectionLogFound.get()) {
            collectionLogAutoSyncManager.startSyncCountdown();
        }
    }
}

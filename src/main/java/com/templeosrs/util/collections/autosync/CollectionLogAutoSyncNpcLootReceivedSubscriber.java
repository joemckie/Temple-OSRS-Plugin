package com.templeosrs.util.collections.autosync;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.client.util.Text.removeTags;

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

    @Subscribe
    private void onNpcLootReceived(NpcLootReceived npcLootReceived)
    {
        if (collectionLogAutoSyncManager.obtainedItemNames.isEmpty()) {
            return;
        }

        npcLootReceived.getItems().forEach(item -> {
            final int itemId = item.getId();
            final String itemName = itemManager.getItemComposition(itemId).getName();

            if (collectionLogAutoSyncManager.obtainedItemNames.contains(itemName)) {
                collectionLogAutoSyncManager.pendingSyncItems.add(itemId);
                collectionLogAutoSyncManager.obtainedItemNames.remove(itemName);
            }
        });

        collectionLogAutoSyncManager.startSyncCountdown();
    }
}

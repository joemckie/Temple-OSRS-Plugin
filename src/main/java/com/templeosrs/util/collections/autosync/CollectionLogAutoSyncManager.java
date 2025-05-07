package com.templeosrs.util.collections.autosync;

import lombok.Getter;

import javax.inject.Inject;
import java.util.HashSet;

public class CollectionLogAutoSyncManager {
    @Inject
    private CollectionLogAutoSyncChatMessageSubscriber collectionLogAutoSyncChatMessageSubscriber;

    @Inject
    private CollectionLogAutoSyncItemContainerChangedSubscriber collectionLogAutoSyncItemContainerChangedSubscriber;

    @Getter
    protected final HashSet<String> obtainedItemNames = new HashSet<>();

    /**
     * Keeps track of what item IDs are pending a server sync
     */
    @Getter
    protected final HashSet<Integer> pendingSyncItems = new HashSet<>();

    public void startUp()
    {
        collectionLogAutoSyncChatMessageSubscriber.startUp();
        collectionLogAutoSyncItemContainerChangedSubscriber.startUp();
    }

    public void shutDown()
    {
        collectionLogAutoSyncChatMessageSubscriber.shutDown();
        collectionLogAutoSyncItemContainerChangedSubscriber.shutDown();
    }
}

package com.templeosrs;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import net.runelite.api.*;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class MockedTest {
    @Bind
    protected final Client client = mock(Client.class);

    @Bind
    protected final RuneLiteConfig runeLiteConfig = mock(RuneLiteConfig.class);

    @Bind
    protected final ItemManager itemManager = mock(ItemManager.class);

    @Bind
    protected final ItemContainer itemContainer = mock(ItemContainer.class);

    @Bind
    protected final ItemComposition itemComposition = mock(ItemComposition.class);

    @Bind
    protected final Item item = mock(Item.class);

    @Bind
    protected final TempleOSRSConfig templeOSRSConfig = mock(TempleOSRSConfig.class);

    @Bind
    protected final ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
    
    @Bind
    protected final EventBus eventBus = spy(EventBus.class);

    @Bind
    protected final NPC npc = mock(NPC.class);

    @BeforeEach
    protected void beforeEach()
    {
        Guice.createInjector(BoundFieldModule.of(this));
    }
}

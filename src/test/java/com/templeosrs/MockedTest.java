package com.templeosrs;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import net.runelite.api.Client;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.game.ItemManager;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Mockito.mock;

public class MockedTest {
    @Bind
    protected Client client = mock(Client.class);

    @Bind
    protected RuneLiteConfig runeLiteConfig = mock(RuneLiteConfig.class);

    @Bind
    protected ItemManager itemManager = mock(ItemManager.class);

    @Bind
    protected TempleOSRSConfig templeOSRSConfig = mock(TempleOSRSConfig.class);

    @Bind
    protected ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);

    @BeforeEach
    protected void beforeEach()
    {
        Guice.createInjector(BoundFieldModule.of(this));
    }
}

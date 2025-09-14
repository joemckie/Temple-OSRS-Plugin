package com.templeosrs;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import com.templeosrs.util.collections.autosync.LoggedInState;
import java.util.concurrent.ScheduledExecutorService;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.xpupdater.XpUpdaterConfig;
import net.runelite.client.ui.ClientUI;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class MockedTest
{
	@Bind
	protected final Client client = spy(Client.class);

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
	protected final ScheduledExecutorService scheduledExecutorService = spy(ScheduledExecutorService.class);

	@Bind
	protected final EventBus eventBus = spy(EventBus.class);

	@Bind
	protected final NPC npc = mock(NPC.class);

	@Bind
	protected final Player player = mock(Player.class);

	@Bind
	protected final ConfigManager configManager = mock(ConfigManager.class);

	@Bind
	protected final PluginManager pluginManager = mock(PluginManager.class);

	@Bind
	protected final ClientUI clientUI = mock(ClientUI.class);

	@Bind
	protected final ChatMessageManager chatMessageManager = mock(ChatMessageManager.class);

	@Bind
	protected final XpUpdaterConfig xpUpdaterConfig = mock(XpUpdaterConfig.class);

	@Bind
	protected final GameStateChanged gameStateChanged = mock(GameStateChanged.class);

	@Bind
	protected final LoggedInState loggedInState = spy(LoggedInState.class);

	@BeforeEach
	protected void beforeEach()
	{
		Guice.createInjector(BoundFieldModule.of(this));
	}
}

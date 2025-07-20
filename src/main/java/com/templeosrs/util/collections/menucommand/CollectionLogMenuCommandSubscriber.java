package com.templeosrs.util.collections.menucommand;

import com.templeosrs.util.collections.CollectionLogRequestManager;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Provider;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.util.Text;

@Slf4j
public class CollectionLogMenuCommandSubscriber
{
	@Inject
	private Client client;

	@Inject
	private EventBus eventBus;

	@Inject
	private Provider<MenuManager> menuManager;

	@Inject
	private CollectionLogRequestManager collectionLogRequestManager;

	@Inject
	private ScheduledExecutorService scheduledExecutorService;

	private final String MENU_OPTION_NAME = "Collection log";

	public void startUp()
	{
		eventBus.register(this);

		menuManager.get().addPlayerMenuItem(MENU_OPTION_NAME);
	}

	public void shutDown()
	{
		eventBus.unregister(this);

		menuManager.get().removePlayerMenuItem(MENU_OPTION_NAME);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if ((event.getType() != MenuAction.CC_OP.getId() && event.getType() != MenuAction.CC_OP_LOW_PRIORITY.getId()))
		{
			return;
		}

		final String option = event.getOption();
		final int componentId = event.getActionParam1();
		final int groupId = WidgetUtil.componentToInterface(componentId);

		if (groupId == InterfaceID.FRIENDS && option.equals("Delete")
			|| groupId == InterfaceID.CHATCHANNEL_CURRENT && (option.equals("Add ignore") || option.equals("Remove friend"))
			|| groupId == InterfaceID.CHATBOX && (option.equals("Add ignore") || option.equals("Message"))
			|| groupId == InterfaceID.IGNORE && option.equals("Delete")
			|| (componentId == InterfaceID.ClansSidepanel.PLAYERLIST || componentId == InterfaceID.ClansGuestSidepanel.PLAYERLIST) && (option.equals("Add ignore") || option.equals("Remove friend"))
			|| groupId == InterfaceID.PM_CHAT && (option.equals("Add ignore") || option.equals("Message"))
			|| groupId == InterfaceID.GIM_SIDEPANEL && (option.equals("Add friend") || option.equals("Remove friend") || option.equals("Remove ignore"))
		)
		{
			client.getMenu().createMenuEntry(-2)
				.setOption(MENU_OPTION_NAME)
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.setIdentifier(event.getIdentifier())
				.onClick(e ->
				{
					String target = Text.removeTags(e.getTarget()).toLowerCase();

					scheduledExecutorService.execute(() ->
					{
						String json = collectionLogRequestManager.getPlayerCollectionLog(target);

						log.debug(json);
					});
				});
		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() == MenuAction.RUNELITE_PLAYER && event.getMenuOption().equals(MENU_OPTION_NAME))
		{
			Player player = event.getMenuEntry().getPlayer();

			if (player == null)
			{
				return;
			}

			String target = player.getName();

			if (target == null)
			{
				return;
			}

			scheduledExecutorService.execute(() ->
			{
				String normalizedPlayerName = target.toLowerCase();
				String json = collectionLogRequestManager.getPlayerCollectionLog(normalizedPlayerName);

				log.debug(json);
			});
		}
	}
}

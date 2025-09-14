package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.CollectionLogCategoryGroup;
import com.templeosrs.util.collections.chatcommands.ChatCommand;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ChatMessage;

@Slf4j
public class ListAllBossCategoriesChatCommand extends ChatCommand
{
	public ListAllBossCategoriesChatCommand()
	{
		super("!col list bosses", "Lists all available boss categories", true);
	}

	@Override
	public void command(ChatMessage event)
	{
		listAvailableCollectionLogCategories(CollectionLogCategoryGroup.bosses);
	}
}

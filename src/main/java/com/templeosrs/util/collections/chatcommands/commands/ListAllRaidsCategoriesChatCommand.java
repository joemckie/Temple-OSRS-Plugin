package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.CollectionLogCategoryGroup;
import com.templeosrs.util.collections.chatcommands.ChatCommand;
import net.runelite.api.events.ChatMessage;

public class ListAllRaidsCategoriesChatCommand extends ChatCommand
{
	public ListAllRaidsCategoriesChatCommand()
	{
		super("!col list raids", "Lists all available raids categories", true);
	}

	@Override
	public void command(ChatMessage event)
	{
		listAvailableCollectionLogCategories(CollectionLogCategoryGroup.raids);
	}
}

package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.CollectionLogCategoryGroup;
import com.templeosrs.util.collections.chatcommands.ChatCommand;
import net.runelite.api.events.ChatMessage;

public class ListAllCluesCategoriesChatCommand extends ChatCommand
{
	public ListAllCluesCategoriesChatCommand()
	{
		super("!col list clues", "Lists all available clues categories", true);
	}

	@Override
	public void command(ChatMessage event)
	{
		listAvailableCollectionLogCategories(CollectionLogCategoryGroup.clues);
	}
}

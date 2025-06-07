package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.CollectionLogCategoryGroup;
import com.templeosrs.util.collections.chatcommands.ChatCommand;
import net.runelite.api.events.ChatMessage;

public class ListAllMinigamesCategoriesChatCommand extends ChatCommand {
    public ListAllMinigamesCategoriesChatCommand()
    {
        super("!col list minigames", "Lists all available minigames categories", true);
    }

    @Override
    public void command(ChatMessage event)
    {
        listAvailableCollectionLogCategories(CollectionLogCategoryGroup.minigames);
    }
}

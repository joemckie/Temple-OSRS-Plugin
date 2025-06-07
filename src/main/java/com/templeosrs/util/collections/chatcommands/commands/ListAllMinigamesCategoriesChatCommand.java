package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.chatcommands.ChatCommand;
import net.runelite.api.events.ChatMessage;

public class ListAllMinigamesCategoriesChatCommand extends ChatCommand {
    public ListAllMinigamesCategoriesChatCommand()
    {
        super("!col help minigames", "Lists all available minigames categories");
    }

    @Override
    public void execute(ChatMessage event)
    {

    }
}

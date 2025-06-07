package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.chatcommands.ChatCommand;
import net.runelite.api.events.ChatMessage;

public class ListAllRaidsCategoriesChatCommand extends ChatCommand {
    public ListAllRaidsCategoriesChatCommand()
    {
        super("!col help raids", "Lists all available raids categories");
    }

    @Override
    public void execute(ChatMessage event)
    {

    }
}

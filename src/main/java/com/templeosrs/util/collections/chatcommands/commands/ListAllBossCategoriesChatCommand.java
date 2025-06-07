package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.chatcommands.ChatCommand;
import net.runelite.api.events.ChatMessage;

public class ListAllBossCategoriesChatCommand extends ChatCommand {
    public ListAllBossCategoriesChatCommand()
    {
        super("!col help bosses", "Lists all available boss categories");
    }

    @Override
    public void execute(ChatMessage event)
    {

    }
}

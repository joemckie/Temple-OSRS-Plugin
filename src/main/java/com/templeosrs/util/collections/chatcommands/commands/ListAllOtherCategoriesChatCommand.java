package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.chatcommands.ChatCommand;
import net.runelite.api.events.ChatMessage;

public class ListAllOtherCategoriesChatCommand extends ChatCommand {
    public ListAllOtherCategoriesChatCommand()
    {
        super("!col help other", "Lists all available other categories");
    }

    @Override
    public void execute(ChatMessage event)
    {

    }
}

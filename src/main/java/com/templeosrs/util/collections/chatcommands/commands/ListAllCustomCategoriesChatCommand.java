package com.templeosrs.util.collections.chatcommands.commands;

import com.templeosrs.util.collections.chatcommands.ChatCommand;
import net.runelite.api.events.ChatMessage;

public class ListAllCustomCategoriesChatCommand extends ChatCommand {
    public ListAllCustomCategoriesChatCommand()
    {
        super("!col help custom", "Lists all available custom categories");
    }

    @Override
    public void execute(ChatMessage event)
    {

    }
}

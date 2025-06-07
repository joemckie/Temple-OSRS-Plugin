package com.templeosrs.util.collections.chatcommands;

import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatMessageManager;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;

public abstract class ChatCommand {
    @Inject
    protected Client client;

    @Inject
    protected ChatMessageManager chatMessageManager;

    @Inject
    protected ScheduledExecutorService scheduledExecutorService;

    public String trigger;

    public String description;

    public void execute(ChatMessage event) {}

    public ChatCommand(String trigger, String description)
    {
        this.trigger = trigger;
        this.description = description;
    }
}

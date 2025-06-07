package com.templeosrs.util.collections.chatcommands;

import com.templeosrs.util.collections.CollectionLogCategoryGroup;
import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.utils.PlayerNameUtils;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

public abstract class ChatCommand {
    public ChatCommand(String trigger, String description, boolean onlyShowForLocalPlayer)
    {
        this.trigger = trigger;
        this.description = description;
        this.onlyShowForLocalPlayer = onlyShowForLocalPlayer;
    }

    @Inject
    protected Client client;

    @Inject
    protected ChatMessageManager chatMessageManager;

    @Inject
    protected ScheduledExecutorService scheduledExecutorService;

    /**
     * The chat message that triggers the command, e.g. "!col help"
     */
    public String trigger;

    /**
     * The description given to the command when listed by the "!col help" command
     */
    public String description;

    /**
     * If true, hide the message from all other players except the local player
     */
    private final boolean onlyShowForLocalPlayer;

    /**
     * Checks whether the message sender is the currently logged in player
     * @param event the ChatMessage event
     * @return true if the message sender if the currently logged in player
     */
    public boolean isOtherPlayer(ChatMessage event)
    {
        String localName = PlayerNameUtils.normalizePlayerName(client.getLocalPlayer().getName());
        String senderName = PlayerNameUtils.normalizePlayerName(event.getName());

        return !senderName.equalsIgnoreCase(localName);
    }

    /**
     * Outputs a list of all available collection log categories.
     * As this is derived from the in-game cache,
     * it will always be up-to-date with the latest changes (unless a new tab is added).
     * @param categoryGroup The category group (i.e. tab) for which to list items
     */
    public void listAvailableCollectionLogCategories(CollectionLogCategoryGroup categoryGroup)
    {
        scheduledExecutorService.execute(() -> {
            chatMessageManager.queue(
                QueuedMessage.builder()
                    .type(ChatMessageType.GAMEMESSAGE)
                    .runeLiteFormattedMessage("Available " + categoryGroup.toString() + " categories (this is only visible to you):")
                    .build()
            );

            Set<String> categorySlugs = CollectionLogManager
                    .getCollectionLogCategoryTabSlugs()
                    .get(categoryGroup.getStructId());

            for (String categorySlug : categorySlugs) {
                chatMessageManager.queue(
                    QueuedMessage.builder()
                        .type(ChatMessageType.GAMEMESSAGE)
                        .runeLiteFormattedMessage(categorySlug)
                        .build()
                );
            }

            client.refreshChat();
        });
    }

    /**
     * Executes the chat command handler.
     * If it has been configured to only show for the current player, it will not be triggered for anyone else.
     * @param event The chat message event
     */
    public void execute(ChatMessage event)
    {
        if (onlyShowForLocalPlayer && isOtherPlayer(event)) {
            return;
        }

        command(event);
    }

    public void command(ChatMessage event) {}

    public void shutDown() {}
}

package com.templeosrs.util.collections.chatcommands;

import com.templeosrs.TempleOSRSPlugin;
import com.templeosrs.util.collections.CollectionLogCategory;
import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.CollectionLogRequestManager;
import com.templeosrs.util.collections.data.ObtainedCollectionItem;
import com.templeosrs.util.collections.database.CollectionDatabase;
import com.templeosrs.util.collections.parser.CollectionParser;
import com.templeosrs.util.collections.services.CollectionLogService;
import com.templeosrs.util.collections.utils.CollectionLogCategoryUtils;
import com.templeosrs.util.collections.utils.PlayerNameUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.IndexedSprite;
import net.runelite.api.StructComposition;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class CollectionLogChatCommandChatMessageSubscriber {
    @Inject
    private Client client;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private ScheduledExecutorService scheduledExecutorService;

    @Inject
    private CollectionLogRequestManager collectionLogRequestManager;

    @Inject
    private TempleOSRSPlugin templeOSRSPlugin;

    @Inject
    private CollectionParser collectionParser;

    @Inject
    private ItemManager itemManager;

    @Inject
    private CollectionLogService collectionLogService;

    @Inject
    private EventBus eventBus;

    @Inject
    private CollectionLogManager collectionLogManager;

    private final Map<Integer, Integer> itemIconIndexes = new HashMap<>();

    private final Set<Integer> loadedItemIds = new HashSet<>();

    public void startUp()
    {
        eventBus.register(this);
    }

    public void shutDown()
    {
        eventBus.unregister(this);

        // 🧼 Clear cached icons and IDs to prevent memory buildup
        itemIconIndexes.clear();
        loadedItemIds.clear();
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        final ChatMessageType type = event.getType();
        final String rawMessage = event.getMessage().trim();

        // Only react to public, private, or clan chat
        if (type != ChatMessageType.PUBLICCHAT &&
                type != ChatMessageType.FRIENDSCHAT &&
                type != ChatMessageType.PRIVATECHAT &&
                type != ChatMessageType.CLAN_CHAT &&
                type != ChatMessageType.CLAN_GIM_CHAT &&
                type != ChatMessageType.CLAN_GUEST_CHAT)
        {
            return;
        }

        final String COLLECTION_LOG_CHAT_TRIGGER = "!col ";

        // Command must start with "!col "
        if (!rawMessage.toLowerCase().startsWith(COLLECTION_LOG_CHAT_TRIGGER))
        {
            return;
        }

        String[] parts = rawMessage.substring(5).trim().split(" ", 2);

        if (parts.length == 0) {
            return;
        }

        // Normalize boss name
        String bossInput = parts[0].trim().replace(' ', '_').toLowerCase();

        final boolean isAliasFound = CollectionLogCategoryUtils.CATEGORY_ALIASES.containsKey(bossInput);

        CollectionLogCategory category;

        try {
            category = isAliasFound
                    ? CollectionLogCategoryUtils.CATEGORY_ALIASES.get(bossInput)
                    : CollectionLogCategory.valueOf(bossInput);
        } catch (IllegalArgumentException e) {
            log.warn("❌ No alias or category found for {}", bossInput);

            final String finalMessage = "\"" + bossInput + "\" is not a valid collection log category or alias!";

            chatMessageManager.queue(
                    QueuedMessage.builder()
                            .type(ChatMessageType.GAMEMESSAGE)
                            .runeLiteFormattedMessage("<col=ff0000>" + finalMessage + "</col>")
                            .build()
            );

            return;
        }

        StructComposition categoryStruct = client.getStructComposition(category.getStructId());

        // Determine target player (specified or sender)
        String playerName = (parts.length == 2) ? parts[1].trim() : event.getName();
        String normalizedPlayerName = PlayerNameUtils.normalizePlayerName(playerName);  // Normalize the player name for the API call
        String localName = PlayerNameUtils.normalizePlayerName(client.getLocalPlayer().getName());
        boolean isLocalPlayer = normalizedPlayerName.equalsIgnoreCase(localName);

        scheduledExecutorService.execute(() ->
        {
            final boolean isDataStale = !collectionLogService.isDataFresh(normalizedPlayerName);
            final boolean hasLocalData = CollectionDatabase.hasPlayerData(normalizedPlayerName);
            final boolean shouldUpdate = !hasLocalData || isDataStale;

            if (shouldUpdate)
            {
                log.debug("📭 No local data for '{}', fetching from API...", normalizedPlayerName);
                String json = collectionLogRequestManager.getPlayerCollectionLog(normalizedPlayerName);

                // Handle empty or failed fetch
                if (json == null || json.contains("error:unsynced"))
                {
                    log.warn("❌ No data fetched for user: {}", normalizedPlayerName);

                    final String errorMessage = json != null && json.contains("error:unsynced")
                        ? "⚠️ " + playerName + " has not synced their log on TempleOSRS."
                        : "⚠️ Failed to fetch log for " + playerName + ".";  // Use original name here

                    chatMessageManager.queue(
                            QueuedMessage.builder()
                                    .type(ChatMessageType.GAMEMESSAGE)
                                    .runeLiteFormattedMessage("<col=ff6666>" + errorMessage + "</col>")
                                    .build()
                    );

                    return;
                }

                if (!isLocalPlayer)
                {
                    CollectionDatabase.pruneOldPlayers(localName, templeOSRSPlugin.getConfig().maxCachedPlayers());
                }

                collectionParser.parseAndStore(PlayerNameUtils.normalizePlayerName(playerName), json);
            }
            else
            {
                log.debug("✔️ Found cached data for '{}'", normalizedPlayerName);
            }

            // Fetch the requested category
            Set<ObtainedCollectionItem> items = CollectionDatabase.getItemsByCategory(
                    normalizedPlayerName,
                    collectionLogManager.getCollectionLogCategoryItemMap().get(categoryStruct.getId())
            );

            loadItemIcons(items);

            StringBuilder sb = new StringBuilder();
            String categoryName = categoryStruct.getStringValue(689);

            // If sender's name is same as the player being queried, omit the player's name
            if (!event.getName().equalsIgnoreCase(playerName)) {
                sb
                        .append("<col=ffffaa>")
                        .append(playerName)  // Append the original player name here
                        .append("'s ")
                        .append(categoryName).append(": ")
                        .append("</col>");
            } else {
                sb.append(categoryName).append(": ");
            }

            if (items.isEmpty()) {
                sb.append("No data found.");
            } else {
                int i = 0;

                for (ObtainedCollectionItem item : items)
                {
                    Integer icon = itemIconIndexes.get(item.getId());

                    if (icon != null) {
                        sb.append("<img=").append(icon).append("> ");
                    }

                    sb.append("x").append(item.getCount());

                    if (i++ < items.size() - 1) {
                        sb.append(", ");
                    }
                }
            }

            SwingUtilities.invokeLater(() -> {
                event.getMessageNode().setRuneLiteFormatMessage(sb.toString());
                client.refreshChat();
            });
        });
    }

    /**
     * Loads the in-game icons for a given item list, ready to be used in the chat message.
     * @param items The item list for which to load item icons.
     */
    private void loadItemIcons(Set<ObtainedCollectionItem> items) {
        List<ObtainedCollectionItem> newItems = new ArrayList<>();

        for (ObtainedCollectionItem item : items) {
            if (!loadedItemIds.contains(item.getId())) {
                newItems.add(item);
                loadedItemIds.add(item.getId());
            }
        }

        if (newItems.isEmpty()) {
            return;
        }

        IndexedSprite[] modIcons = client.getModIcons();

        if (modIcons == null) {
            return;
        }

        int currentLength = modIcons.length;
        int newSize = currentLength + newItems.size();
        IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, newSize);
        client.setModIcons(newModIcons);

        for (int i = 0; i < newItems.size(); i++) {
            ObtainedCollectionItem item = newItems.get(i);
            int modIconIndex = currentLength + i;
            itemIconIndexes.put(item.getId(), modIconIndex);

            AsyncBufferedImage img = itemManager.getImage(item.getId());

            img.onLoaded(() -> {
                BufferedImage scaled = ImageUtil.resizeImage(img, 18, 16);
                IndexedSprite sprite = ImageUtil.getImageIndexedSprite(scaled, client);
                client.getModIcons()[modIconIndex] = sprite;
            });
        }
    }
}

package com.templeosrs.util.collections.chatcommands;

import com.templeosrs.TempleOSRSPlugin;
import com.templeosrs.util.collections.CollectionLogCategory;
import com.templeosrs.util.collections.CollectionLogRequestManager;
import com.templeosrs.util.collections.chatcommands.db.CollectionDatabase;
import com.templeosrs.util.collections.chatcommands.parser.CollectionParser;
import com.templeosrs.util.collections.chatcommands.utils.CategoryAliases;
import com.templeosrs.util.collections.chatcommands.utils.PlayerNameUtils;
import com.templeosrs.util.collections.data.CollectionItem;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.IndexedSprite;
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
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class CollectionLogChatCommandsManager {
    @Inject
    private EventBus eventBus;

    @Inject
    private Client client;

    @Inject
    private ExecutorService executor;

    @Inject
    private TempleOSRSPlugin templeOSRSPlugin;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private CollectionLogRequestManager collectionLogRequestManager;

    private final Map<Integer, Integer> itemIconIndexes = new HashMap<>();

    private final Set<Integer> loadedItemIds = new HashSet<>();

    public void startUp()
    {
        eventBus.register(this);

        CollectionDatabase.init();
    }

    public void shutDown()
    {
        eventBus.unregister(this);

//        clientToolbar.removeNavigation(navButton);

        // 🧼 Clear cached icons and IDs to prevent memory buildup
        itemIconIndexes.clear();
        loadedItemIds.clear();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        log.debug("Collection Tracker stopped!");
    }

    private void syncCollectionLog(Client client) {
        Executors.newSingleThreadExecutor().execute(() -> {
            log.debug("🔄 Starting syncCollectionLog()...");

            CollectionDatabase.clearAll();

            if (client.getLocalPlayer() == null) {
                log.warn("⚠️ Local player is null — not logged in yet.");
                return;
            }

            String username = Objects.requireNonNull(client.getLocalPlayer().getName()).toLowerCase();
            log.debug("👤 Detected username: {}", username);

            String json = collectionLogRequestManager.getPlayerCollectionLog(username);

            log.debug("📥 Fetched JSON: {} characters", json != null ? json.length() : 0);

            if (json == null || json.isEmpty()) {
                log.error("❌ Empty or null response from Temple API");
                return;
            }

            log.debug("🧩 Parsing and storing JSON...");
            CollectionParser parser = new CollectionParser();

            parser.parseAndStore(PlayerNameUtils.normalizePlayerName(username), json);
            log.debug("✅ Parsing complete.");
        });
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

        // Command must start with "!col "
        if (!rawMessage.toLowerCase().startsWith("!col "))
        {
            return;
        }

        String[] parts = rawMessage.substring(5).trim().split(" ", 2);
        if (parts.length == 0)
            return;

        // Normalize boss name
        String bossInput = parts[0].trim().replace(' ', '_');
        CollectionLogCategory bossKey = CategoryAliases.CATEGORY_ALIASES.getOrDefault(
                bossInput.toLowerCase(),
                CollectionLogCategory.valueOf(bossInput.toLowerCase())
        );

        // Determine target player (specified or sender)
        String playerName = (parts.length == 2) ? parts[1].trim() : event.getName();
        String normalizedPlayerName = PlayerNameUtils.normalizePlayerName(playerName);  // Normalize the player name for the API call
        String localName = client.getLocalPlayer() != null
                ? PlayerNameUtils.normalizePlayerName(client.getLocalPlayer().getName())
                : "";
        boolean isLocalPlayer = normalizedPlayerName.equalsIgnoreCase(localName);

        executor.execute(() ->
        {
            String lastChanged = collectionLogRequestManager.getLastChangedTimestamp(normalizedPlayerName);
            Timestamp dbTimestamp = CollectionDatabase.getLatestTimestamp(normalizedPlayerName);
            Timestamp apiTimestamp = lastChanged != null ? Timestamp.valueOf(lastChanged) : null;

            log.debug("🕒 [Compare] {} | DB: {} | API: {}", normalizedPlayerName, dbTimestamp, apiTimestamp);

            boolean hasLocalData = CollectionDatabase.hasPlayerData(normalizedPlayerName);
            boolean shouldUpdate = !hasLocalData || (apiTimestamp != null && (dbTimestamp == null || dbTimestamp.before(apiTimestamp)));

            if (shouldUpdate)
            {
                log.debug("📭 No local data for '{}', fetching from API...", normalizedPlayerName);
                String json = collectionLogRequestManager.getPlayerCollectionLog(normalizedPlayerName);

                // Handle empty or failed fetch
                if (json == null || json.isEmpty())
                {
                    log.warn("❌ No data fetched for user: {}", normalizedPlayerName);

                    String errorMessage = "⚠️ Failed to fetch log for " + playerName + ".";  // Use original name here
                    if (json != null && json.contains("Player has not synced"))
                    {
                        errorMessage = "⚠️ " + playerName + " has not synced their log on TempleOSRS.";  // Use original name here
                    }

                    final String finalMessage = errorMessage;
                    chatMessageManager.queue(
                            QueuedMessage.builder()
                                    .type(ChatMessageType.GAMEMESSAGE)
                                    .runeLiteFormattedMessage("<col=ff6666>" + finalMessage + "</col>")
                                    .build()
                    );
                    return;
                }

                if (!isLocalPlayer)
                {
                    CollectionDatabase.pruneOldPlayers(localName, templeOSRSPlugin.getConfig().maxCachedPlayers());
                }

                CollectionParser parser = new CollectionParser();
                parser.parseAndStore(PlayerNameUtils.normalizePlayerName(playerName), json);
            }
            else
            {
                log.debug("✔️ Found cached data for '{}'", normalizedPlayerName);
            }

            // Fetch the requested category
            List<CollectionItem> items = CollectionDatabase.getItemsByCategory(normalizedPlayerName, bossKey.toString());
            loadItemIcons(items);

            StringBuilder sb = new StringBuilder();

            // If sender's name is same as the player being queried, omit the player's name
            if (!event.getName().equalsIgnoreCase(playerName)) {
                sb.append("<col=ffffaa>")
                        .append(playerName)  // Append the original player name here
                        .append("'s ");
            }

            sb.append(toTitleCase(bossKey.toString().replace('_', ' ')))
                    .append("</col>");

            if (items.isEmpty())
            {
                sb.append(" No data found.");
            }
            else
            {
                Map<Integer, CollectionItem> merged = new HashMap<>();
                for (CollectionItem item : items)
                {
                    merged.compute(item.getItemId(), (id, existing) ->
                    {
                        if (existing == null) return item;
                        existing.setCount(existing.getCount() + item.getCount());
                        return existing;
                    });
                }

                int i = 0;
                for (CollectionItem item : merged.values())
                {
                    Integer icon = itemIconIndexes.get(item.getItemId());
                    if (icon != null)
                    {
                        sb.append("<img=").append(icon).append("> ");
                    }
                    sb.append("x").append(item.getCount());
                    if (i++ < merged.size() - 1) sb.append(", ");
                }
            }

            SwingUtilities.invokeLater(() -> {
                event.getMessageNode().setRuneLiteFormatMessage(sb.toString());
                client.refreshChat();
            });
        });
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;

        String[] words = input.toLowerCase().split(" ");
        StringBuilder titleCase = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                titleCase.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return titleCase.toString().trim();
    }

    private void loadItemIcons(List<CollectionItem> items) {
        List<CollectionItem> newItems = new ArrayList<>();

        for (CollectionItem item : items) {
            if (!loadedItemIds.contains(item.getItemId())) {
                newItems.add(item);
                loadedItemIds.add(item.getItemId());
            }
        }

        if (newItems.isEmpty()) return;

        IndexedSprite[] modIcons = client.getModIcons();
        if (modIcons == null) return;

        int currentLength = modIcons.length;
        int newSize = currentLength + newItems.size();
        IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, newSize);
        client.setModIcons(newModIcons);

        for (int i = 0; i < newItems.size(); i++) {
            CollectionItem item = newItems.get(i);
            int modIconIndex = currentLength + i;
            itemIconIndexes.put(item.getItemId(), modIconIndex);

            AsyncBufferedImage img = itemManager.getImage(item.getItemId());

            img.onLoaded(() -> {
                BufferedImage scaled = ImageUtil.resizeImage(img, 18, 16);
                IndexedSprite sprite = ImageUtil.getImageIndexedSprite(scaled, client);
                client.getModIcons()[modIconIndex] = sprite;
            });
        }
    }
}

package com.templeosrs.util.collections.chatcommands;

import com.templeosrs.util.collections.CollectionLogCategoryGroup;
import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.utils.CollectionLogCategoryUtils;
import com.templeosrs.util.collections.utils.PlayerNameUtils;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.IndexedSprite;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

@Slf4j
public abstract class ChatCommand
{
	/**
	 * Maintain a map of item IDs to their respective index in the icon list
	 */
	protected final Map<Integer, Integer> itemIconIndexes = new HashMap<>();
	/**
	 * Maintain a list of previously seen item icons to avoid loading them twice
	 */
	protected final Set<Integer> loadedItemIds = new HashSet<>();
	/**
	 * If true, hide the message from all other players except the local player
	 */
	private final boolean onlyShowForLocalPlayer;
	/**
	 * The chat message that triggers the command, e.g. "!col help"
	 */
	public String trigger;
	/**
	 * The description given to the command when listed by the "!col help" command
	 */
	public String description;
	@Inject
	protected Client client;
	@Inject
	protected ClientThread clientThread;
	@Inject
	protected ChatMessageManager chatMessageManager;
	@Inject
	protected ItemManager itemManager;

	public ChatCommand(String trigger, String description, boolean onlyShowForLocalPlayer)
	{
		this.trigger = trigger;
		this.description = description;
		this.onlyShowForLocalPlayer = onlyShowForLocalPlayer;
	}

	/**
	 * Checks whether the message sender is the currently logged in player
	 *
	 * @param event the ChatMessage event
	 * @return true if the message sender if the currently logged in player
	 */
	public boolean isOtherPlayer(ChatMessage event)
	{
		String localName = PlayerNameUtils.normalizePlayerName(client.getLocalPlayer().getName());
		String senderName = PlayerNameUtils.normalizePlayerName(event.getName());

		return !senderName.equalsIgnoreCase(localName);
	}

	public String buildAvailableCategoriesMessage(String category)
	{
		return new ChatMessageBuilder()
			.append(ChatColorType.NORMAL)
			.append("Available ")
			.append(ChatColorType.HIGHLIGHT)
			.append(category)
			.append(ChatColorType.NORMAL)
			.append(" categories:")
			.build();
	}

	public void overwriteMessage(String newMessage, MessageNode messageNode)
	{
		messageNode.setRuneLiteFormatMessage(newMessage);
		client.refreshChat();
	}

	/**
	 * Outputs a list of all available collection log categories.
	 * As this is derived from the in-game cache,
	 * it will always be up-to-date with the latest changes (unless a new tab is added).
	 *
	 * @param categoryGroup The category group (i.e. tab) for which to list items
	 */
	public void listAvailableCollectionLogCategories(CollectionLogCategoryGroup categoryGroup)
	{
		clientThread.invoke(() -> {
			chatMessageManager.queue(
				QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(buildAvailableCategoriesMessage(categoryGroup.toString()))
					.build()
			);

			Set<String> categorySlugs = CollectionLogManager
				.getCollectionLogCategoryTabSlugs()
				.get(categoryGroup.getStructId());

			List<Integer> iconItemIds = categorySlugs.stream().map(slug -> {
				int structId = CollectionLogManager.getCollectionLogCategoryStructIdMap().get(slug);
				Set<Integer> categoryItems = CollectionLogManager.getCollectionLogCategoryItemMap().get(structId);

				return new ArrayList<>(categoryItems).get(0);
			}).collect(Collectors.toList());

			loadItemIcons(iconItemIds);

			int i = 0;

			for (String categorySlug : categorySlugs)
			{
				int structId = CollectionLogManager.getCollectionLogCategoryStructIdMap().get(categorySlug);
				String categoryTitle = client.getStructComposition(structId).getStringValue(689);
				int iconIndex = itemIconIndexes.get(iconItemIds.get(i++));

				ChatMessageBuilder chatMessageBuilder = new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.img(iconIndex)
					.append(categoryTitle)
					.append(": ");

				Set<String> categoryAliases = CollectionLogCategoryUtils.INVERTED_ALIASES.get(categorySlug);

				chatMessageBuilder
					.append(ChatColorType.HIGHLIGHT)
					.append(
						categoryAliases == null
							? categorySlug
							: String.join(", ", categoryAliases)
					);

				chatMessageManager.queue(
					QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(chatMessageBuilder.build())
						.build()
				);
			}

			client.refreshChat();
		});
	}

	/**
	 * Loads the in-game icons for a given item list, ready to be used in the chat message.
	 *
	 * @param iconItemIds The item list for which to load item icons.
	 */
	protected void loadItemIcons(List<Integer> iconItemIds)
	{
		// Starting with an empty list, we find which icons haven't previously been seen
		List<Integer> newItems = new ArrayList<>();

		for (int itemId : iconItemIds)
		{
			if (!loadedItemIds.contains(itemId))
			{
				newItems.add(itemId);
				loadedItemIds.add(itemId);
			}
		}

		if (newItems.isEmpty())
		{
			return;
		}

		final IndexedSprite[] modIcons = client.getModIcons();

		assert modIcons != null;

		final IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + iconItemIds.size());
		final int iconIndex = modIcons.length;

		client.setModIcons(newModIcons);

		int i = 0;

		for (int itemId : iconItemIds)
		{
			final AsyncBufferedImage img = itemManager.getImage(itemId);
			final int idx = iconIndex + i++;

			itemIconIndexes.put(itemId, idx);

			img.onLoaded(() ->
			{
				final BufferedImage image = ImageUtil.resizeImage(img, 18, 16);
				final IndexedSprite sprite = ImageUtil.getImageIndexedSprite(image, client);
				// modicons array might be replaced in between when we assign it and the callback,
				// so fetch modicons again
				client.getModIcons()[idx] = sprite;
			});
		}
	}

	/**
	 * Executes the chat command handler.
	 * If it has been configured to only show for the current player, it will not be triggered for anyone else.
	 *
	 * @param event The chat message event
	 */
	public void execute(ChatMessage event)
	{
		if (onlyShowForLocalPlayer && isOtherPlayer(event))
		{
			return;
		}

		command(event);
	}

	public void command(ChatMessage event)
	{
	}

	public void shutDown()
	{
		itemIconIndexes.clear();
		loadedItemIds.clear();
	}
}

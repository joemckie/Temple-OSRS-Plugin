package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.TempleOSRSPlugin;
import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.CollectionLogRequestManager;
import com.templeosrs.util.collections.data.CollectionLogItem;
import com.templeosrs.util.collections.data.PlayerInfoResponse;
import com.templeosrs.util.collections.database.CollectionDatabase;
import com.templeosrs.util.collections.parser.CollectionParser;
import com.templeosrs.util.collections.services.CollectionLogService;
import com.templeosrs.util.collections.utils.PlayerNameUtils;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

@Slf4j
public class CollectionLogPlayerLookupPanel extends PluginPanel
{
	private final Client client;
	private final CollectionLogRequestManager collectionLogRequestManager;
	private final ScheduledExecutorService scheduledExecutorService;
	private final CollectionLogService collectionLogService;
	private final CollectionParser collectionParser;
	private final TempleOSRSPlugin templeOSRSPlugin;

	@Getter
	private final ItemManager itemManager;

	private final JPanel layoutPanel = new JPanel();

	private CollectionLogPlayerLookupResultPanel resultsPanel;

	private Map<Integer, CollectionLogItem> lookupResult;

	@Inject
	public CollectionLogPlayerLookupPanel(
		final Client client,
		final CollectionLogRequestManager collectionLogRequestManager,
		final ScheduledExecutorService scheduledExecutorService,
		final CollectionLogService collectionLogService,
		final CollectionParser collectionParser,
		final TempleOSRSPlugin templeOSRSPlugin,
		final ItemManager itemManager
	)
	{
		this.client = client;
		this.collectionLogRequestManager = collectionLogRequestManager;
		this.scheduledExecutorService = scheduledExecutorService;
		this.collectionLogService = collectionLogService;
		this.collectionParser = collectionParser;
		this.templeOSRSPlugin = templeOSRSPlugin;
		this.itemManager = itemManager;

		setBorder(new EmptyBorder(6, 6, 6, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.Y_AXIS));
		add(layoutPanel, BorderLayout.NORTH);
	}

	public void lookup(@NonNull String username)
	{
		scheduledExecutorService.execute(() ->
		{
			String normalizedPlayerName = PlayerNameUtils.normalizePlayerName(username);

			PlayerInfoResponse.Data playerInfo = getPlayerInfo(normalizedPlayerName);

			if (playerInfo == null || playerInfo.getCollectionLog().getLastChanged() == null)
			{
				// Player has no log
				// TODO: Render message in panel
				return;
			}

			String lastChanged = playerInfo.getCollectionLog().getLastChanged();

			final boolean isDataStale = !collectionLogService.isDataFresh(normalizedPlayerName, lastChanged);
			final boolean hasLocalData = CollectionDatabase.hasPlayerData(normalizedPlayerName);
			final boolean shouldUpdate = !hasLocalData || isDataStale;
			final String localName = PlayerNameUtils.normalizePlayerName(client.getLocalPlayer().getName());
			boolean isLocalPlayer = normalizedPlayerName.equalsIgnoreCase(localName);

			if (shouldUpdate)
			{
				log.debug("📭 No local data for '{}', fetching from API...", normalizedPlayerName);
				String json = collectionLogRequestManager.getPlayerCollectionLog(normalizedPlayerName);

				if (json == null) {
					log.warn("❌ No data fetched for user: {}", normalizedPlayerName);

					return;
				}

				if (!isLocalPlayer)
				{
					CollectionDatabase.pruneOldPlayers(localName, templeOSRSPlugin.getConfig().maxCachedPlayers());
				}

				collectionParser.parseAndStore(normalizedPlayerName, json);
			}
			else
			{
				log.debug("✔️ Found cached data for '{}'", normalizedPlayerName);
			}

			final Map<Integer, CollectionLogItem> allCollectionLogItems = CollectionLogManager.getCollectionLogItems();

			// Fetch the requested category
			Map<Integer, CollectionLogItem> results = CollectionDatabase.getItemsByCategory(
				normalizedPlayerName,
				new LinkedHashMap<>(allCollectionLogItems)
			);

			Map<Integer, CollectionLogItem> obtainedCollectionItemMap = new HashMap<>();

			for (CollectionLogItem item : results.values())
			{
				obtainedCollectionItemMap.put(item.getId(), item);
			}

			lookupResult = obtainedCollectionItemMap;

			refreshPanel();
		});
	}

	private PlayerInfoResponse.Data getPlayerInfo(@NonNull String playerName)
	{
		try
		{
			return collectionLogRequestManager.getPlayerInfo(playerName);
		}
		catch (IOException e)
		{
			return null;
		}
	}

	private void rebuildPanel()
	{
		SwingUtil.fastRemoveAll(layoutPanel);

		resultsPanel = new CollectionLogPlayerLookupResultPanel(this.lookupResult);

		resultsPanel
			.getCollectionLogGridItems()
			.forEach(gridItem -> gridItem.updateIcon(this));

		layoutPanel.add(resultsPanel);

		revalidate();
		repaint();
	}

	private void refreshPanel()
	{
		SwingUtilities.invokeLater(this::rebuildPanel);
	}
}

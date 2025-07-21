package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.TempleOSRSPlugin;
import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.CollectionLogRequestManager;
import com.templeosrs.util.collections.data.ObtainedCollectionItem;
import com.templeosrs.util.collections.data.PlayerInfoResponse;
import com.templeosrs.util.collections.database.CollectionDatabase;
import com.templeosrs.util.collections.parser.CollectionParser;
import com.templeosrs.util.collections.services.CollectionLogService;
import com.templeosrs.util.collections.utils.PlayerNameUtils;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
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
	private final ItemManager itemManager;
	private final ClientThread clientThread;

	private CollectionLogPlayerLookupResultPanel resultsPanel;

	private final JPanel mainPanel = new JPanel();

	private Map<Integer, ObtainedCollectionItem> lookupResult;

	@Inject
	public CollectionLogPlayerLookupPanel(
		Client client,
		CollectionLogRequestManager collectionLogRequestManager,
		ScheduledExecutorService scheduledExecutorService,
		CollectionLogService collectionLogService,
		CollectionParser collectionParser,
		TempleOSRSPlugin templeOSRSPlugin,
		ItemManager itemManager,
		ClientThread clientThread
	)
	{
		this.client = client;
		this.collectionLogRequestManager = collectionLogRequestManager;
		this.scheduledExecutorService = scheduledExecutorService;
		this.collectionLogService = collectionLogService;
		this.collectionParser = collectionParser;
		this.templeOSRSPlugin = templeOSRSPlugin;
		this.itemManager = itemManager;
		this.clientThread = clientThread;

		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new GridBagLayout());

		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		add(mainPanel);
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

			final Set<Integer> allCollectionLogItems = CollectionLogManager.getAllCollectionLogItems();

			// Fetch the requested category
			Set<ObtainedCollectionItem> results = CollectionDatabase.getItemsByCategory(
				normalizedPlayerName,
				new LinkedHashSet<>(allCollectionLogItems)
			);

			Map<Integer, ObtainedCollectionItem> obtainedCollectionItemMap = new HashMap<>();

			for (ObtainedCollectionItem item : results)
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
		SwingUtil.fastRemoveAll(mainPanel);

		resultsPanel = new CollectionLogPlayerLookupResultPanel(itemManager, clientThread, this.lookupResult);

		mainPanel.add(resultsPanel);

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	private void refreshPanel()
	{
		SwingUtilities.invokeLater(this::rebuildPanel);
	}
}

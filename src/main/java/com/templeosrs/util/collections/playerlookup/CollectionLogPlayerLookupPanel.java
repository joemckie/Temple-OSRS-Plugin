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
import java.awt.Dimension;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import joptsimple.internal.Strings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

@Slf4j
public class CollectionLogPlayerLookupPanel extends PluginPanel
{
	/* The maximum allowed username length in RuneScape accounts */
	private static final int MAX_USERNAME_LENGTH = 12;

	private final Client client;
	private final CollectionLogRequestManager collectionLogRequestManager;
	private final ScheduledExecutorService scheduledExecutorService;
	private final CollectionLogService collectionLogService;
	private final CollectionParser collectionParser;
	private final TempleOSRSPlugin templeOSRSPlugin;

	private Map<Integer, CollectionLogItem> lookupResult = new HashMap<>();

	private String lookupUsername = null;

	private final IconTextField searchBar;

	private final CollectionLogPlayerLookupResultPanel resultsPanel;

	private boolean loading = false;

	@Inject
	public CollectionLogPlayerLookupPanel(
		final Client client,
		final CollectionLogRequestManager collectionLogRequestManager,
		final ScheduledExecutorService scheduledExecutorService,
		final CollectionLogService collectionLogService,
		final CollectionParser collectionParser,
		final TempleOSRSPlugin templeOSRSPlugin,
		final ItemManager itemManager,
		final SpriteManager spriteManager
	)
	{
		super(false);

		this.client = client;
		this.collectionLogRequestManager = collectionLogRequestManager;
		this.scheduledExecutorService = scheduledExecutorService;
		this.collectionLogService = collectionLogService;
		this.collectionParser = collectionParser;
		this.templeOSRSPlugin = templeOSRSPlugin;
		this.resultsPanel = new CollectionLogPlayerLookupResultPanel(itemManager, spriteManager);

		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		searchBar = new IconTextField();
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.setMinimumSize(new Dimension(0, 30));
		searchBar.addActionListener(
			e -> {
				lookup();
			}
		);
		searchBar.addClearListener(
			() ->
			{
				searchBar.setIcon(IconTextField.Icon.SEARCH);
				searchBar.setEditable(true);
			}
		);

		add(searchBar, BorderLayout.NORTH);
		add(resultsPanel, BorderLayout.CENTER);

		refreshPanel();
	}

	private void updateLoadingUI()
	{
		searchBar.setEditable(!loading);
		searchBar.setIcon(
			loading
				? IconTextField.Icon.LOADING_DARKER
				: IconTextField.Icon.SEARCH
		);
	}

	public void lookup(@NonNull String username)
	{
		loading = true;
		lookupUsername = username;

		lookupResult.clear();
		searchBar.setText(username);

		updateLoadingUI();

		scheduledExecutorService.execute(() ->
		{

			String normalizedPlayerName = PlayerNameUtils.normalizePlayerName(username);

			PlayerInfoResponse.Data playerInfo = getPlayerInfo(normalizedPlayerName);

			if (playerInfo == null || playerInfo.getCollectionLog().getLastChanged() == null)
			{
				loading = false;
				updateLoadingUI();

				// Player has no log
				refreshPanel();

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

				if (json == null)
				{
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

			loading = false;
			updateLoadingUI();
			refreshPanel();
		});
	}

	public void lookup()
	{
		final String username = sanitize(searchBar.getText());

		if (Strings.isNullOrEmpty(username))
		{
			return;
		}

		// RuneScape usernames can't be longer than 12 characters long
		if (username.length() > MAX_USERNAME_LENGTH)
		{
			loading = false;
			searchBar.setIcon(IconTextField.Icon.ERROR);
			return;
		}

		lookup(username);
	}

	private PlayerInfoResponse.Data getPlayerInfo(@NonNull String playerName)
	{
		try
		{
			return collectionLogRequestManager.getPlayerInfo(playerName);
		}
		catch (IOException | NullPointerException e)
		{
			return null;
		}
	}

	private static String sanitize(String lookup)
	{
		return lookup.replace('\u00A0', ' ');
	}

	private void rebuildResultsPanel()
	{
		resultsPanel.rebuildPanel(lookupUsername, lookupResult, loading);
		resultsPanel.revalidate();
		resultsPanel.repaint();
	}

	private void refreshPanel()
	{
		SwingUtilities.invokeLater(this::rebuildResultsPanel);
	}
}

package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.data.CollectionLogCategory;
import com.templeosrs.util.collections.data.CollectionLogItem;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;import net.runelite.client.util.SwingUtil;

@Slf4j
public class CollectionLogPlayerLookupResultPanel extends JPanel
{
	@Getter
	private final List<CollectionLogGridItemLabel> collectionLogGridItems = new ArrayList<>();

	private final List<CollectionLogCategoryItemsPanel> collectionLogCategoryItemsPanels = new ArrayList<>();

	private final JPanel panel = new JPanel();

	@Getter
	private Map<Integer, CollectionLogItem> obtainedCollectionLogItems;

	private final ItemManager itemManager;

	public CollectionLogPlayerLookupResultPanel(final ItemManager itemManager)
	{
		super(new BorderLayout());

		this.itemManager = itemManager;

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JScrollPane scrollPane = new JScrollPane(panel);

		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(new EmptyBorder(10, 0, 0, 0));

		add(scrollPane, BorderLayout.CENTER);
	}

	private void addCollectionLogCategory(final CollectionLogCategory category)
	{
		final CollectionLogCategoryItemsPanel collectionLogCategoryItemsPanel = new CollectionLogCategoryItemsPanel(
			category.getItems(),
			obtainedCollectionLogItems
		);

		final CollectionLogCategoryHeader collectionLogCategoryHeader = new CollectionLogCategoryHeader(
			this,
			collectionLogCategoryItemsPanel,
			category
		);

		collectionLogCategoryItemsPanel.repaintGrid();

		collectionLogCategoryItemsPanels.add(collectionLogCategoryItemsPanel);
		collectionLogGridItems.addAll(collectionLogCategoryItemsPanel.getCategoryCollectionLogGridItems());

		final JPanel categoryContainer = new JPanel();
		categoryContainer.setLayout(new BoxLayout(categoryContainer, BoxLayout.Y_AXIS));

		categoryContainer.add(collectionLogCategoryHeader);
		categoryContainer.add(collectionLogCategoryItemsPanel);

		panel.add(categoryContainer);
	}

	public void rebuildPanel(
		final String username,
		final Map<Integer, CollectionLogItem> obtainedCollectionLogItems,
		final boolean isLoading
	)
	{
		log.debug("username {}, isloading {}", username, isLoading);

		SwingUtil.fastRemoveAll(panel);

		collectionLogCategoryItemsPanels.clear();

		if (isLoading)
		{
			return;
		}

		if (username == null)
		{
			this.obtainedCollectionLogItems = new HashMap<>();

			JLabel noLookupText = new JLabel();

			noLookupText.setText("Search for a user");

			panel.add(noLookupText, BorderLayout.CENTER);

			return;
		}

		if (obtainedCollectionLogItems.isEmpty())
		{
			this.obtainedCollectionLogItems = new HashMap<>();

			JLabel noResultsText = new JLabel();

			noResultsText.setText("No results found for " + username);

			panel.add(noResultsText, BorderLayout.CENTER);

			return;
		}

		this.obtainedCollectionLogItems = obtainedCollectionLogItems;

		Map<Integer, CollectionLogCategory> collectionLogCategoryItemMap = CollectionLogManager.getCollectionLogCategoryMap();

//		final CollectionLogPlayerMetadataPanel collectionLogPlayerMetadataPanel = new CollectionLogPlayerMetadataPanel(
//			username
//		);
//
//		add(collectionLogPlayerMetadataPanel, BorderLayout.NORTH);

		for (CollectionLogCategory category : collectionLogCategoryItemMap.values())
		{
			addCollectionLogCategory(category);
		}

		collectionLogGridItems.forEach(gridItem -> gridItem.updateIcon(itemManager));

		collectionLogCategoryItemsPanels.get(0).setVisible(true);
	}
}

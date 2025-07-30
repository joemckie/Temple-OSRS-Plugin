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
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.SwingUtil;

@Slf4j
public class CollectionLogPlayerLookupResultPanel extends JPanel
{
	@Getter
	private final List<CollectionLogGridItemLabel> collectionLogGridItems = new ArrayList<>();

	private final JPanel panel = new JPanel();

	@Getter
	private final Map<Integer, CollectionLogItem> obtainedCollectionLogItems = new HashMap<>();

	private final ItemManager itemManager;

	private final SpriteManager spriteManager;

	public CollectionLogPlayerLookupResultPanel(
		final ItemManager itemManager,
		final SpriteManager spriteManager
	)
	{
		super(new BorderLayout());

		this.itemManager = itemManager;
		this.spriteManager = spriteManager;

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JScrollPane scrollPane = new JScrollPane(panel);

		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(new EmptyBorder(10, 0, 0, 0));

		add(scrollPane, BorderLayout.CENTER);
	}

	private void addCollectionLogCategory(final CollectionLogCategory category, final boolean isExpanded)
	{
		final CollectionLogCategoryItemsPanel collectionLogCategoryItemsPanel = new CollectionLogCategoryItemsPanel(
			category.getItems(),
			obtainedCollectionLogItems,
			isExpanded
		);

		final CollectionLogCategoryHeader collectionLogCategoryHeader = new CollectionLogCategoryHeader(
			this,
			collectionLogCategoryItemsPanel,
			category,
			spriteManager,
			isExpanded
		);

		collectionLogCategoryItemsPanel.repaintGrid();

		collectionLogGridItems.addAll(collectionLogCategoryItemsPanel.getCategoryCollectionLogGridItems());

		final JPanel categoryContainer = new JPanel();
		categoryContainer.setLayout(new BoxLayout(categoryContainer, BoxLayout.Y_AXIS));

		categoryContainer.add(collectionLogCategoryHeader);
		categoryContainer.add(collectionLogCategoryItemsPanel);

		panel.add(categoryContainer);
	}

	public void addMessage(final String message)
	{
		panel.add(new JLabel(message), BorderLayout.CENTER);
	}

	public void rebuildPanel(
		final String username,
		final Map<Integer, CollectionLogItem> obtainedCollectionLogItems,
		final boolean isLoading
	)
	{
		SwingUtil.fastRemoveAll(panel);

		if (isLoading)
		{
			log.debug("Rendering loading screen");
			this.obtainedCollectionLogItems.clear();

			return;
		}

		if (username == null)
		{
			log.debug("Rendering no username screen");
			addMessage("Search for a user");

			return;
		}

		if (obtainedCollectionLogItems.isEmpty())
		{
			log.debug("Rendering no results screen");
			addMessage("No results found for " + username);

			return;
		}

		this.obtainedCollectionLogItems.putAll(obtainedCollectionLogItems);

		Map<Integer, CollectionLogCategory> collectionLogCategoryItemMap = CollectionLogManager.getCollectionLogCategoryMap();

		int index = 0;

		for (CollectionLogCategory category : collectionLogCategoryItemMap.values())
		{
			addCollectionLogCategory(category, index == 0);
			index++;
		}

		collectionLogGridItems.forEach(gridItem -> gridItem.updateIcon(itemManager));
	}
}

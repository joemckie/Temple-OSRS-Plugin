package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.data.CollectionLogCategory;
import com.templeosrs.util.collections.data.CollectionLogItem;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CollectionLogPlayerLookupResultPanel extends JPanel
{
	@Getter
	private final List<CollectionLogGridItemLabel> collectionLogGridItems = new ArrayList<>();

	private final List<CollectionLogCategoryItemsPanel> collectionLogCategoryItemsPanels = new ArrayList<>();

	@Getter
	private final Map<Integer, CollectionLogItem> obtainedCollectionLogItems;

	public CollectionLogPlayerLookupResultPanel(Map<Integer, CollectionLogItem> obtainedCollectionLogItems)
	{
		this.obtainedCollectionLogItems = obtainedCollectionLogItems;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		Map<Integer, CollectionLogCategory> collectionLogCategoryItemMap = CollectionLogManager.getCollectionLogCategoryMap();

		for (CollectionLogCategory category : collectionLogCategoryItemMap.values())
		{
			addCollectionLogCategory(category);
		}

		collectionLogCategoryItemsPanels.get(0).setVisible(true);
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

		add(collectionLogCategoryHeader, BorderLayout.WEST);
		add(collectionLogCategoryItemsPanel);
	}
}

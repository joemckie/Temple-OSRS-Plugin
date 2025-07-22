package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.data.CollectionLogCategory;
import com.templeosrs.util.collections.data.CollectionLogItem;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;import javax.swing.JPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CollectionLogPlayerLookupResultPanel extends JPanel
{
	@Getter
	private final List<CollectionLogGridItemLabel> collectionLogGridItems = new ArrayList<>();

	private final List<CollectionLogCategoryItemsPanel> collectionLogCategoryItemsPanels = new ArrayList<>();

	private final GridBagConstraints c = new GridBagConstraints();

	@Getter
	private final Map<Integer, CollectionLogItem> obtainedCollectionLogItems;

	public CollectionLogPlayerLookupResultPanel(
		final String username,
		final Map<Integer, CollectionLogItem> obtainedCollectionLogItems
	)
	{
		this.obtainedCollectionLogItems = obtainedCollectionLogItems;

		setLayout(new GridBagLayout());

		Map<Integer, CollectionLogCategory> collectionLogCategoryItemMap = CollectionLogManager.getCollectionLogCategoryMap();

		final CollectionLogPlayerMetadataPanel collectionLogPlayerMetadataPanel = new CollectionLogPlayerMetadataPanel(
			username
		);

		c.insets = new Insets(4, 2, 4, 2);
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;

		add(collectionLogPlayerMetadataPanel, c);

		for (CollectionLogCategory category : collectionLogCategoryItemMap.values())
		{
			c.gridy++;

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

		final JPanel categoryContainer = new JPanel();
		categoryContainer.setLayout(new BoxLayout(categoryContainer, BoxLayout.Y_AXIS));

		categoryContainer.add(collectionLogCategoryHeader);
		categoryContainer.add(collectionLogCategoryItemsPanel);

		add(categoryContainer, c);
	}
}

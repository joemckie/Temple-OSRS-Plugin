package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.data.CollectionLogCategory;
import com.templeosrs.util.collections.data.CollectionLogItem;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CollectionLogPlayerLookupResultPanel extends JPanel
{
	@Getter
	private final List<CollectionLogGridItemLabel> collectionLogGridItems = new ArrayList<>();

	private final List<CollectionLogCategoryItemsPanel> collectionLogCategoryItemsPanels = new ArrayList<>();

	private final JPanel panel = new JPanel();

	@Getter
	private final Map<Integer, CollectionLogItem> obtainedCollectionLogItems;

	public CollectionLogPlayerLookupResultPanel(
		final String username,
		final Map<Integer, CollectionLogItem> obtainedCollectionLogItems
	)
	{
		this.obtainedCollectionLogItems = obtainedCollectionLogItems;

		setLayout(new BorderLayout());

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(new EmptyBorder(10, 0, 0, 0));

		Map<Integer, CollectionLogCategory> collectionLogCategoryItemMap = CollectionLogManager.getCollectionLogCategoryMap();

		final CollectionLogPlayerMetadataPanel collectionLogPlayerMetadataPanel = new CollectionLogPlayerMetadataPanel(
			username
		);

		add(collectionLogPlayerMetadataPanel, BorderLayout.NORTH);

		for (CollectionLogCategory category : collectionLogCategoryItemMap.values())
		{
			addCollectionLogCategory(category);
		}

		collectionLogCategoryItemsPanels.get(0).setVisible(true);

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
}

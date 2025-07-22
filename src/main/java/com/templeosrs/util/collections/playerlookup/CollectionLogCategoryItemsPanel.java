package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.data.CollectionLogItem;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CollectionLogCategoryItemsPanel extends JPanel
{
	@Getter
	private final List<CollectionLogGridItemLabel> categoryCollectionLogGridItems = new ArrayList<>();

	public CollectionLogCategoryItemsPanel(
		final Map<Integer, CollectionLogItem> categoryItems,
		final Map<Integer, CollectionLogItem> obtainedCollectionLogItems
	)
	{
		setLayout(new GridLayout(0, 4, 1, 1));
		setBorder(new EmptyBorder(10, 0, 0, 0));

		categoryCollectionLogGridItems.clear();

		categoryItems.values().forEach(
			categoryItem -> categoryCollectionLogGridItems.add(
				new CollectionLogGridItemLabel(
					obtainedCollectionLogItems.getOrDefault(categoryItem.getId(), categoryItem)
				)
			)
		);
	}

	public void repaintGrid()
	{
		removeAll();
		categoryCollectionLogGridItems.forEach(this::add);
	}
}

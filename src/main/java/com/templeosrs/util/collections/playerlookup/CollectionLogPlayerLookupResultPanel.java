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
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

@Slf4j
public class CollectionLogPlayerLookupResultPanel extends JPanel
{
	@Getter
	private final List<CollectionLogGridItemLabel> collectionLogGridItems = new ArrayList<>();

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
	}

	void addCollectionLogCategory(CollectionLogCategory category)
	{
		JLabel categoryLabel = new JLabel(category.getTitle());
		categoryLabel.setFont(FontManager.getRunescapeBoldFont());
		categoryLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel labelContainer = new JPanel(new BorderLayout());
		labelContainer.add(categoryLabel, BorderLayout.WEST);

		final CollectionLogCategoryItemsPanel collectionLogCategoryItemsPanel = new CollectionLogCategoryItemsPanel(
			category.getItems(),
			obtainedCollectionLogItems
		);

		collectionLogCategoryItemsPanel.repaintGrid();

		collectionLogGridItems.addAll(collectionLogCategoryItemsPanel.getCategoryCollectionLogGridItems());

		add(Box.createRigidArea(new Dimension(0, 5)));
		add(labelContainer);
		add(collectionLogCategoryItemsPanel);
	}
}

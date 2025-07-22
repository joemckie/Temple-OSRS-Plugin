package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.data.CollectionLogCategory;
import com.templeosrs.util.collections.data.CollectionLogItem;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;

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
		final int obtainedItemCount = category.obtainedItemCount(obtainedCollectionLogItems);
		final Color highlightColor = obtainedItemCount == 0
			? ColorScheme.PROGRESS_ERROR_COLOR
			: obtainedItemCount == category.getItems().size()
				? ColorScheme.PROGRESS_COMPLETE_COLOR
				: ColorScheme.PROGRESS_INPROGRESS_COLOR;

		JLabel categoryLabel = new JShadowedLabel();
		categoryLabel.setText(
			"<html>" +
			category.getTitle() +
			(
				obtainedItemCount > 0
					? "<br />" + obtainedItemCount + "/" + category.getItems().size()
					: ""
			) +
			"</html>"
		);
		categoryLabel.setFont(FontManager.getRunescapeBoldFont());
		categoryLabel.setForeground(highlightColor);
		categoryLabel.setBorder(new EmptyBorder(10, 0, 5, 0));

		JPanel labelContainer = new JPanel(new BorderLayout());
		labelContainer.add(categoryLabel, BorderLayout.WEST);
		labelContainer.setBorder(new MatteBorder(0, 0, 1, 0, highlightColor));

		final CollectionLogCategoryItemsPanel collectionLogCategoryItemsPanel = new CollectionLogCategoryItemsPanel(
			category.getItems(),
			obtainedCollectionLogItems
		);

		collectionLogCategoryItemsPanel.repaintGrid();

		collectionLogGridItems.addAll(collectionLogCategoryItemsPanel.getCategoryCollectionLogGridItems());

		add(labelContainer);
		add(collectionLogCategoryItemsPanel);
	}
}

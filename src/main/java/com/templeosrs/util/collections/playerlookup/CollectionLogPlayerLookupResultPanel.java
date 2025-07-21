package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.CollectionLogManager;
import com.templeosrs.util.collections.data.CollectionLogCategory;
import com.templeosrs.util.collections.data.ObtainedCollectionItem;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.MatteBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

@Slf4j
public class CollectionLogPlayerLookupResultPanel extends JPanel
{
	private final ItemManager itemManager;
	private final ClientThread clientThread;

	private final JPanel panelContent = new JPanel();

	private final Map<Integer, ObtainedCollectionItem> obtainedCollectionItems;

	@Inject
	public CollectionLogPlayerLookupResultPanel(
		ItemManager itemManager,
		ClientThread clientThread,
		Map<Integer, ObtainedCollectionItem> obtainedCollectionItems
	)
	{
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.obtainedCollectionItems = obtainedCollectionItems;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		panelContent.setLayout(new BoxLayout(panelContent, BoxLayout.Y_AXIS));

		JScrollPane scrollPane = new JScrollPane(this);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getVerticalScrollBar().setUnitIncrement(250);

		Map<Integer, CollectionLogCategory> collectionLogCategoryItemMap = CollectionLogManager.getCollectionLogCategoryMap();

		for (Map.Entry<Integer, CollectionLogCategory> entry : collectionLogCategoryItemMap.entrySet())
		{
			CollectionLogCategory category = entry.getValue();
			int categoryId = entry.getKey();

			addCollectionLogCategory(
				categoryId,
				category.getTitle(),
				category.getItems()
			);
		}

		add(panelContent);
	}

	@Inject
	void addCollectionLogCategory(
		int categoryId,
		String categoryTitle,
		Set<Integer> categoryItems
	)
	{
		JLabel categoryLabel = new JLabel(categoryTitle);
		categoryLabel.setFont(FontManager.getRunescapeBoldFont());
		categoryLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel labelContainer = new JPanel(new BorderLayout());
		labelContainer.add(categoryLabel, BorderLayout.WEST);

		GridPanel gridPanel = new GridPanel(itemManager, clientThread, categoryItems, obtainedCollectionItems);

		panelContent.add(Box.createRigidArea(new Dimension(0, 5)));
		panelContent.add(labelContainer);
		panelContent.add(gridPanel);
	}
}

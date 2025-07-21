package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.data.ObtainedCollectionItem;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

@Slf4j
public class GridPanel extends JPanel
{
	public GridPanel(
		ItemManager itemManager,
		ClientThread clientThread,
		Set<Integer> categoryItems,
		Map<Integer, ObtainedCollectionItem> obtainedCollectionItems
	)
	{
		final int itemsPerRow = 3;
		final int rowSize = (int)Math.ceil((double)categoryItems.size() / itemsPerRow);

		setLayout(new GridLayout(rowSize, itemsPerRow, 2, 2));
		setBorder(new EmptyBorder(0, 0, 0, 0));

		for (int itemId : categoryItems)
		{
			final JPanel container = new JPanel(new BorderLayout());

			container.add(new GridItem(itemManager, clientThread, itemId, obtainedCollectionItems.get(itemId)));

			add(container);
		}
	}
}

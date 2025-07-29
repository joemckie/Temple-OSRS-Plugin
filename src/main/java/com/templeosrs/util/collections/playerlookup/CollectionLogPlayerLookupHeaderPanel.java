package com.templeosrs.util.collections.playerlookup;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

public class CollectionLogPlayerLookupHeaderPanel extends JPanel
{
	private IconTextField searchBar;

	public CollectionLogPlayerLookupHeaderPanel()
	{
		super(new BorderLayout());

		searchBar = new IconTextField();
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.setMinimumSize(new Dimension(0, 30));
		searchBar.addActionListener(e -> { });
		searchBar.addMouseListener(
			new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() != 2)
					{
						return;
					}

//					Player localPlayer = client.getLocalPlayer();
//
//					if (localPlayer != null)
//					{
//
//					}
				}
			}
		);
		searchBar.addClearListener(
			() ->
			{
				searchBar.setIcon(IconTextField.Icon.SEARCH);
				searchBar.setEditable(true);
			}
		);

		add(searchBar, BorderLayout.NORTH);
	}
}

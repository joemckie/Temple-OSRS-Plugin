package com.templeosrs.ui.ranks;

import com.templeosrs.TempleOSRSPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TempleActivitySortFilter extends JPanel
{
	private final JLabel icon;

	boolean increasing;

	TempleActivitySortFilter(String text)
	{
		icon = new JLabel();

		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());

		setLayout(new GridLayout(0, 2));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		/* on mouse-event, flip ascending/ descending icon */
		addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent me)
			{
				increasing = !increasing;
				icon.setIcon(new ImageIcon(increasing ? ImageUtil.loadImageResource(TempleOSRSPlugin.class, "sorting/up.png") : ImageUtil.loadImageResource(TempleOSRSPlugin.class, "sorting/down.png")));
			}
		});

		add(label);
		add(icon);
	}

	void reset()
	{
		icon.setIcon(null);
	}
}

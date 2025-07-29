package com.templeosrs.util.collections.playerlookup;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;

public class CollectionLogPlayerMetadataPanel extends JPanel
{
	public CollectionLogPlayerMetadataPanel(final String username)
	{
		super(new BorderLayout());

		setOpaque(true);
		setBorder(
			new CompoundBorder(
				new MatteBorder(1, 1, 1, 1, ColorScheme.BORDER_COLOR),
				new EmptyBorder(5, 5, 5, 5)
			)
		);
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
	}
}

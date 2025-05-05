package com.templeosrs.ui.clans;

import com.templeosrs.TempleOSRSPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static net.runelite.client.ui.PluginPanel.PANEL_WIDTH;

public class TempleClanMember extends JPanel
{
	TempleClanMember(TempleOSRSPlugin plugin, String name, Color color)
	{
		setLayout(new BorderLayout());
		setBackground(color);

		JPanel row = new JPanel();
		row.setLayout(new GridLayout());
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 5, 2, 0));
		row.setPreferredSize(new Dimension(PANEL_WIDTH, 25));

		/* add username to row */
		JLabel username = new JLabel(name);
		username.setFont(FontManager.getRunescapeSmallFont());
		username.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		row.add(username);

		/* add on-click menu to row */
		JPopupMenu menu = new JPopupMenu();

		JMenuItem lookupPlayer = new JMenuItem();
		lookupPlayer.setText("<html>Lookup <span style='color:#6ee16e'>" + name + "</span></html>");
		lookupPlayer.addActionListener(e -> plugin.fetchUser(name));
		menu.add(lookupPlayer);

		/* show lookup player menu option on click mouse-event */
		row.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				menu.show(row, e.getX(), e.getY());
			}
		});

		add(row);
	}
}

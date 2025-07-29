package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.data.CollectionLogItem;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;import net.runelite.client.util.AsyncBufferedImage;

@Slf4j
public class CollectionLogGridItemLabel extends JLabel
{
	private final CollectionLogItem item;

	private static final Color OBTAINED_BACKGROUND = new Color(0, 70, 0);
	private static final Color UNOBTAINED_BACKGROUND = new Color(90, 0, 0);

	public CollectionLogGridItemLabel(CollectionLogItem item)
	{
		super("");

		this.item = item;

		this.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		setOpaque(true);

		setVerticalAlignment(SwingConstants.CENTER);
		setHorizontalAlignment(SwingConstants.CENTER);

		setToolTipText(buildTooltipText());

		this.addMouseListener(
			new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					final CollectionLogGridItemLabel item = (CollectionLogGridItemLabel) e.getSource();
					item.setBackground(getBackgroundColor().brighter());
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					final CollectionLogGridItemLabel item = (CollectionLogGridItemLabel) e.getSource();
					item.setBackground(getBackgroundColor());
				}
			}
		);
	}

	public void updateIcon(ItemManager itemManager)
	{
		final AsyncBufferedImage img = itemManager.getImage(
			item.getId(),
			item.getQuantityObtained(),
			item.getQuantityObtained() > 1
		);

		setBackground(getBackgroundColor());

		img.addTo(this);
	}

	private Color getBackgroundColor()
	{
		if (item.isObtained())
		{
			return OBTAINED_BACKGROUND;
		}

		return UNOBTAINED_BACKGROUND;
	}

	private String buildTooltipText()
	{
		final StringBuilder sb = new StringBuilder("<html>");

		final String itemInfo = String.format("%s", item.getName());

		sb.append(itemInfo);

		if (item.isObtained())
		{
			final String obtainedInfo = String.format(
				"<br />---" +
				"<br />Quantity: %s" +
				"<br />Obtained on %s",
				item.getQuantityObtained(),
				DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(item.getDateObtained())
			);

			sb.append(obtainedInfo);
		}

		sb.append("</html>");

		return sb.toString();
	}
}

package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.data.CollectionLogItem;
import java.awt.Color;
import java.text.SimpleDateFormat;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.AsyncBufferedImage;

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
	}

	public void updateIcon(CollectionLogPlayerLookupPanel playerLookupPanel)
	{
		final AsyncBufferedImage img = playerLookupPanel
			.getItemManager()
			.getImage(item.getId(), item.getQuantityObtained(), item.getQuantityObtained() > 1);

		setBackground(buildBackground());

		img.addTo(this);
	}

	private Color buildBackground()
	{
		if (item.isObtained())
		{
			return OBTAINED_BACKGROUND;
		}

		return UNOBTAINED_BACKGROUND;
	}

	private String buildTooltipText()
	{
		String tooltip = "<html>";

		tooltip += item.getName();

		if (item.isObtained())
		{
			final String formattedObtainedDate = new SimpleDateFormat("MM/dd/yyyy").format(item.getDateObtained());

			tooltip += "<br />---";
			tooltip += "<br />Quantity: " + item.getQuantityObtained();
			tooltip += "<br />Obtained at: " + formattedObtainedDate;
		}

		tooltip += "</html>";

		return tooltip;
	}
}

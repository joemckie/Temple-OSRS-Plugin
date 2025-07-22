package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.data.CollectionLogCategory;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;

public class CollectionLogCategoryLabel extends JPanel
{
	final int obtainedItemCount;
	final Color highlightColor;
	final CollectionLogCategory category;

	public CollectionLogCategoryLabel(
		CollectionLogPlayerLookupResultPanel collectionLogPlayerLookupResultPanel,
		final CollectionLogCategory category
	)
	{
		super(new BorderLayout());

		this.category = category;
		this.obtainedItemCount = category.obtainedItemCount(
			collectionLogPlayerLookupResultPanel.getObtainedCollectionLogItems()
		);
		this.highlightColor = obtainedItemCount == 0
			? ColorScheme.PROGRESS_ERROR_COLOR
			: obtainedItemCount == category.getItems().size()
				? ColorScheme.PROGRESS_COMPLETE_COLOR
				: ColorScheme.PROGRESS_INPROGRESS_COLOR;

		final JLabel labelText = new LabelText();

		add(labelText, BorderLayout.WEST);
		setBorder(new MatteBorder(0, 0, 1, 0, highlightColor));
	}

	private class LabelText extends JShadowedLabel
	{
		public LabelText()
		{
			setText(
				String.format(
					"<html>" +
					"%s<br />" +
					"%s/%s" +
					"</html>",
					category.getTitle(), obtainedItemCount, category.getItems().size()
				)
			);
			setFont(FontManager.getRunescapeBoldFont());
			setForeground(highlightColor);
			setBorder(new EmptyBorder(10, 0, 5, 0));
		}
	}
}

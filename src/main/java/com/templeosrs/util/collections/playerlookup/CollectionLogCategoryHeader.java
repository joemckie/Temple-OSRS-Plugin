package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.data.CollectionLogCategory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;
import net.runelite.client.util.SwingUtil;

public class CollectionLogCategoryHeader extends JPanel
{
	final int obtainedItemCount;
	final Color highlightColor;
	final CollectionLogCategory category;
	final CollectionLogCategoryItemsPanel linkedItemsPanel;

	public CollectionLogCategoryHeader(
		CollectionLogPlayerLookupResultPanel collectionLogPlayerLookupResultPanel,
		CollectionLogCategoryItemsPanel linkedItemsPanel,
		final CollectionLogCategory category
	)
	{
		super();

		this.linkedItemsPanel = linkedItemsPanel;
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
		final JButton collapseButton = new CollapseButton();

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		add(collapseButton, BorderLayout.WEST);
		add(Box.createRigidArea(new Dimension(7, 0)));
		add(labelText, BorderLayout.WEST);
	}

	private class CollapseButton extends JButton
	{
		public CollapseButton()
		{
			super("+");

			SwingUtil.removeButtonDecorations(this);

			setUI(new BasicButtonUI());
			setLayout(new BorderLayout());
			setMaximumSize(new Dimension(24, 24));
			setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			setHorizontalAlignment(SwingConstants.CENTER);
			setVerticalAlignment(SwingConstants.CENTER);

			addMouseListener(
				new MouseAdapter()
				{
					@Override
					public void mousePressed(MouseEvent mouseEvent)
					{
						toggleSelected();
						linkedItemsPanel.toggleVisibility();
					}
				}
			);
		}

		public void toggleSelected()
		{
			setSelected(!isSelected());

			if (isSelected())
			{
				setText("-");
			}
			else
			{
				setText("+");
			}
		}
	}

	private class LabelText extends JShadowedLabel
	{
		public LabelText()
		{
			super(
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

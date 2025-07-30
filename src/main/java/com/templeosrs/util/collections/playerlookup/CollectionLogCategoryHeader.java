package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.data.CollectionLogCategory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

public class CollectionLogCategoryHeader extends JPanel
{
	final int obtainedItemCount;
	final Color highlightColor;
	final CollectionLogCategory category;
	final CollectionLogCategoryItemsPanel linkedItemsPanel;
	final SpriteManager spriteManager;
	final JButton collapseButton;

	@Getter
	@Setter
	private boolean expanded;

	public CollectionLogCategoryHeader(
		CollectionLogPlayerLookupResultPanel collectionLogPlayerLookupResultPanel,
		CollectionLogCategoryItemsPanel linkedItemsPanel,
		final CollectionLogCategory category,
		final SpriteManager spriteManager,
		final boolean isExpanded
	)
	{
		super();

		this.expanded = isExpanded;
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
		this.spriteManager = spriteManager;

		final JLabel labelText = new LabelText();

		collapseButton = new CollapseButton();

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		add(collapseButton, BorderLayout.WEST);
		add(Box.createRigidArea(new Dimension(7, 0)));
		add(labelText, BorderLayout.WEST);
	}

	public void toggleExpanded()
	{
		final boolean nextState = !isExpanded();

		setExpanded(nextState);

		collapseButton.setSelected(nextState);
		collapseButton.setText(
			nextState ? "-" : "+"
		);
		linkedItemsPanel.setVisible(nextState);
	}

	private class CollapseButton extends JButton
	{
		public CollapseButton()
		{
			super(expanded ? "-" : "+");

			SwingUtil.removeButtonDecorations(this);

			setUI(new BasicButtonUI());
			setLayout(new BorderLayout());
			setMaximumSize(new Dimension(24, 24));
			setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			setHorizontalAlignment(SwingConstants.CENTER);
			setVerticalAlignment(SwingConstants.CENTER);
			setSelected(expanded);

			addMouseListener(
				new MouseAdapter()
				{
					@Override
					public void mousePressed(MouseEvent mouseEvent)
					{
						toggleExpanded();
					}
				}
			);
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
			setVerticalAlignment(SwingConstants.CENTER);
			setIconTextGap(7);

			spriteManager.getSpriteAsync(
				SpriteID.HISCORE_ARAXXOR,
				0,
				(sprite) ->
					SwingUtilities.invokeLater(
						() ->
							{
								// Icons are all 25x25 or smaller, so they're fit into a 25x25 canvas to give them a consistent size for
								// better alignment. Further, they are then scaled down to 20x20 to not be overly large in the panel.
								final BufferedImage scaledSprite = ImageUtil.resizeImage(ImageUtil.resizeCanvas(sprite, 25, 25), 20, 20);
								setIcon(new ImageIcon(scaledSprite));
							}
						)
					);
		}
	}
}

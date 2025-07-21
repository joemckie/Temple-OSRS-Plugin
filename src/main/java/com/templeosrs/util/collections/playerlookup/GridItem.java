package com.templeosrs.util.collections.playerlookup;

import com.templeosrs.util.collections.data.ObtainedCollectionItem;
import java.awt.BorderLayout;
import java.awt.Color;import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Constants;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

@Slf4j public class GridItem extends JPanel
{
	private final JPanel mainContainer = new JPanel(new BorderLayout());
	private final JPanel overlayPanel = new JPanel(new BorderLayout());
	private final JPanel imagePanel = new JPanel(new BorderLayout());
	private final JLayeredPane layeredPane = new JLayeredPane();

	public GridItem(
		ItemManager itemManager,
		ClientThread clientThread,
		int itemId,
		@Nullable ObtainedCollectionItem obtainedCollectionItem
	)
	{
		final boolean isObtained = obtainedCollectionItem != null;
		final int obtainedCount = isObtained ? obtainedCollectionItem.getCount() : 0;

		clientThread.invokeLater(() ->
		{
			final AsyncBufferedImage img = itemManager.getImage(itemId);
			final String itemName = itemManager.getItemComposition(itemId).getName();

			mainContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			mainContainer.setToolTipText(itemName);

			img.onLoaded(() ->
			{
				final BufferedImage image = ImageUtil.resizeImage(
					img,
					Constants.ITEM_SPRITE_WIDTH,
					Constants.ITEM_SPRITE_HEIGHT
				);

				final Dimension imageDimension = image.getData().getBounds().getSize();

				setLayout(new BorderLayout());
				setBorder(new EmptyBorder(0, 0, 0, 0));

				layeredPane.setSize(new Dimension(100, 100));

				imagePanel.setOpaque(false);
				imagePanel.setPreferredSize(new Dimension((int)(imageDimension.width * 1.5), (int)(imageDimension.height * 1.5)));

				final JLabel itemImage = new JLabel(new ImageIcon(image));
				itemImage.setPreferredSize(imageDimension);

				overlayPanel.setOpaque(false);
				overlayPanel.setSize(new Dimension(100, 100));

				if (obtainedCount > 0)
				{
					final JLabel obtainedCountLabel = new JShadowedLabel(String.valueOf(obtainedCount));
					obtainedCountLabel.setHorizontalAlignment(SwingConstants.LEFT);
					obtainedCountLabel.setBorder(new EmptyBorder(5, 8, 0, 0));
					obtainedCountLabel.setFont(FontManager.getRunescapeSmallFont());
					obtainedCountLabel.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
					obtainedCountLabel.setOpaque(false);

					overlayPanel.add(obtainedCountLabel, BorderLayout.NORTH);
				}
				else
				{
					final JPanel opacityOverlay = new JPanel();
					opacityOverlay.setBackground(new Color(0, 0, 0, 127));

					overlayPanel.add(opacityOverlay, BorderLayout.CENTER);
				}

				layeredPane.add(overlayPanel, JLayeredPane.DEFAULT_LAYER);

				imagePanel.add(layeredPane, BorderLayout.CENTER);
				imagePanel.add(itemImage, BorderLayout.CENTER);

				mainContainer.add(imagePanel, BorderLayout.CENTER);

				add(mainContainer);
			});
		});
	}
}

package com.templeosrs.util.collections.utils;

public class PlayerNameUtils
{
	/**
	 * Normalises the player's name by removing Ironman prefixes and formatting.
	 * @param playerName The player name to normalise.
	 * @return The normalised player name.
	 */
	public static String normalizePlayerName(String playerName)
	{
		if (playerName == null)
		{
			return "";
		}

		// Replace non-breaking space and other unusual whitespace with normal space
		String normalizedName = playerName.replace('\u00A0', ' ')
			.replaceAll("\\s+", " ")
			.trim();

		// Remove known Ironman prefixes
		String[] ironmanPrefixes = {
			"Ultimate Ironman",
			"Hardcore Ironman",
			"Ironman"
		};

		for (String prefix : ironmanPrefixes)
		{
			if (normalizedName.startsWith(prefix))
			{
				normalizedName = normalizedName.replaceFirst(prefix, "").trim();
				break;
			}
		}

		// Remove <img=xx> tags
		normalizedName = normalizedName.replaceAll("<img=\\d+>", "").trim();

		// Replace spaces with underscores and lowercase
		normalizedName = normalizedName.replace(' ', '_').toLowerCase();

		return normalizedName;
	}
}
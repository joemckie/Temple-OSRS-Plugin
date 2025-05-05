package com.templeosrs.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum CurrentTopRanges
{
	Day("Day", "day"),
	Week("Week", "week"),
	Month("Month", "month");

	private static final Map<String, PlayerRanges> lookup = new HashMap<>();

	static
	{
		for (PlayerRanges r : PlayerRanges.values())
		{
			lookup.put(r.getName(), r);
		}
	}

	private final String name;
	private final String range;

	public String getName()
	{
		return this.name;
	}

}

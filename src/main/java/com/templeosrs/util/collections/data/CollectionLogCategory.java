package com.templeosrs.util.collections.data;

import java.util.Map;
import lombok.Value;

@Value
public class CollectionLogCategory
{
	String title;
	Map<Integer, CollectionLogItem> items;

	public int obtainedItemCount(Map<Integer, CollectionLogItem> obtainedCollectionLogItems)
	{
		return items.values()
			.stream()
			.mapToInt(item -> obtainedCollectionLogItems.containsKey(item.getId()) ? 1 : 0)
			.sum();
	}
}

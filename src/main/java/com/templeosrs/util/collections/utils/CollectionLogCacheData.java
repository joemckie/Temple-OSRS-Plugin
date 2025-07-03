package com.templeosrs.util.collections.utils;

import java.util.Map;
import java.util.Set;
import lombok.Value;

@Value
public class CollectionLogCacheData
{
	Set<Integer> itemIds;
	Map<Integer, Set<Integer>> categoryItems;
	Map<String, Integer> categoryStructIds;
	Map<Integer, Set<String>> categorySlugs;
}

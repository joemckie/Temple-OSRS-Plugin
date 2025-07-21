package com.templeosrs.util.collections.utils;

import com.templeosrs.util.collections.data.CollectionLogCategory;import lombok.Value;

import java.util.Map;
import java.util.Set;

@Value
public class CollectionLogCacheData {
    Set<Integer> itemIds;
    Map<Integer, CollectionLogCategory> categories;
    Map<String, Integer> categoryStructIds;
    Map<Integer, Set<String>> categorySlugs;
}

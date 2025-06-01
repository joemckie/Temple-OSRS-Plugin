package com.templeosrs.util.collections.utils;

import lombok.Value;

import java.util.Map;
import java.util.Set;

@Value
public class CollectionLogCacheData {
    Set<Integer> itemIds;
    Map<Integer, Set<Integer>> categoryItems;
}

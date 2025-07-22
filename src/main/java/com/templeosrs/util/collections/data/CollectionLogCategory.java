package com.templeosrs.util.collections.data;

import java.util.Map;
import lombok.Value;

@Value
public class CollectionLogCategory
{
	String title;
	Map<Integer, CollectionLogItem> items;
}

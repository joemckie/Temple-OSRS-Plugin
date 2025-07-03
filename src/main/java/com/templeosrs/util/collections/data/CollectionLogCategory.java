package com.templeosrs.util.collections.data;

import java.util.Set;
import lombok.Value;

@Value
public class CollectionLogCategory
{
	String title;
	Set<Integer> items;
}

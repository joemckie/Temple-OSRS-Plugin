package com.templeosrs.util.collections.data;

import lombok.Value;

import java.util.Set;

@Value
public class CollectionLogCategory {
    String title;
    Set<Integer> items;
}

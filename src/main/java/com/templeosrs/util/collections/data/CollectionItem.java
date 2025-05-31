package com.templeosrs.util.collections.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class CollectionItem {
    private int categoryId;
    private int itemId;
}
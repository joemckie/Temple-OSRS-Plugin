package com.templeosrs.util.collections.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ObtainedCollectionItem extends CollectionItem {
    private int count;

    @Nullable
    private Timestamp date;
}

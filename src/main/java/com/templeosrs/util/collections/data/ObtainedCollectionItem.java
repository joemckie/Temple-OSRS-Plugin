package com.templeosrs.util.collections.data;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;

@Value
public class ObtainedCollectionItem {
    int id;
    String name;
    int count;
    @Nullable String date;

    public Timestamp getDate()
    {
        if (this.date == null) {
            return null;
        }

        return Timestamp.valueOf(this.date);
    }
}

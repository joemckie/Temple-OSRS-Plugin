package com.templeosrs.util.collections.autosync;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ObtainedItem {
    String name;
    int id;
    int count;
}

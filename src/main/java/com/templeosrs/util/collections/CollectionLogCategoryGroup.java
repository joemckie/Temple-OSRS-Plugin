package com.templeosrs.util.collections;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * All available category groups (i.e. tabs) mapped to their in-game struct IDs
 */
@RequiredArgsConstructor
public enum CollectionLogCategoryGroup {
    bosses(471),
    raids(472),
    clues(473),
    minigames(474),
    other(475);

    @Getter
    private final int structId;
}
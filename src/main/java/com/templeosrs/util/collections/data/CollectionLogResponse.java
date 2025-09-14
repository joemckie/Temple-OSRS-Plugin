package com.templeosrs.util.collections.data;

import com.google.gson.annotations.SerializedName;
import com.templeosrs.util.api.APIError;
import java.util.Set;
import lombok.Value;

@Value
public class CollectionLogResponse
{
	Data data;
	
	APIError error;

	@Value
	public static class Data
	{
		String player;
		@SerializedName("last_changed")
		String lastChanged;
		Set<ObtainedCollectionItem> items;
	}
}

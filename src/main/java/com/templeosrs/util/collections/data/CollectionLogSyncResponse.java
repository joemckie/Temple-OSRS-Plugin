package com.templeosrs.util.collections.data;

import com.google.gson.annotations.SerializedName;
import com.templeosrs.util.api.APIError;
import javax.annotation.Nullable;
import lombok.Value;

@Value
public class CollectionLogSyncResponse
{
	@Nullable
	Data data;

	@Nullable
	APIError error;

	@Value
	public static class Data
	{
		@SerializedName("sync_date")
		String syncDate;

		@SerializedName("sync_date_unix")
		int syncDateUnix;

		@SerializedName("last_changed")
		String lastChanged;

		@SerializedName("last_changed_unix")
		int lastChangedUnix;

		String username;

		String message;
	}
}

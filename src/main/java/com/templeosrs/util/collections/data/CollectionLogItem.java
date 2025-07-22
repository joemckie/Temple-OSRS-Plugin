package com.templeosrs.util.collections.data;

import com.google.gson.annotations.SerializedName;
import java.sql.Timestamp;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class CollectionLogItem
{
	int id;
	String name;
	@SerializedName("count") int quantityObtained;
	@SerializedName("date") String dateObtained;

	public CollectionLogItem(int id)
	{
		this.id = id;
		this.name = null;
		this.dateObtained = null;
		this.quantityObtained = 0;
	}

	public CollectionLogItem(int id, int quantityObtained)
	{
		this.id = id;
		this.name = null;
		this.quantityObtained = quantityObtained;
		this.dateObtained = null;
	}

	public CollectionLogItem(int id, @NotNull String name, int quantityObtained)
	{
		this.id = id;
		this.name = name;
		this.quantityObtained = quantityObtained;
		this.dateObtained = null;
	}

	public CollectionLogItem(int id, @NotNull String name, int quantityObtained, String dateObtained)
	{
		this.id = id;
		this.name = name;
		this.quantityObtained = quantityObtained;
		this.dateObtained = dateObtained;
	}

	public Timestamp getDateObtained()
	{
		if (this.dateObtained == null)
		{
			return null;
		}

		return Timestamp.valueOf(this.dateObtained);
	}

	public boolean isObtained()
	{
		return quantityObtained > 0;
	}
}

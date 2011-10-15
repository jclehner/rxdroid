package at.caspase.rxdroid.db;

import java.io.Serializable;
import java.util.Collection;

import android.util.Log;

import com.j256.ormlite.field.DatabaseField;

/**
 * Base class for all database entries.
 *
 * The main purpose of this class is to provide alleviate child classes from
 * declaring an ID field and to provide an unimplemented equals() method.
 *
 * @author Joseph Lehner
 *
 */
public abstract class Entry implements Serializable
{
	private static final long serialVersionUID = 8300191193261799857L;

	public static final String COLUMN_ID = "id";

	@DatabaseField(columnName = COLUMN_ID, generatedId = true)
	protected int id;

	@Override
	public abstract boolean equals(Object other);

	@Override
	public abstract int hashCode();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public static<T extends Entry> T findInCollection(Collection<T> collection, int id)
	{
		for(T t : collection)
		{
			if(t.getId() == id)
				return t;
		}

		return null;
	}
}


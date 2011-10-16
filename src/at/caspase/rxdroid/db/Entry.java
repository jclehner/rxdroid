/**
 * Copyright (C) 2011 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 * This file is part of RxDroid.
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

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

	/**
	 * Returns the auto-generated database ID.
	 */
	public final int getId() {
		return id;
	}

	/**
	 * Sets the database ID.
	 * <p>
	 * As the ID is automatically generated, there's no need to use this function,
	 * apart from a few special cases.
	 */
	public final void setId(int id) {
		this.id = id;
	}
	
	/**
	 * Converts this Entry to an instance usable by the current database version.
	 */
	public Entry convert() {
		throw new UnsupportedOperationException();
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


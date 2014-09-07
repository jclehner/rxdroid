/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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

package at.jclehner.rxdroid.db;

import java.lang.reflect.Field;

import at.jclehner.androidutils.Reflect;
import at.jclehner.rxdroid.db.DatabaseHelper.DatabaseError;

import com.j256.ormlite.field.DatabaseField;

/**
 * Base class for all database entries.
 * <p>
 * The main purpose of this class is to alleviate child classes from
 * declaring an ID field and to force descendants to declare {@link #equals(Object)} and
 * {@link #hashCode()}.
 * <p>
 * Also note that you may define hooks for descendant classes that are called when
 * the database is modified. A hook has the name HOOK_&lt;db action name&gt; (see
 * Drug docs for valid action names). The following is a sample hook implementation:
 *
 *
 * @see
 *
 * @author Joseph Lehner
 *
 */
public abstract class Entry
{
	interface Callback<E extends Entry>
	{
		void call(E entry);
	}

	@DatabaseField(generatedId = true)
	protected int id = -1;

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
	protected Entry convertToCurrentDatabaseFormat() {
		throw new UnsupportedOperationException();
	}

	protected static void copy(Entry dest, Entry src)
	{
		Class<? extends Entry> clsD = dest.getClass();
		Class<? extends Entry> clsS = src.getClass();

		for(Field fS : Reflect.getAllFieldsUpTo(Entry.class, clsS))
		{
			if(fS.isAnnotationPresent(DatabaseField.class))
			{
				Field fD = Reflect.getDeclaredField(clsD, fS.getName());
				if(fD != null)
				{
					try
					{
						fD.setAccessible(true);
						fS.setAccessible(true);
						fD.set(dest, fS.get(src));
					}
					catch(IllegalArgumentException e)
					{
						throw new DatabaseError(DatabaseError.E_GENERAL, e);
					}
					catch(IllegalAccessException e)
					{
						throw new DatabaseError(DatabaseError.E_GENERAL, e);
					}
				}
			}
		}

		dest.id = src.id;
	}
}


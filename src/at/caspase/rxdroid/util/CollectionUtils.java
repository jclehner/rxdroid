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

package at.caspase.rxdroid.util;

import java.util.Collection;

/**
 * Utility functions for dealing with java <code>Collections</code>.
 *
 * @author Joseph Lehner
 */
public final class CollectionUtils
{
	/**
	 * Interface for filtering collections based on specific criteria.
	 *
	 * @author Joseph Lehner
	 */
	public interface Filter<E>
	{
		/**
		 * Returns <code>true</code> if the given entry has passed the filter.
		 * <p>
		 * Returning <code>false</code> here leads to this entry not being
		 * included in the <code>Collection</code> returned by filter().
		 */
		boolean matches(E e);
	}

	public static<E> Collection<E> filter(final Collection<E> collection, Filter<E> filter)
	{
		final Collection<E> filtered = create(collection);

		for(E e : collection)
		{
			if(filter.matches(e))
				filtered.add(e);
		}

		return filtered;
	}

	public static<E> Collection<E> copy(final Collection<E> collection)
	{
		final Collection<E> cloned = create(collection);
		cloned.addAll(collection);
		return cloned;
	}

	@SuppressWarnings("unchecked")
	public static<E> Collection<E> create(final Collection<E> collection)
	{
		if(collection == null)
			throw new IllegalArgumentException("Supplied argument was null");

		try
		{
			return collection.getClass().newInstance();
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException(collection.getClass().getName() + " lacks a visible default constructor", e);
		}
	}

	public static<T> int indexOf(T e, T[] array)
	{
		for(int i = 0; i != array.length; ++i)
		{
			if(array[i].equals(e))
				return i;
		}

		return -1;
	}

	public static int indexOf(int n, int[] array)
	{
		for(int i = 0; i != array.length; ++i)
		{
			if(array[i] == n)
				return i;
		}

		return -1;
	}

	private CollectionUtils() {}
}

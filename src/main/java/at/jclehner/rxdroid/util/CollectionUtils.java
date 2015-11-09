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

package at.jclehner.rxdroid.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import at.jclehner.androidutils.Reflect;

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

	public static <E> Collection<E> filter(final Collection<E> collection, Filter<E> filter)
	{
		final Collection<E> filtered = create(collection);

		for(E e : collection)
		{
			if(filter.matches(e))
				filtered.add(e);
		}

		return filtered;
	}

	public static <E> List<E> filter(final List<E> list, Filter<E> filter)
	{
		final List<E> filtered = new ArrayList<>();
		for(E e : list)
		{
			if(filter.matches(e))
				filtered.add(e);
		}
		return filtered;
	}

	public static <E> Collection<E> copy(final Collection<E> collection)
	{
		final Collection<E> cloned = create(collection);
		cloned.addAll(collection);
		return cloned;
	}

	@SuppressWarnings("unchecked")
	public static <E> Collection<E> create(final Collection<E> collection)
	{
		if(collection == null)
			throw new NullPointerException();

		return Reflect.newInstance(collection.getClass());
	}

	public static <T> int indexOf(T e, T[] array)
	{
		for(int i = 0; i != array.length; ++i)
		{
			if(array[i].equals(e))
				return i;
		}

		return -1;
	}

	public static <T> boolean contains(T[] array, T e) {
		return indexOf(e, array) != -1;
	}

	public static <T> int indexOfByReference(T e, T[] array)
	{
		for(int i = 0; i != array.length; ++i)
		{
			if(array[i] == e)
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

	public static char[] toCharArray(Object[] array)
	{
		final int s = array.length;
		final char[] ret = new char[s];
		for(int i = 0; i != s; ++i)
			ret[i] = (Character) array[i];
		return ret;
	}

	public static boolean getRandomPermutation(StringBuilder sb)
	{
		for(int i = 0; i != sb.length(); ++i)
		{
			if(!getNextPermutation(sb))
				return false;
		}

		return true;
	}

	public static boolean getNextPermutation(StringBuilder sb)
	{
		List<Character> list = asList(sb.toString().toCharArray());
		boolean ret = getNextPermutation(list);

		Object[] array = list.toArray();
		sb.delete(0, sb.length());
		sb.append(toCharArray(array));

		return ret;
	}

	/**
	 * Get the next permutation.
	 * <p>
	 * This function's implementation is based on the implementation found in
	 * the GNU ISO C++ library's implementation of <code>std::next_permutation()</code>.
	 *
	 */
	public static <E extends Comparable<E>> boolean getNextPermutation(List<E> list)
	{
		final int first = 0;
		final int last = list.size();

		if(first == last)
			return false;

		int i = first;
		++i;

		if(i == last)
			return false;

		i = last;
		--i;

		for(;;)
		{
			int ii = i;
			--i;

			if(list.get(i).compareTo(list.get(ii)) < 0)
			{
				int j = last;
				while(!(list.get(i).compareTo(list.get(--j)) < 0))
					; // empty body

				swap(list, i, j);
				reverse(list, ii, last);

				return true;
			}
			if(i == first)
			{
				Collections.reverse(list);
				return false;
			}
		}
	}

	public static List<Integer> asList(int[] array)
	{
		List<Integer> list = new ArrayList<Integer>(array.length);

		for(int i = 0; i != array.length; ++i)
			list.add(array[i]);

		return list;
	}

	public static List<Character> asList(char[] array)
	{
		List<Character> list = new ArrayList<Character>(array.length);

		for(int i = 0; i != array.length; ++i)
			list.add(array[i]);

		return list;
	}

	public static <E> void swap(List<E> list, int i, int j)
	{
		E tmp = list.get(i);
		list.set(i, list.get(j));
		list.set(j, tmp);
	}

	public static <E> void reverse(List<E> list, final int begin, final int end)
	{
		if(end <= begin)
			throw new IllegalArgumentException("end <= begin");

		if(end - begin == 1)
			return;

		List<E> reversed = new ArrayList<E>(end - begin);

		for(int i = begin; i != end; ++i)
		{
			final int j = begin + (end - i - 1);
			reversed.add(list.get(j));
		}

		for(int i = 0; i != reversed.size(); ++i)
			list.set(begin + i, reversed.get(i));
	}

	private CollectionUtils() {}
}

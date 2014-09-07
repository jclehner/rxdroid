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

public class SimpleBitSet
{
	private long mValue;

	public SimpleBitSet(long value) {
		mValue = value;
	}

	public void set(long value) {
		mValue = value;
	}

	public void set(int n, boolean value)
	{
		if(value)
			mValue |= 1 << n;
		else
			mValue ^= 1 << n;
	}

	public boolean get(int n) {
		return (mValue & 1 << n) != 0;
	}

	public int cardinality() {
		return Long.bitCount(mValue);
	}

	public long longValue() {
		return mValue;
	}

	public boolean[] toBooleanArray() {
		return toBooleanArray(64);
	}

	public boolean[] toBooleanArray(int size) {
		return toBooleanArray(mValue, size);
	}

	public static boolean[] toBooleanArray(long value, int size)
	{
		boolean[] ret = new boolean[size];

		for(int i = 0; i != size; ++i)
			ret[i] = (value & 1 << i) != 0;

		return ret;
	}
}

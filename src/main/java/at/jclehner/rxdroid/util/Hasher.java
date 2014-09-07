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

import java.lang.reflect.Array;

/**
 * Helper class for writing hashCode methods.
 *
 * The design is essentially copied from the Android SDK's javadoc.
 *
 * Usage example:
 * <pre>
 *
 * // ...
 * &#64;Override
 * public int hashCode()
 * {
 *     Hasher hasher = new Hasher();
 *
 *     hasher.hash(mMember1);
 *     hasher.hash(mMember2);
 *     // ...
 *     hasher.hash(mMemberX);
 *
 *     return hasher.getHashCode();
 * }
 * // ...
 * </pre>
 *
 * @author Joseph Lehner
 *
 */
public class Hasher
{
	private static final int PRIME = 37;
	private static final int INITIAL_HASH = 23;

	private int mHash = INITIAL_HASH;
	private int mHashedCount = 0;

	public void hash(boolean b) {
		hash(b ? 1 : 0);
	}

	public void hash(char c) {
		hash((int) c);
	}

	public void hash(int i)
	{
		// In case a class with two int members, a and b, were to have the values a = 1 and b = 2,
		// the hash would be the same if the values were a = 2 and b = 1. By multiplying the actual
		// value + 1 with the number of hashed values, this problem should be alleviated.
		mHash = term() + (i + 1) * ++mHashedCount;
	}

	public void hash(long l) {
		hash((int) (l ^ l >>> 32));
	}

	public void hash(float f) {
		hash(Float.floatToIntBits(f));
	}

	public void hash(double d) {
		hash(Double.doubleToLongBits(d));
	}

	public void hash(Object o)
	{
		if(o == null)
			hash(0);
		else if(!o.getClass().isArray())
			hash(o.hashCode());
		else
		{
			final int length = Array.getLength(o);
			for(int i = 0; i != length; ++i)
				hash(Array.get(o, i));
		}
	}

	public int getHashCode() {
		return mHash;
	}

	public Hasher reset()
	{
		mHash = INITIAL_HASH;
		mHashedCount = 0;
		return this;
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException("Call getHashCode() to obtain the calculated hash");
	}

	@Override
	public boolean equals(Object o) {
		throw new UnsupportedOperationException();
	}

	public static Hasher getInstance() {
		return sHashers.get().reset();
	}

	private int term() {
		return PRIME * mHash;
	}

	private static final ThreadLocal<Hasher> sHashers = new ThreadLocal<Hasher>() {
		@Override
		protected Hasher initialValue()
		{
			return new Hasher();
		}
	};

	private Hasher() {}
}

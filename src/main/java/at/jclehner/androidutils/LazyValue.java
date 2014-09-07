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

package at.jclehner.androidutils;

public abstract class LazyValue<T>
{
	public interface Mutator<T>
	{
		void mutate(T value);
	}

	private volatile T mValue;

	public T get()
	{
		if(mValue == null)
		{
			synchronized(this)
			{
				if(mValue == null)
					mValue = value();
			}
		}

		return mValue;
	}

	public void set(T value)
	{
		synchronized(this) {
			mValue = value;
		}
	}

	public synchronized void reset() {
		mValue = null;
	}

	public synchronized void mutate(Mutator<T> mutator)
	{
		if(mValue == null)
			return;

		mutator.mutate(mValue);
	}

	public abstract T value();
}

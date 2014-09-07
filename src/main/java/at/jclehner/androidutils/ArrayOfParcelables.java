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

import android.os.Parcel;
import android.os.Parcelable;

import at.jclehner.rxdroid.util.Util;


public class ArrayOfParcelables implements Parcelable
{
	private Parcelable[][] mArray;

	public <T extends Parcelable> ArrayOfParcelables(T[][] array)
	{
		mArray = new Parcelable[array.length][];
		for(int i = 0; i != array.length; ++i)
		{
			final int length = array[i].length;
			mArray[i] = new Parcelable[length];
			System.arraycopy(array[i], 0, mArray[i], 0, length);
		}
	}

	public Parcelable[][] get() {
		return mArray;
	}

	public <T extends Parcelable> T[][] get(Creator<T> creator, T[][] outArray)
	{
		if(outArray.length < mArray.length)
			throw new IllegalArgumentException("Insufficient capacity in outArray");

		for(int i = 0; i != mArray.length; ++i)
		{
			final int length = mArray[i].length;

			if(i == 0)
				outArray[i] = creator.newArray(length);

			System.arraycopy(mArray[i], 0, outArray[i], 0, length);
		}

		return outArray;
	}

	public int length() {
		return mArray.length;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags)
	{
		parcel.writeInt(mArray.length);

		for(int i = 0; i != mArray.length; ++i)
			parcel.writeParcelableArray(mArray[i], flags);
	}

	@Override
	public String toString() {
		return Util.arrayToString(mArray);
	}

	public static final Parcelable.Creator<ArrayOfParcelables> CREATOR = new Parcelable.Creator<ArrayOfParcelables>() {

		@Override
		public ArrayOfParcelables createFromParcel(Parcel in)
		{
			final ArrayOfParcelables array = new ArrayOfParcelables();
			array.mArray = new Parcelable[in.readInt()][];

			for(int i = 0; i != array.mArray.length; ++i)
				array.mArray[i] = in.readParcelableArray(getClass().getClassLoader());

			return array;
		}

		@Override
		public ArrayOfParcelables[] newArray(int size)
		{
			return new ArrayOfParcelables[size];
		}
	};

	private ArrayOfParcelables() {}
}

/**
 * Copyright (C) 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
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

package at.caspase.androidutils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashMap;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference.BaseSavedState;
import android.util.Log;

/**
 * A helper class for saving Object states.
 * <p>
 * This class vastly reduces the amount of boilerplate code
 * required for saving/restoring states.
 */

public final class StateSaver
{
	private static final String TAG = StateSaver.class.getName();
	private static final boolean LOGV = true;

	/**
	 * Marks object members whose value should be saved/restored.
	 * @author Joseph Lehner
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface SaveState {};

	/**
	 * Creates a Parcelable with the given object's state.
	 * <p>
	 * This function automagically saves all members annotated using
	 * SaveState and combines their values with the state of the object's
	 * superclass.
	 *
	 * @param o The object from which to create an instance state.
	 * @param superState The state of pref's superclass.
	 * @param extras Any additional data, can be <code>null</code>.
	 * @return
	 */
	public static Parcelable createInstanceState(Object o, Parcelable superState, Bundle extras)
	{
		final SavedState myState = new SavedState(superState);
		myState.extras = extras;

		forEachAnnotatedMember(o, new Callback() {

			@Override
			public void invoke(Object o, Field f, String mapKey)
			{
				try
				{
					myState.values.put(mapKey, f.get(o));
				}
				catch (IllegalArgumentException e)
				{
					Log.w(TAG, e);
				}
				catch (IllegalAccessException e)
				{
					Log.w(TAG, e);
				}
			}
		});

		return myState;
	}

	public static Parcelable getSuperState(Parcelable state)
	{
		if(state instanceof SavedState)
			return ((SavedState) state).getSuperState();

		return state;
	}

	public static Bundle getExtras(Parcelable state)
	{
		if(state instanceof SavedState)
			return ((SavedState) state).extras;

		return null;
	}

	public static void restoreInstanceState(Object o, Parcelable state)
	{
		if(!(state instanceof SavedState))
			return;

		final SavedState myState = (SavedState) state;

		forEachAnnotatedMember(o, new Callback() {

			@Override
			public void invoke(Object o, Field f, String mapKey)
			{
				Object value = myState.values.get(mapKey);

				try
				{
					f.set(o, value);
				}
				catch(IllegalArgumentException e)
				{
					if(value != null)
					{
						Log.w(TAG, "Illegal value: field " + f.getName() + "=" + value +
								" in " + o.getClass().getSimpleName());
					}
				}
				catch(IllegalAccessException e)
				{
					// a notification message is printed in forEachAnnotatedMemberRecursive!
				}
			}
		});
	}

	private static void forEachAnnotatedMember(Object o, Callback callback) {
		forEachAnnotatedMemberInternal(o.getClass(), o, callback);
	}

	private static void forEachAnnotatedMemberInternal(Class<?> clazz, Object o, Callback callback)
	{
		Class<?> mySuper = clazz.getSuperclass();
		if(mySuper != null)
			forEachAnnotatedMemberInternal(mySuper, o, callback);

		if(LOGV) Log.v(TAG, "forEachAnnotatedMemberInternal: " + clazz.getSimpleName());

		for(Field f : clazz.getDeclaredFields())
		{
			if(f.isAnnotationPresent(SaveState.class))
			{
				String mapKey = f.getName() + "@" + clazz.getName();
				if(LOGV) Log.v(TAG, "  " + f.getName());

				try
				{
					boolean wasAccessibleFlagChanged = makeAccessible(f);
					callback.invoke(o, f, mapKey);
					if(wasAccessibleFlagChanged)
						f.setAccessible(false);
				}
				catch(SecurityException e)
				{
					throw new RuntimeException("Error accessing field " +
							f.getName() + " in class " + clazz.getSimpleName());
				}
			}
		}
	}

	private static boolean makeAccessible(Field f)
	{
		if(!f.isAccessible())
		{
			f.setAccessible(true);
			return true;
		}

		return false;
	}

	private interface Callback
	{
		void invoke(Object o, Field f, String mapKey);
	}

	private static class SavedState extends BaseSavedState
	{
		HashMap<String, Object> values = new HashMap<String, Object>();
		Bundle extras;

		@SuppressWarnings("unchecked")
		public SavedState(Parcel parcel)
		{
			super(parcel);

			values = (HashMap<String, Object>) parcel.readSerializable();
			extras = parcel.readBundle();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags)
		{
			super.writeToParcel(dest, flags);
			dest.writeSerializable(values);
			dest.writeBundle(extras);
		}

		public SavedState(Parcelable superState) {
			super(superState);
		}

		@SuppressWarnings("unused")
		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {

			@Override
			public SavedState createFromParcel(Parcel in)
            {
                return new SavedState(in);
            }

            @Override
			public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };
	}
}

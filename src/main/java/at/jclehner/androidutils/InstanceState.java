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

import at.jclehner.rxdroid.BuildConfig;

/**
 * A helper class for saving Object states.
 * <p>
 * This class vastly reduces the amount of boilerplate code
 * required for saving/restoring states.
 */

public final class InstanceState
{
	private static final String TAG = InstanceState.class.getSimpleName();
	private static final boolean LOGV = false;

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
	 * This function automagically saves all members annotated with
	 * {@link SaveState} and combines their values with the state of the object's
	 * superclass.
	 *
	 * @param object The object from which to create an instance state.
	 * @param superState The state of pref's superclass.
	 * @param extras Any additional data, can be <code>null</code>.
	 * @return
	 */
	public static Parcelable createFrom(Object object, Parcelable superState, Bundle extras)
	{
		final SavedState myState;
		if(superState instanceof SavedState)
		{
			myState = (SavedState) superState;
			if(myState.extras == null)
				myState.extras = extras;
			else
				myState.extras.putAll(extras);
		}
		else
		{
			myState = new SavedState(superState);
			myState.extras = extras;
		}

		if(!myState.wasCreateInstanceStateCalled)
		{
			forEachAnnotatedMember(object, new Callback() {

				@Override
				public void invoke(Object o, Field f, String mapKey)
				{
					try
					{
						myState.values.put(mapKey, f.get(o));
					}
					catch(IllegalArgumentException e)
					{
						Log.w(TAG, e);
					}
					catch(IllegalAccessException e)
					{
						Log.w(TAG, e);
					}
				}
			});

			myState.wasCreateInstanceStateCalled = true;
		}

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

	public static void restoreTo(Object object, Parcelable state)
	{
		if(!(state instanceof SavedState))
			return;

		final SavedState myState = (SavedState) state;

		forEachAnnotatedMember(object, new Callback() {

			@Override
			public void invoke(Object o, Field f, String mapKey)
			{
				if(!myState.values.containsKey(mapKey))
				{
					Log.w(TAG, "restoreInstanceState: key does not exist: " + mapKey);
					return;
				}

				final Object value = myState.values.get(mapKey);

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

	private static void forEachAnnotatedMember(Object o, Callback callback)
	{
		final Class<?> clazz = o.getClass();

		if(LOGV) Log.v(TAG, "forEachAnnotatedMember: " + clazz.getSimpleName());

		for(Field f : Reflect.getAllFields(clazz))
		{
			if(!f.isAnnotationPresent(SaveState.class))
				continue;

			final String mapKey = f.getName() + "@" + f.getDeclaringClass().getName();
			if(LOGV) Log.v(TAG, "  " + f.getName() + " (" + mapKey + ")");
			f.setAccessible(true);
			callback.invoke(o, f, mapKey);
		}

	}

	private interface Callback
	{
		void invoke(Object o, Field f, String mapKey);
	}

	public static class SavedState extends BaseSavedState
	{
		protected HashMap<String, Object> values = new HashMap<String, Object>();
		protected Bundle extras;
		protected boolean wasCreateInstanceStateCalled = false;

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

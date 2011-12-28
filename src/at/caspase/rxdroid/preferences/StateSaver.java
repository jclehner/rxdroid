package at.caspase.rxdroid.preferences;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.HashMap;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.Preference.BaseSavedState;
import android.util.Log;

/**
 * A helper class for saving Preference states.
 * <p>
 * This class vastly reduces the amount of boilerplate code
 * required for saving/restoring states.
 */

public final class StateSaver
{
	private static final String TAG = StateSaver.class.getName();
	private static final boolean LOGV = true;
	
	// A key for the SavedState.values HashMap. To prevent the extras
	// from being overwritten, this name must be a name that is illegal
	// as a Java identifier.
	private static final String KEY_EXTRAS = "!extras!";
	
	/**
	 * Marks preference members whose value should be saved/restored. 
	 * @author Joseph Lehner
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public @interface SaveState {};	
	
	/**
	 * Creates a Parcelable with the given Preference's state.
	 * <p>
	 * This function automagically saves all members annotated using
	 * SaveState and combines their values with the Preference
	 * 
	 * @param pref The preference to save.
	 * @param superState The state of pref's superclass.
	 * @param extras Any additional data, can be <code>null</code>.
	 * @return
	 */
	public static Parcelable createInstanceState(Preference pref, Parcelable superState, Bundle extras)
	{
		final SavedState myState = new SavedState(superState);
		
		if(LOGV) Log.d(TAG, "saveState: key=" + pref.getKey());
		
		forEachAnnotatedMember(pref, new Callback() {
			
			@Override
			public void invoke(Preference pref, Field f)
			{
				try
				{
					myState.values.put(f.getName(), f.get(pref));
					if(LOGV) Log.d(TAG, "  " + f.getName() + " <- " + f.get(pref));
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
	
	public static void restoreInstanceState(Preference pref, Parcelable state)
	{
		if(LOGV) Log.d(TAG, "restoreState: key=" + pref.getKey());
		
		if(state instanceof SavedState)
		{
			final SavedState myState = (SavedState) state;
			
			forEachAnnotatedMember(pref, new Callback() {
				
				@Override
				public void invoke(Preference pref, Field f)
				{
					Object value = myState.values.get(f.getName());
					if(value == null)
						Log.w(TAG, "restoreState: " + f.getName() + " is null");
					
					try
					{
						f.set(pref, value);
						if(LOGV) Log.d(TAG, "  " + f.getName() + " := " + value);
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
		}		
	}
	
	private static void forEachAnnotatedMember(Preference pref, Callback callback)
	{
		Class<?> cls = pref.getClass();
		Class<?> mySuper = cls.getSuperclass();
		
		if(pref.getClass().equals(mySuper))
			forEachAnnotatedMember((Preference) mySuper.cast(pref), callback);
						
		for(Field f : cls.getDeclaredFields())
		{
			if(f.isAnnotationPresent(SaveState.class))
				callback.invoke(pref, f);
		}	
	}
	
	private interface Callback
	{
		void invoke(Preference pref, Field f);
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
			
			public SavedState createFromParcel(Parcel in) 
            {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) 
            {
                return new SavedState[size];
            }
        };
	}	
}

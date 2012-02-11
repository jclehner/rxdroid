package at.caspase.androidutils.otpm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import at.caspase.androidutils.otpm.ObjectToPreferenceMapper.MapToPreference;
import at.caspase.androidutils.otpm.ObjectToPreferenceMapper.ObjectWrapper;
import at.caspase.rxdroid.util.Reflect;

/**
 * A helper class for intializing Preferences.
 * <p>
 * Implementations of this class serve two purposes:
 * <ol>
 * <li>Initialize the Preference type specified in {@link MapToPreference}</li>
 * <li>Update this Preference's state if the value changed</li>
 * </ol>
 * Also
 *
 * @author Joseph Lehner
 *
 * @param <P> The Preference type to use (must correspond to the type specified in
 * 	the {@link MapToPreference} annotation).
 * @param <T> The type of the field to map to a Preference.
 */
public abstract class PreferenceHelper<P extends Preference, T>
{
	private static final String TAG = PreferenceHelper.class.getName();

	/**
	 *
	 */
	protected ObjectWrapper<?> mWrapper;
	private Field mField;

	/* package */ final void setData(ObjectWrapper<?> data, Field field)
	{
		mWrapper = data;
		mField = field;
	}

	/**
	 * Initializes the <code>Preference</code>.
	 * <p>
	 * Don't call {@link Preference#setOnPreferenceChangeListener(OnPreferenceChangeListener)}, as it
	 * will be overridden by a subsequent call to that function. Instead, use
	 * {@link #updatePreference(Preference, Object)} for that purpose.
	 *
	 * @param preference The preference to initialize. The following properties
	 * 	are guaranteed to be set: title, order and key.
	 * @param fieldValue The value of the field that is to be mapped to a <code>Preference</code>.
	 */
	public abstract void initPreference(P preference, T fieldValue);

	public final OnPreferenceChangeListener getOnPreferenceChangeListener()
	{
		return new OnPreferenceChangeListener() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				Log.d(TAG, "onPreferenceChange: key=" + preference.getKey() + ", value=" + newValue);
				return updatePreference((P) preference, newValue);
			}
		};
	}

	/**
	 * Updates the preference.
	 * <p>
	 * This function is called from the Preference's OnPreferenceChangeListener. For
	 * more info see the docs of {@link OnPreferenceChangeListener#onPreferenceChange(Preference, Object)}.
	 *
	 * @returns whether the preference should be updated. Default is <code>true</code>.
	 */
	@SuppressWarnings("unchecked")
	public boolean updatePreference(P preference, Object newPrefValue)
	{
		setFieldValue((T) newPrefValue);
		return true;
	}

	/**
	 * Returns whether or not a certain field is disabled.
	 * <p>
	 * Disabled fields will not be added to the Preference hierarchy.
	 *
	 * @return <code>false</code> by default.
	 */
	public boolean isDisabled() {
		return false;
	}

	protected final void setFieldValue(T value)
	{
		try
		{
			boolean changedAccess = Reflect.makeAccessible(mField);
			mField.set(mWrapper, value);
			if(changedAccess)
				mField.setAccessible(true);
		}
		catch(IllegalArgumentException e)
		{
			throw new RuntimeException(e);
		}
		catch(IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}

	protected final String getSummary()
	{
		Annotation a = mField.getAnnotation(MapToPreference.class);
		return Reflect.getAnnotationParameter(a, "summary");
	}
}
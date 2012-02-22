package at.caspase.androidutils.otpm;

import java.lang.reflect.Field;

import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import at.caspase.androidutils.otpm.OTPM.MapToPreference;
import at.caspase.androidutils.otpm.OTPM.ObjectWrapper;
import at.caspase.rxdroid.util.Reflect;

/**
 * A helper class for intializing Preferences.
 * <p>
 * Implementations of this class serve two purposes:
 * <ol>
 * <li>Initialize the Preference type specified in a field's {@link MapToPreference} annotation.</li>
 * <li>Update this Preference's state if the value changed (summary, title, etc.).</li>
 * </ol>
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
	 * The wrapper around the Object that is being mapped to a PreferenceScreen.
	 * <p>
	 * The reason for providing this is that so you can update a Preference's
	 * state depending on the state of other fields. As a general rule, do not
	 * use it for anything besides calling {@link ObjectWrapper#get()}.
	 */
	protected ObjectWrapper<?> mWrapper;
	private Field mField;

	/**
	 * Sets an instance's data (used internally).
	 *
	 * @param wrapper The wrapper that is being mapped to a <code>PreferenceScreen</code>.
	 * @param field The wrapper's field that is being mapped to a <code>Preference</code>.
	 */
	/* package */ final void setData(ObjectWrapper<?> wrapper, Field field)
	{
		mWrapper = wrapper;
		mField = field;
	}

	/**
	 * Initializes the <code>Preference</code>.
	 * <p>
	 *
	 * @param preference The preference to initialize. The following properties
	 * 	are guaranteed to be set: title, order and key.
	 * @param fieldValue The value of the field that is to be mapped to a <code>Preference</code>.
	 */
	public abstract void initPreference(P preference, T fieldValue);

	/**
	 * Returns the <code>Preference</code>'s <code>OnPreferenceChangeListener</code>.
	 * <p>
	 * The default implementation simply calls {@link #updatePreference(Preference, Object)}.
	 * If using your own listener, make sure you do the same.	 *
	 *
	 * @return The <code>OnPreferenceChangeListener</code> that will be applied to this helper's <code>Preference</code>.
	 */
	public OnPreferenceChangeListener getOnPreferenceChangeListener()
	{
		return new OnPreferenceChangeListener() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				return updatePreference((P) preference, newValue);
			}
		};
	}

	/**
	 * Updates the preference.
	 * <p>
	 * This function is called from the Preference's OnPreferenceChangeListener. For
	 * more info see the docs of
	 * <p>
	 * This function's default implementation looks like this:
	 * <pre>
	 * {@code
	 *
	 * public boolean updatePreference(Preference preference, Object newValue)
	 * {
	 *     setFieldValue(newValue); // This will update the ObjectWrapper's field. You should
	 *                              // do so too if overriding this function!
	 *     return true; // Return true from the Preference's OnPreferenceChangeListener
	 * }
	 *
	 * }
	 * <pre>
	 *
	 * @see OnPreferenceChangeListener#onPreferenceChange(Preference, Object)
	 *
	 * @param preference The preference to update.
	 * @param newValue The new value of that preference.
	 *
	 * @returns whether the preference should be updated. Default is <code>true</code>.
	 */
	@SuppressWarnings("unchecked")
	public boolean updatePreference(P preference, Object newValue)
	{
		setFieldValue((T) newValue);
		return true;
	}

	/**
	 * Returns whether or not a certain field is disabled.
	 * <p>
	 * Disabled fields will not be added to the Preference hierarchy.
	 *
	 * @return <code>false</code> by default.
	 */
	public boolean isPreferenceDisabled() {
		return false;
	}

	protected final void setFieldValue(T value)
	{
		try
		{
			boolean changedAccess = Reflect.makeAccessible(mField);
			mField.set(mWrapper, value);
			if(changedAccess)
				mField.setAccessible(false);
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

	/**
	 * Returns the summary, as specified in {@link MapToPreference}.
	 *
	 * @return The preference summary.
	 */
	/*protected final String getSummary()
	{
		Annotation a = mField.getAnnotation(MapToPreference.class);
		return Reflect.getAnnotationParameter(a, "summary");
	}*/
}
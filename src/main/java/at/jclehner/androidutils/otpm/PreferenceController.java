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

package at.jclehner.androidutils.otpm;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.util.Log;
import at.jclehner.androidutils.Extras;
import at.jclehner.androidutils.Reflect;
import at.jclehner.androidutils.otpm.OTPM.CreatePreference;
import at.jclehner.rxdroid.util.CollectionUtils;

/**
 * A helper class for intializing Preferences.
 * <p>
 * Implementations of this class serve two purposes:
 * <ol>
 * <li>Initialize the Preference type specified in a field's {@link CreatePreference} annotation.</li>
 * <li>Update this Preference's state if the value changed (summary, title, etc.).</li>
 * </ol>
 *
 * @author Joseph Lehner
 *
 * @param <P> The Preference type to use (must correspond to the type specified in
 * 	the {@link CreatePreference} annotation).
 * @param <T> The type of the field to map to a Preference.
 */
public abstract class PreferenceController<P extends Preference, T>
{
	private static final String TAG = PreferenceController.class.getSimpleName();
	private static final boolean LOGV = false;

	protected Object mObject;
	private Field mField;

	//private String[] mSummaryDependencies = null;

	private String mPrefKey = null;

	private WeakReference<PreferenceGroup> mRootPrefGroup = null;
	//private WeakReference<P> mPreference = null;

	private String[] mFieldDependencies = null;
	private List<String> mForwardDependencies = null;

	private boolean mAutoUpdateSummaries = true;

	public PreferenceController() {}

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
	 * Converts the Preference's value to the field's type.
	 * <p>
	 * This function must be overridden if the field's type differs from
	 * the Preference's.
	 */
	@SuppressWarnings("unchecked")
	public T toFieldType(Object prefValue) {
		return (T) prefValue;
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
	public boolean updatePreference(P preference, T newValue)
	{
		setFieldValue(newValue);
		onPreferenceUpdate(preference, newValue);
		return true;
	}

	/**
	 * Updates the preference's summary.
	 */
	public void updateSummary(P preference, T newValue) {
		preference.setSummary(toString(newValue));
	}

	/**
	 * Returns whether or not a certain field is disabled.
	 * <p>
	 * Disabled fields will not be added to the Preference hierarchy.
	 *
	 * @return <code>false</code> by default.
	 */
	public boolean isPreferenceHidden() {
		return false;
	}

	public void onDependencyChange(P preference, String depKey, Object newPrefValue) {
		// do nothing
	}

	public void onPreferenceUpdate(P preference, T newValue) {
		// do nothing
	}

	public void onFieldValueSet(T value) {
		// do nothing
	}

	protected final void setFieldValue(T value)
	{
		Reflect.setFieldValue(mField, mObject, value);
		onFieldValueSet(value);
	}

	protected final void setFieldValue(String fieldName, Object value)
	{
		checkAccessToField(fieldName);

		final Class<?> clazz = mObject.getClass();
		final Field field;

		try
		{
			field = clazz.getDeclaredField(fieldName);
		}
		catch(NoSuchFieldException e)
		{
			throw new RuntimeException("No field " + fieldName + " in type " + clazz.getSimpleName());
		}

		Reflect.setFieldValue(field, mObject, value);

		//if(isAdditionalField(fieldName))
		//	notifyForwardDependencies(
	}

	protected final Object getFieldValue(String fieldName)
	{
		checkAccessToField(fieldName);

		final Class<?> clazz = mObject.getClass();
		final Field field;

		try
		{
			field = clazz.getDeclaredField(fieldName);
		}
		catch(NoSuchFieldException e)
		{
			throw new RuntimeException(e);
		}

		return Reflect.getFieldValue(field, mObject);
	}

	@SuppressWarnings("unchecked")
	protected final T getFieldValue() {
		return (T) Reflect.getFieldValue(mField, mObject);
	}

	@SuppressWarnings("unchecked")
	protected final P getPreference()
	{
		final PreferenceGroup root = mRootPrefGroup.get();
		if(root == null)
			throw new IllegalStateException("mRootPrefGroup is not available anymore");

		return (P) root.findPreference(mPrefKey);
	}

	protected final void notifyForwardDependencies() {
		notifyForwardDependencies(getFieldValue());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected final void notifyForwardDependencies(T newPrefValue)
	{
		if(mForwardDependencies == null)
			return;

		final PreferenceGroup root = mRootPrefGroup.get();
		if(root != null)
		{
			final Bundle extras = Extras.get(root);
			if(extras == null)
			{
				Log.w(TAG, "Missing extras, cannot notify dependencies");
				return;
			}

			final Map<String, PreferenceController> prefHelpers =
					(Map<String, PreferenceController>) extras.getSerializable(OTPM.EXTRA_PREF_HELPERS);

			if(prefHelpers == null)
				return;

			for(String key : mForwardDependencies)
			{
				final Preference p = root.findPreference(key);
				final PreferenceController ph = prefHelpers.get(key);

				if(p == null || ph == null)
				{
					Log.w(TAG, "No preference or associated helper for key=" + key);
					continue;
				}

				//ph.updateSummary(p, ph.getFieldValue());
				//ph.onDependencyChange(p, preference.getKey(), newPrefValue);
				ph.onDependencyChange(p, mPrefKey, newPrefValue);
			}
		}
		else
			Log.w(TAG, "Root preference was destroyed, cannot notify dependencies");
	}

	/* package */ final OnPreferenceChangeListener getOnPreferenceChangeListener()
	{
		return new OnPreferenceChangeListener() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean onPreferenceChange(Preference preference, Object newPrefValue)
			{
				if(LOGV) Log.v(TAG, "onPreferenceChange: key=" + preference.getKey() + ", value=" + newPrefValue);

				final T newValue = toFieldType(newPrefValue);
				final boolean doChange = updatePreference((P) preference, newValue);
				if(mAutoUpdateSummaries)
					updateSummary((P) preference, newValue);
				else if(LOGV)
					Log.v(TAG, "Not updating summary of " + preference.getKey());

				if(doChange)
					notifyForwardDependencies(newValue);

				return doChange;
			}
		};
	}

	/* package */ final void initPreferenceInternal(P preference, T fieldValue)
	{
		initPreference(preference, fieldValue);
		if(mAutoUpdateSummaries)
			updateSummary(preference, fieldValue);
		mPrefKey = preference.getKey();
	}

	/**
	 * Sets an instance's data (used internally).
	 *
	 * @param wrapper The wrapper that is being mapped to a <code>PreferenceScreen</code>.
	 * @param field The wrapper's field that is being mapped to a <code>Preference</code>.
	 */
	/* package */ final void setData(Object object, Field field)
	{
		mObject = object;
		mField = field;
	}

	/* package */ final void setForwardDependencies(String[] dependencies) {
		mForwardDependencies = Arrays.asList(dependencies);
	}

	/* package */ final void addForwardDependencies(List<String> dependencies)
	{
		if(mForwardDependencies == null)
			mForwardDependencies = new ArrayList<String>(dependencies);
		else
			mForwardDependencies.addAll(dependencies);
	}

	/* package */ final void setFieldDependencies(String[] fieldNames) {
		mFieldDependencies = fieldNames;
	}

	/* package */ final void enableSummaryUpdates(boolean enabled) {
		mAutoUpdateSummaries = enabled;
	}

	/* package */ final void setRootPreferenceGroup(PreferenceGroup root) {
		mRootPrefGroup = new WeakReference<PreferenceGroup>(root);
	}

	private void checkAccessToField(String fieldName)
	{
		if(fieldName.equals(mField.getName()))
		{
			Log.w(TAG, "Accessing wrapped field \"" + fieldName + "\" from its PreferenceHelper by name is discouraged.");
			return;
		}
		else if(isFieldDependency(fieldName))
			return;

		// XXX this might be changed to throwing an exception in the future
		Log.w(TAG, "Undeclared access to field " + fieldName + " from PreferenceHelper of field " + mField.getName());
	}

	private boolean isFieldDependency(String fieldName)
	{
		if(mFieldDependencies == null)
			return false;

		return CollectionUtils.indexOf(fieldName, mFieldDependencies) != -1;
	}

	private static String toString(Object o)
	{
		if(o == null)
			return "null";

		return o.toString();
	}
}
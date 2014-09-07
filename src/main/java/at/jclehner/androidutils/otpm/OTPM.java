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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.util.Log;
import at.jclehner.androidutils.Extras;
import at.jclehner.androidutils.Reflect;
import at.jclehner.rxdroid.util.WrappedCheckedException;
/**
 * Helper functions for creating a preference hierarchy from an arbitrary <code>Object</code>.
 *
 * @author Joseph Lehner
 *
 */
public class OTPM
{
	private static final String TAG = OTPM.class.getSimpleName();
	private static final boolean LOGV = false;

	/* package */ static final String EXTRA_PREF_HELPERS = TAG + ".EXTRA_PREF_HELPERS";

	private static final String UNDEFINED = "<!!!UNDEFINED!!!>";
	public static final String CLOSE_GROUP = TAG + ".close_group";

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface CreatePreference
	{
		/**
		 * The preference type to use for this field.
		 * <p>
		 * Note that this <code>Preference</code> must provide a <code>Preference(Context)</code>
		 * constructor.
		 */
		Class<? extends Preference> type();

		/**
		 * The type of PreferenceController to use for this preference.
		 */
		Class<? extends PreferenceController<?, ?>> controller();

		/**
		 * The preference key (defaults to the field name).
		 */
		String key() default UNDEFINED;

		/**
		 * The preference title.
		 * <p>
		 * Note that some implementations of {@link PreferenceController} ignore this.
		 */
		String title() default UNDEFINED;

		boolean enabled() default true;

		/**
		 * The preference title.
		 * <p>
		 * Note that some implementations of {@link PreferenceController} ignore this.
		 */
		int titleResId() default 0;

		/**
		 * The preference's summary.
		 * <p>
		 * Note that some implementations of {@link PreferenceController} ignore this.
		 */
		String summary() default UNDEFINED;

		/**
		 * The preference's summary.
		 * <p>
		 * Note that some implementations of {@link PreferenceController} ignore this.
		 */
		int summaryResId() default 0;

		/**
		 * Starts a new preference category with the given title.
		 * <p>
		 * Note that the title also serves as this category's key.
		 */
		String category() default UNDEFINED;

		/**
		 * Starts a new preference category with the given title.
		 * <p>
		 * Note that the title also serves as this category's key.
		 */
		int categoryResId() default 0;

		/**
		 * Set a custom key for the preference.
		 * <p>
		 * Note that this will default to the preference's title if not specified.
		 */
		String categoryKey() default UNDEFINED;

		/**
		 * A list of preference keys acting as forward dependencies.
		 * <p>
		 * Forward dependencies are dependencies that are notified if
		 * this preference is updated.
		 */
		String[] forwardDependencies() default {};

		/**
		 * A list of preference keys acting as reverse dependencies.
		 * <p>
		 * Reverse dependencies are Preferences that this Preference
		 * will depend on. Thus if a preference in this list is updated,
		 * this preference is notified of a change.
		 */
		String[] reverseDependencies() default {};

		/**
		 * A list of field names belonging to this preference.
		 * <p>
		 * If a Preference stores its value in more than one fields, the
		 * other fields' names should be specified here.
		 */
		String[] fieldDependencies() default {};

		/**
		 * Ends a preference category, if one was active.
		 * <p>
		 * Note that this has no effect whatsoever visually, as the last <code>Preference</code> of
		 * the <code>PreferenceCategory</code> will be indiscernible from the first <code>Preference</code>
		 * after that<sup>*</sup>. Use of this function is thus only neccessary, when you intend to access all
		 * children of a certain <code>PreferenceCategory</code>.
		 * <p>
		 * <sup>*</sup>) This applies to both the pre-Honeycomb "Dark" theme and the new "Holo" theme, but might be
		 * different on various custom schemes.
		 */
		boolean endActiveCategory() default false;

		/**
		 * The order of this preference within the parent {@link PreferenceScreen}.
		 * <p>
		 * Note that the preferences may appear in no particular order if you do not
		 * explicitly set this.
		 */
		int order() default 0;

		//boolean persistent() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface AddPreference
	{
		/**
		 * The order of this preference within the parent {@link PreferenceScreen}.
		 * <p>
		 * Note that the preference's may appear in no particular order if you do not
		 * explicitly set this property.
		 */
		int order() default 0;
	};

	public static void mapToPreferenceHierarchy(PreferenceGroup root, Object object)
	{
		final List<PrefInfo> prefInfoList = initializePreferenceHierarchy(root, object);
		initializePreferences(root, object, prefInfoList);
	}

	static class PrefInfo
	{
		final String key;
		final Annotation annotation;
		final Field field;

		PrefInfo(String key, Annotation annotation, Field field)
		{
			this.key = key;
			this.annotation = annotation;
			this.field = field;
		}
	}

	private static List<PrefInfo> initializePreferenceHierarchy(PreferenceGroup root, Object object)
	{
		final ArrayList<PrefInfo> prefInfoList = new ArrayList<PrefInfo>();
		final Context context = root.getContext();
		PreferenceGroup prefCat = null;

		final String hierarchyKey = root.getKey();
		if(hierarchyKey == null)
			throw new IllegalStateException("The root PreferenceGroup must have a key");

		for(Field field : getDeclaredAnnotatedFields(object.getClass()))
		{
			if(field.isAnnotationPresent(AddPreference.class))
			{
				final Preference p = (Preference) Reflect.getFieldValue(field, object);
				if(p != null)
				{
					if(p.hasKey() && root.findPreference(p.getKey()) == null)
						root.addPreference(p);
					continue;
				}
			}

			//final Annotation a = field.getAnnotation(CreatePreference.class);
			final CreatePreference a = field.getAnnotation(CreatePreference.class);
			final Class<? extends Preference> prefClazz = a.type();

			//final Class<? extends Preference> prefClazz = Reflect.getAnnotationParameter(a, "type");
			final String key = getStringParameter(a, "key", field.getName());

			if(key.equals(hierarchyKey))
				throw new IllegalStateException("Cannot use key=" + key + " of root PreferenceGroup");

			prefInfoList.add(new PrefInfo(key, a, field));

			if(root.findPreference(key) != null)
			{
				if(LOGV) Log.v(TAG, "fillPreferenceHierarchy: key=" + key + " exists");
				continue;
			}

			final Preference p = newPreferenceInstance(prefClazz, context);
			//final Preference p = Reflect.newInstance(prefClazz, context);
			p.setTitle(getStringResourceParameter(context, a, "title"));
			p.setPersistent(false);
			p.setKey(key);

			String categoryTitle = getStringResourceParameter(context, a, "category");
			if(categoryTitle != null)
			{
				prefCat = new PreferenceCategory(context);
				prefCat.setTitle(categoryTitle);

				String prefCatKey = a.categoryKey();
//				String prefCatKey = Reflect.getAnnotationParameter(a, "categoryKey");
				if(prefCatKey == null || prefCatKey == UNDEFINED) // == UNDEFINED is intentional
					prefCat.setKey(categoryTitle);
				else
					prefCat.setKey(prefCatKey);

				root.addPreference(prefCat);
			}
			else if(a.endActiveCategory())
				prefCat = null;

			if(prefCat != null)
				prefCat.addPreference(p);
			else
				root.addPreference(p);
		}

		return prefInfoList;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void initializePreferences(PreferenceGroup root, Object object, List<PrefInfo> prefInfoList)
	{
		final Context context = root.getContext();
		//final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

		if(LOGV) Log.v(TAG, "initializePreferences");

		final HashMap<String, PreferenceController> prefHelpers = new HashMap<String, PreferenceController>();
		final HashMap<String, ArrayList<String>> additionalForwardDependencies = new HashMap<String, ArrayList<String>>();

		for(PrefInfo info : prefInfoList)
		{
			final String key = info.key;
			final Preference p = root.findPreference(key);
			if(p == null)
				throw new IllegalStateException("No preference with key=" + key + " in hierarchy");

			if(LOGV) Log.v(TAG, "  key=" + key);

			final Annotation a = info.annotation;
			final Class<? extends PreferenceController> prefHlpClazz = Reflect.getAnnotationParameter(a, "controller");
			final PreferenceController prefHlp;

			try
			{
				prefHlp = Reflect.newInstance(prefHlpClazz);
			}
			catch(WrappedCheckedException e)
			{
				if(e.getCauseType() == NoSuchMethodException.class)
					throw new WrappedCheckedException(prefHlpClazz + " lacks a visible default constructor", e);
				else
					throw e;
			}

			prefHlp.setData(object, info.field);
			prefHlp.setRootPreferenceGroup(root);

			if(prefHlp.isPreferenceHidden())
			{
				root.removePreference(p);
				continue;
			}

			prefHelpers.put(key, prefHlp);

			setupFieldDependencies(prefHlp, a);
			setupSummary(p, prefHlp, a, context);
			prefHlp.initPreferenceInternal(p, Reflect.getFieldValue(info.field, object));
			setupForwardDependencies(prefHlp, a, root);
			collectReverseDependencies(a, key, additionalForwardDependencies);

			final CharSequence title = p.getTitle();
			if(title == null)
				throw new IllegalStateException("No title set for preference " + info.key);

			p.setOnPreferenceChangeListener(prefHlp.getOnPreferenceChangeListener());
		}

		setupReverseDependencies(prefHelpers, additionalForwardDependencies);

		final Bundle rootExtras = Extras.get(root);
		rootExtras.putSerializable(EXTRA_PREF_HELPERS, prefHelpers);
	}

	@SuppressWarnings("rawtypes")
	private static void setupFieldDependencies(PreferenceController prefHlp, Annotation a)
	{
		final String[] fieldDependencies = Reflect.getAnnotationParameter(a, "fieldDependencies");
		if(fieldDependencies.length != 0)
			prefHlp.setFieldDependencies(fieldDependencies);
	}

	@SuppressWarnings("rawtypes")
	private static void setupSummary(Preference p, PreferenceController prefHlp, Annotation a, Context context)
	{
		final String summary = getStringResourceParameter(context, a, "summary");
		if(summary != null)
		{
			if(summary.length() != 0)
				p.setSummary(summary);

			prefHlp.enableSummaryUpdates(false);
			if(LOGV) Log.v(TAG, "    has static summary='" + summary + "'");
		}
	}

	@SuppressWarnings("rawtypes")
	private static void setupForwardDependencies(PreferenceController prefHlp, Annotation a, PreferenceGroup root)
	{
		final String[] fDependencies = Reflect.getAnnotationParameter(a, "forwardDependencies");
		if(fDependencies.length != 0)
		{
			prefHlp.enableSummaryUpdates(true);
			prefHlp.setRootPreferenceGroup(root);
			prefHlp.setForwardDependencies(fDependencies);
		}
	}

	private static void collectReverseDependencies(Annotation a, String key,
			HashMap<String, ArrayList<String>> additionalForwardDependenciesOut)
	{
		final String[] rDependencies = Reflect.getAnnotationParameter(a, "reverseDependencies");
		if(rDependencies.length != 0)
		{
			// We cannot set reverse dependencies, thus we have to store them in order to add
			// them as forward dependencies once we're done.
			for(String depKey : rDependencies)
			{
				if(!additionalForwardDependenciesOut.containsKey(depKey))
					additionalForwardDependenciesOut.put(depKey, new ArrayList<String>());

				additionalForwardDependenciesOut.get(depKey).add(key);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void setupReverseDependencies(HashMap<String, PreferenceController> prefHelpers,
			HashMap<String, ArrayList<String>> additionalForwardDependencies)
	{
		for(String key : additionalForwardDependencies.keySet())
		{
			if(!prefHelpers.containsKey(key))
			{
				Log.w(TAG, "Missing preference helper for key=" + key);
				continue;
			}

			prefHelpers.get(key).addForwardDependencies(additionalForwardDependencies.get(key));
		}
	}

	private static Preference newPreferenceInstance(Class<? extends Preference> prefClazz, Context context)
	{
		try
		{
			return Reflect.newInstance(prefClazz, new Class<?>[] { Context.class }, context);
		}
		catch(WrappedCheckedException e)
		{
			if(e.getCauseType() == NoSuchMethodException.class)
				return Reflect.newInstance(prefClazz, new Class<?>[] { Context.class, AttributeSet.class } , context, null);

			throw e;
		}
	}

	private static String getStringParameter(Annotation a, String parameterName, String defaultValue)
	{
		String ret = Reflect.getAnnotationParameter(a, parameterName);
		if(ret == null || ret == UNDEFINED) // == UNDEFINED is intentional here, as we're using it as a dummy default value
			return defaultValue;

		return ret;
	}

	private static List<Field> getDeclaredAnnotatedFields(Class<?> clazz)
	{
		LinkedList<Field> fields = new LinkedList<Field>();

		for(Field f : clazz.getDeclaredFields())
		{
			if(f.isAnnotationPresent(CreatePreference.class) || f.isAnnotationPresent(AddPreference.class))
				fields.add(f);
		}

		Collections.sort(fields, FIELD_COMPARATOR);
		return fields;
	}

	private static String getStringResourceParameter(Context context, Annotation a, String parameterName)
	{
		if(LOGV) Log.v(TAG, "getStringResourceParameter: parameterName=" + parameterName);

		String str = Reflect.findAnnotationParameter(a, parameterName);
		if(LOGV) Log.v(TAG, "  str='" + str + "'");
		if(str == null || str == UNDEFINED) // == UNDEFINED is intentional
		{
			Integer resId = Reflect.findAnnotationParameter(a, parameterName + "ResId");
			if(LOGV) Log.v(TAG, "  trying " + parameterName + "ResId => " + resId);
			if(resId == null || resId == 0)
				return null;

			return context.getString(resId);
		}

		return str;
	}

	@SuppressWarnings("unchecked")
	private static Integer getFieldOrder(Field f)
	{
		final Class<?>[] types = new Class<?>[] { CreatePreference.class, AddPreference.class };

		for(Class<?> type : types)
		{
			Class<? extends Annotation> annotationType = (Class<? extends Annotation>) type;

			if(f.isAnnotationPresent(annotationType))
			{
				Annotation a = f.getAnnotation(annotationType);
				return Reflect.getAnnotationParameter(a, "order");
			}
		}

		throw new IllegalArgumentException();
	}

	private static final Comparator<Field> FIELD_COMPARATOR = new Comparator<Field>() {

		@Override
		public int compare(Field field1, Field field2)
		{
			Integer order1 = getFieldOrder(field1);
			Integer order2 = getFieldOrder(field2);

			return order1.compareTo(order2);
		}
	};
}

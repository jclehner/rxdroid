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

package at.caspase.androidutils.otpm;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import at.caspase.rxdroid.util.Reflect;

/*
 * TODO
 *
 * - Add some other mechanism to update summaries (maybe function name as
 *   a property of @MapToPreference)
 * - Allow preferences to be loaded from XML files
 *
 */

/**
 * Helper functions for creating a preference hierarchy from an arbitrary <code>Object</code>.
 *
 * @author Joseph Lehner
 *
 */
public class OTPM
{
	private static final String TAG = OTPM.class.getName();
	private static final boolean LOGV = false;

	public static final String EMPTY = "";
	public static final String CLOSE_GROUP = TAG + "close_group";

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface MapToPreference
	{
		/**
		 * The preference type to use for this field.
		 * <p>
		 * Note that this <code>Preference</code> must provide a <code>Preference(Context)</code>
		 * constructor.
		 */
		Class<? extends Preference> type();

		/**
		 * The type of PreferenceHelper to use for this preference.
		 */
		Class<? extends PreferenceHelper<?, ?>> helper();

		/**
		 * The preference key (defaults to the field name).
		 */
		String key() default EMPTY;

		/**
		 * The preference title.
		 * <p>
		 * Note that some implementations of {@link PreferenceHelper} ignore this.
		 */
		String title() default EMPTY;

		/**
		 * The preference title.
		 * <p>
		 * Note that some implementations of {@link PreferenceHelper} ignore this.
		 */
		int titleResId() default 0;

		/**
		 * The preference's summary.
		 * <p>
		 * Note that some implementations of {@link PreferenceHelper} ignore this.
		 */
		String summary() default EMPTY;

		/**
		 * The preference's summary.
		 * <p>
		 * Note that some implementations of {@link PreferenceHelper} ignore this.
		 */
		int summaryResId() default 0;

		/**
		 * Starts a new preference category with the given title.
		 * <p>
		 * Note that the title also serves as this category's key.
		 */
		String category() default EMPTY;

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
		String categoryKey() default EMPTY;

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
		 * Note that the preference's may appear in no particular order if you do not
		 * explicitly set this property.
		 */
		int order() default 0;
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

	public static abstract class ObjectWrapper<T> implements Serializable
	{
		private static final long serialVersionUID = -4236965614902518944L;

		public abstract void set(T value);

		public abstract T get();

		@Override
		public final boolean equals(Object other) {
			throw new UnsupportedOperationException();
		}

		@Override
		public final int hashCode() {
			throw new UnsupportedOperationException();
		}
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void mapToPreferenceScreen(PreferenceScreen prefScreen, ObjectWrapper<?> wrapper)
	{
		if(prefScreen == null)
			throw new NullPointerException("prefScreen");

		if(wrapper == null)
			throw new NullPointerException("wrapper");

		final Context context = prefScreen.getContext();
		PreferenceGroup prefCat = null;

		prefScreen.setOrderingAsAdded(true);

		if(LOGV) Log.v(TAG, "mapToPreferenceScreen");

		for(Field field : getDeclaredAnnotatedFields(wrapper.getClass()))
		{
			if(field.isAnnotationPresent(AddPreference.class))
			{
				prefScreen.addPreference((Preference) Reflect.getFieldValue(field, wrapper));
				continue;
			}

			if(LOGV) Log.v(TAG, "  " + field.getName());

			final String fieldName = field.getName();
			final Annotation a = field.getAnnotation(MapToPreference.class);

			final Class<? extends PreferenceHelper> prefHlpClazz = Reflect.getAnnotationParameter(a, "helper");
			final Class<? extends Preference> prefClazz = Reflect.getAnnotationParameter(a, "type");

			final PreferenceHelper prefHlp = Reflect.newInstance(prefHlpClazz);
			prefHlp.setData(wrapper, field);

			if(prefHlp.isPreferenceDisabled())
				continue;

			String key = Reflect.getAnnotationParameter(a, "key");
			if(key == null || EMPTY.equals(key))
				key = fieldName;

			Preference pref = prefScreen.findPreference(key);
			if(pref == null)
			{
				pref = Reflect.newInstance(prefClazz, new Class<?>[] { Context.class }, context);
				pref.setKey(key);
			}

			// set the title before calling initPreference()
			pref.setTitle(getStringResourceParameter(context, a, "title"));
			pref.setSummary(getStringResourceParameter(context, a, "summary"));

			prefHlp.initPreference(pref, Reflect.getFieldValue(field, wrapper));

			final CharSequence title = pref.getTitle();
			if(title == null || EMPTY.equals(title))
				throw new IllegalStateException(prefHlpClazz.getSimpleName() + " requires you to explicitly set a title for key=" + pref.getKey());

			pref.setPersistent(false);
			pref.setOnPreferenceChangeListener(prefHlp.getOnPreferenceChangeListener());

			String categoryTitle = getStringResourceParameter(context, a, "category");
			if(categoryTitle != null)
			{
				prefCat = new PreferenceCategory(context);
				prefCat.setTitle(categoryTitle);

				String prefCatKey = Reflect.getAnnotationParameter(a, "categoryKey");
				if(title == null || EMPTY.equals(title))
					prefCat.setKey(categoryTitle);
				else
					prefCat.setKey(prefCatKey);

				prefScreen.addPreference(prefCat);
			}
			else if((Boolean) Reflect.getAnnotationParameter(a, "endActiveCategory"))
				prefCat = null; // the preference title is

			if(prefCat != null)
				prefCat.addPreference(pref);
			else
				prefScreen.addPreference(pref);
		}
	}

	private static List<Field> getDeclaredAnnotatedFields(Class<?> clazz)
	{
		LinkedList<Field> fields = new LinkedList<Field>();

		for(Field f : clazz.getDeclaredFields())
		{
			if(f.isAnnotationPresent(MapToPreference.class) ||
					f.isAnnotationPresent(AddPreference.class))
			{
				fields.add(f);
			}
		}

		Collections.sort(fields, FIELD_COMPARATOR);
		return fields;
	}

	private static String getStringResourceParameter(Context context, Annotation a, String parameterName)
	{
		String ret = getPreferenceParameter_(context, a, parameterName);
		if(LOGV) Log.v(TAG, "getPreferenceParameter: " + parameterName + " => '" + ret + "'");
		return ret;
	}

	private static String getPreferenceParameter_(Context context, Annotation a, String parameterName)
	{
		String str = Reflect.findAnnotationParameter(a, parameterName);
		if(str == null || EMPTY.equals(str))
		{
			Integer resId = Reflect.findAnnotationParameter(a, parameterName + "ResId");
			if(resId == null || resId == 0)
				return null;

			return context.getString(resId);
		}

		return str;
	}

	@SuppressWarnings("unchecked")
	private static Integer getFieldOrder(Field f)
	{
		final Class<?>[] types =
				new Class<?>[] { MapToPreference.class, AddPreference.class };

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

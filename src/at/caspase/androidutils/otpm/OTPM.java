package at.caspase.androidutils.otpm;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import at.caspase.rxdroid.util.Reflect;

/**
 * Helper functions for creating a preference hierarchy from an arbitrary <code>Object</code>.
 *
 * @author Joseph Lehner
 *
 */
public class OTPM
{
	private static final String TAG = OTPM.class.getName();
	private static final boolean LOGV = true;

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
		 * Starts a new preference category.
		 */
		String categoryTitle() default EMPTY;

		/**
		 * Starts a new preference category.
		 */
		int categoryResId() default 0;

		/**
		 * Ends a preference category, if one was active.
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
	public @interface AddPreference {};

	public static abstract class ObjectWrapper<T>
	{
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
		final Class<?> clazz = wrapper.getClass();

		PreferenceGroup prefCat = null;

		for(Field field : clazz.getDeclaredFields())
		{
			if(!field.isAnnotationPresent(MapToPreference.class))
			{
				if(field.isAnnotationPresent(AddPreference.class))
					prefScreen.addPreference((Preference) Reflect.getFieldValue(field, wrapper));

				continue;
			}

			final String fieldName = field.getName();
			final Annotation a = field.getAnnotation(MapToPreference.class);

			final Class<? extends PreferenceHelper> prefHlpClazz = Reflect.getAnnotationParameter(a, "helper");
			final Class<? extends Preference> prefClazz = Reflect.getAnnotationParameter(a, "type");

			final PreferenceHelper prefHlp = Reflect.newInstance(prefHlpClazz);
			prefHlp.setData(wrapper, field);

			if(prefHlp.isPreferenceDisabled())
				continue;

			final Preference pref =
					Reflect.newInstance(prefClazz, new Class<?>[] { Context.class }, context);

			final String key = Reflect.getAnnotationParameter(a, "key");
			if(key == null || EMPTY.equals(key))
				pref.setKey(fieldName);
			else
				pref.setKey(key);

			// set the title before calling initPreference()
			pref.setTitle(getPreferenceParameter(context, a, "title"));
			pref.setSummary(getPreferenceParameter(context, a, "summary"));

			prefHlp.initPreference(pref, Reflect.getFieldValue(field, wrapper));

			final CharSequence title = pref.getTitle();
			if(title == null || EMPTY.equals(title))
				throw new IllegalStateException(prefHlpClazz + " requires you to explicitly set a title");

			pref.setPersistent(false);
			pref.setOrder((Integer) Reflect.getAnnotationParameter(a, "order"));
			pref.setOnPreferenceChangeListener(prefHlp.getOnPreferenceChangeListener());

			String categoryTitle = getPreferenceParameter(context, a, "categoryTitle");
			if(categoryTitle != null)
			{
				prefCat = new PreferenceCategory(context);
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

	private static String getPreferenceParameter(Context context, Annotation a, String parameterName)
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
}

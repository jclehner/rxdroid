package at.caspase.androidutils.otpm;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import android.content.Context;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.util.Log;
import at.caspase.rxdroid.util.Reflect;

/**
 * Helper functions for creating a preference hierarchy from an arbitrary <code>Object</code>.
 *
 * @author Joseph Lehner
 *
 */
public class ObjectToPreferenceMapper
{
	private static final String TAG = ObjectToPreferenceMapper.class.getName();
	private static final boolean LOGV = true;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface MapToPreference
	{
		Class<? extends Preference> type();
		Class<? extends PreferenceHelper<?, ?>> helper();
		String title();
		String summary() default "";
		int order() default 0;
	}

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
	public static void populatePreferenceScreen(PreferenceScreen prefScreen, ObjectWrapper<?> wrapper)
	{
		if(prefScreen == null)
			throw new NullPointerException("prefScreen");

		if(wrapper == null)
			throw new NullPointerException("object");

		Class<?> clazz = wrapper.getClass();

		for(Field f : clazz.getDeclaredFields())
		{
			if(!f.isAnnotationPresent(MapToPreference.class))
				continue;

			String fieldName = f.getName();
			Annotation a = f.getAnnotation(MapToPreference.class);

			final Class<? extends PreferenceHelper> prefHlpClazz = Reflect.getAnnotationParameter(a, "helper");
			final Class<? extends Preference> prefClazz = Reflect.getAnnotationParameter(a, "type");

			final PreferenceHelper prefHlp = Reflect.newInstance(prefHlpClazz);
			prefHlp.setData(wrapper, f);

			if(prefHlp.isDisabled())
				continue;

			final Preference pref = Reflect.newInstance(prefClazz, new Class<?>[] { Context.class }, prefScreen.getContext());

			Exception ex;

			try
			{
				boolean changed = Reflect.makeAccessible(f);
				prefHlp.initPreference(pref, f.get(wrapper));

				pref.setTitle((String) Reflect.getAnnotationParameter(a, "title"));
				pref.setKey(fieldName);
				pref.setPersistent(false);
				pref.setOrder((Integer) Reflect.getAnnotationParameter(a, "order"));
				pref.setOnPreferenceChangeListener(prefHlp.getOnPreferenceChangeListener());

				prefScreen.addPreference(pref);

				if(changed)
					f.setAccessible(false);

				continue;
			}
			catch(IllegalArgumentException e)
			{
				ex = e;
			}
			catch(IllegalAccessException e)
			{
				ex = e;
			}

			throw new RuntimeException(ex);
		}
	}
}

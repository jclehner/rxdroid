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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.util.Log;
import at.jclehner.rxdroid.util.WrappedCheckedException;

/**
 * Utility functions for reflection.
 *
 * @author Joseph Lehner
 *
 */
public final class Reflect
{
	private static final String TAG = Reflect.class.getSimpleName();
	private static final boolean LOGV = false;

	/**
	 * Get the declared field, returning <code>null</code> if it wasn't found.
	 *
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static Field getDeclaredField(Class<?> clazz, String fieldName)
	{
		Exception ex;

		try
		{
			final Field f = clazz.getDeclaredField(fieldName);
			f.setAccessible(true);
			return f;
		}
		catch(SecurityException e)
		{
			ex = e;
		}
		catch(NoSuchFieldException e)
		{
			ex = e;
		}

		if(LOGV) Log.d(TAG, "getDeclaredField: failed to obtain field " + clazz.getName() + "." + fieldName, ex);

		return null;
	}

	public static Object getFieldValue(Field field, Object object)
	{
		try
		{
			makeAccessible(field);
			return field.get(object);
		}
		catch(IllegalArgumentException e)
		{
			throw new WrappedCheckedException(e);
		}
		catch(IllegalAccessException e)
		{
			throw new WrappedCheckedException(e);
		}
	}

	public static Object getFieldValue(Field field, Object object, Object defValue)
	{
		Exception ex;

		try
		{
			makeAccessible(field);
			return field.get(object);
		}
		catch(IllegalArgumentException e)
		{
			ex = e;
		}
		catch(IllegalAccessException e)
		{
			ex = e;
		}

		Log.i(TAG, "getFieldValue: failed to obtain field value from field " + fieldName(field), ex);

		return defValue;
	}

	public static <T> void setFieldValue(Field field, Object o, T value)
	{
		try
		{
			makeAccessible(field);
			field.set(o, value);
			return;
		}
		catch(IllegalArgumentException e)
		{
			Log.e(TAG, "setFieldValue: value=" + value);
			throw new WrappedCheckedException(e);
		}
		catch(IllegalAccessException e)
		{
			throw new WrappedCheckedException(e);
		}
	}

	public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes)
	{
		try
		{
			final Method m = clazz.getMethod(name, parameterTypes);
			m.setAccessible(true);
			return m;
		}
		catch(NoSuchMethodException e)
		{
			return null;
		}
	}

	public static Object invokeMethod(Method m, Object receiver, Object... args)
	{
		Exception ex;

		try
		{
			m.setAccessible(true);
			return m.invoke(receiver, args);
		}
		catch(IllegalArgumentException e)
		{
			ex = e;
		}
		catch(IllegalAccessException e)
		{
			ex = e;
		}
		catch(InvocationTargetException e)
		{
			ex = e;
		}

		throw new WrappedCheckedException(ex);
	}

	public static List<Field> getAllFields(Class<?> clazz) {
		return getAllFieldsUpTo(null, clazz);
	}

	public static <T> List<Field> getAllFieldsUpTo(Class<? super T> superClazz, Class<T> clazz)
	{
		if(clazz == Object.class)
			throw new IllegalArgumentException();

		final List<Field> fields = new ArrayList<Field>();

		Class<?> cls = clazz;

		do
		{
			getDeclaredFields(cls, fields);

		} while((cls != superClazz) && ((cls = cls.getSuperclass()) != Object.class));

		return fields;
	}

	/**
	 * Makes a <code>Field</code> accessible.
	 *
	 * @param field
	 * @return Whether the field's accessible status was changed.
	 */
	public static boolean makeAccessible(Field field)
	{
		if(!field.isAccessible())
		{
			field.setAccessible(true);
			return true;
		}

		return false;
	}

	public static <T> T newInstance(Class<T> clazz) {
		return newInstance(clazz, EMPTY_CLASS_ARRAY, EMPTY_OBJECT_ARRAY);
	}

	public static <T> T newInstance(Class<T> clazz, Object... args) {
		return newInstance(clazz, getTypes(args), args);
	}

	//@SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<T> clazz, Class<?>[] argTypes, Object... args)
	{
		if(argTypes == null || args == null)
		{
			argTypes = EMPTY_CLASS_ARRAY;
			args = EMPTY_OBJECT_ARRAY;
		}
		else if(argTypes.length != args.length)
			throw new IllegalArgumentException("argTypes.length != args.length");

		//Log.d(TAG, "newInstance: clazz=" + clazz + ", args=" + args);

		Exception ex;

		try
		{
			Constructor<T> ctor = clazz.getConstructor(argTypes);
			ctor.setAccessible(true);
			return ctor.newInstance(args);
		}
		catch (NoSuchMethodException e)
		{
			ex = e;
		}
		catch (IllegalArgumentException e)
		{
			ex = e;
		}
		catch (InstantiationException e)
		{
			ex = e;
		}
		catch (IllegalAccessException e)
		{
			ex = e;
		}
		catch (InvocationTargetException e)
		{
			ex = e;
		}

		throw new WrappedCheckedException(ex);
	}

	public static Class<?> classForName(String className)
	{
		try
		{
			return Class.forName(className);
		}
		catch(ClassNotFoundException e)
		{
			return null;
		}
	}

	/**
	 * Obtains the value of an <code>Annotation</code>'s parameter.
	 *
	 * @param annotation The annotation.
	 * @param parameterName The name of the <code>Annotation</code> parameter to obtain.
	 * @throws IllegalArgumentException If the parameter could not be obtained.
	 * @return The value of the parameter specified, cast to the receiving type.
	 */
	public static <T> T getAnnotationParameter(Annotation annotation, String parameterName) {
		return getAnnotationParameterInternal(annotation, parameterName, false);
	}


	/**
	 * Same as {@link #getAnnotationParameter(Annotation, String)}, but returns <code>null</code> on error.
	 *
	 * @see #getAnnotationParameter(Annotation, String)
	 */
	public static <T> T findAnnotationParameter(Annotation annotation, String parameterName) {
		return getAnnotationParameterInternal(annotation, parameterName, true);
	}

	public static Class<?>[] getTypes(Object[] args)
	{
		Class<?>[] types = new Class<?>[args.length];
		for(int i = 0; i != args.length; ++i)
			types[i] = args[i].getClass();

		return types;
	}

	private static void getDeclaredFields(Class<?> clazz, List<Field> outFields)
	{
		outFields.addAll(Arrays.asList(clazz.getDeclaredFields()));

		for(Field f : outFields)
			f.setAccessible(true);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getAnnotationParameterInternal(Annotation annotation, String parameterName, boolean returnNullOnError)
	{
		if(annotation == null)
			throw new NullPointerException("annotation");

		Exception ex;

		try
		{
			final Method m = annotation.getClass().getMethod(parameterName);
			m.setAccessible(true);
			return (T) m.invoke(annotation);
		}
		catch(NoSuchMethodException e)
		{
			ex = e;
		}
		catch(IllegalArgumentException e)
		{
			ex = e;
		}
		catch(IllegalAccessException e)
		{
			ex = e;
		}
		catch(InvocationTargetException e)
		{
			ex = e;
		}

		if(!returnNullOnError)
			throw new WrappedCheckedException("Failed to obtain parameter " + parameterName, ex);

		return null;
	}

	private static String fieldName(Field f)
	{
		Class<?> clazz = f.getDeclaringClass();
		return clazz.getSimpleName() + "." + f.getName();
	}

	private Reflect() {}

	private static final Class<?>[] EMPTY_CLASS_ARRAY = {};
	private static final Object[] EMPTY_OBJECT_ARRAY = {};
}

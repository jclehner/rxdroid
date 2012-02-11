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

package at.caspase.rxdroid.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.util.Log;

/**
 * Utility functions for reflection.
 *
 * @author Joseph Lehner
 *
 */
public final class Reflect
{
	private static final String TAG = Reflect.class.getName();
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
			return clazz.getDeclaredField(fieldName);
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

	public static Object getFieldValue(Field field, Object o, Object defValue)
	{
		Exception ex;

		try
		{
			return field.get(o);
		}
		catch(IllegalArgumentException e)
		{
			ex = e;
		}
		catch(IllegalAccessException e)
		{
			ex = e;
		}

		if(LOGV)
		{
			Class<?> clazz = field.getDeclaringClass();
			Log.d(TAG, "getFieldValue: failed to obtain field value from field " + fieldName(field), ex);
		}

		return defValue;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getAnnotationParameter(Annotation annotation, String parameterName)
	{
		if(annotation == null)
			throw new NullPointerException("annotation");

		Exception ex;

		try
		{
			final Method m = annotation.getClass().getMethod(parameterName);
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

		throw new RuntimeException("Failed to obtain paramter " + parameterName, ex);
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
		return newInstance(clazz, new Class<?>[0]);
	}

	public static <T> T newInstance(Class<T> clazz, Object... args) {
		return newInstance(clazz, getTypes(args), args);
	}

	//@SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<T> clazz, Class<?>[] argTypes, Object... args)
	{
		if(argTypes.length != args.length)
			throw new IllegalArgumentException("argTypes.length != args.length");

		Log.d(TAG, "newInstance: clazz=" + clazz + ", args=" + args);

		Exception ex;

		try
		{
			Constructor<T> ctor = clazz.getConstructor(argTypes);
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

		throw new RuntimeException(ex);
	}

	private static Class<?>[] getTypes(Object[] args)
	{
		Class<?>[] types = new Class<?>[args.length];
		for(int i = 0; i != args.length; ++i)
			types[i] = args[i].getClass();

		return types;
	}

	private static String fieldName(Field f)
	{
		Class<?> clazz = f.getDeclaringClass();
		return clazz.getSimpleName() + "." + f.getName();
	}

	private Reflect() {}
}

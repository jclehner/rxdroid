package at.jclehner.androidutils;

import java.lang.reflect.Array;
import java.util.Arrays;

import at.jclehner.rxdroid.util.Util;

public final class Objects
{
	public static String toString(Object o)
	{
		if(o == null)
			return "null";
		else if(o.getClass().isArray())
			return arrayToString(o);
		else
			return o.toString();
	}

	public static String arrayToString(Object array)
	{
		if(array == null)
			return "null";

		final Class<?> type = array.getClass();
		if(!type.isArray())
			throw new IllegalArgumentException("Not an array");

		final int length = Array.getLength(array);
		if(length == 0)
			return "[]";

		final StringBuilder sb = new StringBuilder("[");

		for(int i = 0; i != length; ++i)
		{
			if(i != 0)
				sb.append(", ");

			final Object elem = Array.get(array, i);
			if(elem != null)
			{
				final Class<?> elemType = elem.getClass();

				if(elemType.isArray())
					sb.append(arrayToString(elem));
				else if(elemType == String.class)
					sb.append("\"" + elem.toString() + "\""); // TODO escape string?
				else
					sb.append(elem.toString());
			}
			else
				sb.append("null");
		}

		return sb.append("]").toString();
	}

	private Objects() {}
}

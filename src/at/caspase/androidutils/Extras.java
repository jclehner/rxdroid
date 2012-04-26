package at.caspase.androidutils;

import java.util.WeakHashMap;

import android.os.Bundle;

public class Extras
{
	private static WeakHashMap<Object, Bundle> sExtras = new WeakHashMap<Object, Bundle>();

	public static Bundle get(Object object)
	{
		if(!isRegistered(object))
			sExtras.put(object, new Bundle());

		return sExtras.get(object);
	}

	public static boolean isRegistered(Object object) {
		return sExtras.containsKey(object);
	}
}

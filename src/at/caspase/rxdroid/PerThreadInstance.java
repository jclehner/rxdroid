package at.caspase.rxdroid;

import java.util.HashMap;

import at.caspase.androidutils.Reflect;

/**
 * Per-thread instance storage with caching.
 *
 * This class implements a storage for global per-thread instances, caching them
 * based on specified constructor arguments.
 *
 * Usage example:
 * <code>
 * // If PerThreadInstance.get() was called with ctor args of "yyyy-MM-dd" before
 * // in the calling thread, this instance will be returned. Otherwise, a new
 * // object will be instantiated using new SimpleDateFormat("yyyy-MM-dd");
 * SimpleDateFormat sdf = PerThreadInstance.get(SimpleDateFormat.class, "yyyy-MM-dd");
 * </code>
 *
 * @author Joseph Lehner
 *
 */
public final class PerThreadInstance extends ThreadLocal<HashMap<Class<?>, HashMap<Object[], Object>>>
{
	// Reminder to self: premature optimization is the root of all evil!
	private static final boolean ENABLED = false;

	private static final PerThreadInstance DATA = ENABLED ? new PerThreadInstance() : null;

	public static <T> T get(Class<? extends T> clazz, Object... ctorArgs)
	{
		if(!ENABLED)
			return Reflect.newInstance(clazz, ctorArgs);

		synchronized(PerThreadInstance.DATA)
		{
			HashMap<Object[], Object> classData = PerThreadInstance.DATA.get(clazz);
			if(classData == null)
			{
				classData = new HashMap<Object[], Object>();
				PerThreadInstance.DATA.get().put(clazz, classData);
			}

			@SuppressWarnings("unchecked")
			T value = (T) classData.get(ctorArgs);
			if(value == null)
			{
				value = Reflect.newInstance(clazz, ctorArgs);
				classData.put(ctorArgs, value);
			}

			return value;
		}
	}

	@Override
	protected HashMap<Class<?>, HashMap<Object[], Object>> initialValue() {
		return new HashMap<Class<?>, HashMap<Object[],Object>>();
	}

	private HashMap<Object[], Object> get(Class<?> clazz) {
		return get().get(clazz);
	}
}

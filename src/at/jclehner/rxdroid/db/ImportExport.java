package at.jclehner.rxdroid.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import at.caspase.rxdroid.Fraction;
import at.jclehner.androidutils.Reflect;
import at.jclehner.rxdroid.util.CollectionUtils;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTableConfig;

public final class ImportExport
{
	private static final String TAG = ImportExport.class.getName();

	public interface JsonPersister<T>
	{
		public String toJsonString(T value) throws JSONException;
		public T fromJsonString(String string) throws JSONException;

		public T nullValue();
	}

	public static abstract class JsonPersisterBase<T> implements JsonPersister<T>
	{
		@Override
		public T nullValue() {
			return null;
		}
	}

	public interface JsonForeignPersister<T>
	{
		//public static final long NO_ID = Integer.MIN_VALUE;

		public long toId(T value);
		public T fromId(long id);

		public long nullId();
	}

	@SuppressWarnings("rawtypes")
	private static HashMap<Class<?>, JsonPersister> sPersisters =
			new HashMap<Class<?>, JsonPersister>();

	@SuppressWarnings("rawtypes")
	private static HashMap<Class<?>, JsonForeignPersister> sForeignPersisters =
			new HashMap<Class<?>, JsonForeignPersister>();

	static
	{
		sPersisters.put(Fraction.class, new JsonPersisters.FractionPersister());
		sPersisters.put(Date.class, new JsonPersisters.DatePersister());

		register(Drug.class, new JsonPersisters.ForeignDrugPersister());
		register(Schedule.class, new JsonPersisters.ForeignSchedulePersister());
	}

	public static <T> void register(Class<? extends T> clazz, JsonPersister<T> persister) {
		sPersisters.put(clazz, persister);
	}

	public static <T> void register(Class<? extends T> clazz, JsonForeignPersister<T> persister) {
		sForeignPersisters.put(clazz, persister);
	}

	public static JSONObject tableToJsonObject(Class<?> clazz, Collection<?> entries) throws JSONException
	{
		final JSONArray array = new JSONArray();

		for(Object t : entries)
			array.put(entryToJsonObject(t));

		return new JSONObject().put(DatabaseTableConfig.extractTableName(clazz), array);
	}

	public static void tableFromJsonObject(JSONObject json, Collection<?> outEntries)
	{
		throw new UnsupportedOperationException();
	}

	public static JSONObject entryToJsonObject(Object object) throws JSONException
	{
		final Class<?> clazz = object.getClass();
		final JSONObject json = new JSONObject();

		for(Field f : clazz.getDeclaredFields())
		{
			Annotation a = f.getAnnotation(DatabaseField.class);
			if(a == null)
				continue;

			final Class<?> type = f.getType();
			final String name = getColumnName(f, a);
			final Object value = Reflect.getFieldValue(f, object);

			if(!isForeignField(f, a))
			{
				if(isJsonable(type) || isFloat(type))
				{
					if(value != null)
						json.put(name, value);
					else
						json.put(name, JSONObject.NULL);
				}
				else
				{
					if(value == null)
						json.put(name, JSONObject.NULL);
					else
						json.put(name, toJsonStringInternal(value, type));
				}
			}
			else
				json.put(name, getForeignIdInternal(value, type));
		}

		return json;
	}

	public static void entryFromJsonObject(JSONObject json, Object outObject) throws JSONException
	{
		final Class<?> clazz = outObject.getClass();

		for(Field f : clazz.getFields())
		{
			Annotation a = f.getAnnotation(DatabaseField.class);
			if(a == null)
				continue;

			final Class<?> type = f.getType();
			final String name = getColumnName(f, a);
			final Object value;

			if(!isForeignField(f, a))
			{
				if(isJsonable(type))
					value = json.get(name);
				else if(isFloat(type))
					value = (float) json.getDouble(name);
				else
					value = fromJsonStringInternal(json.getString(name), clazz);
			}
			else
				value = fromForeignIdInternal(json.getLong(name), type);

			try
			{
				f.set(outObject, value);
			}
			catch(IllegalArgumentException e)
			{
				throw new JSONException(e.getMessage());
			}
			catch(IllegalAccessException e)
			{
				throw new JSONException(e.getMessage());
			}
		}
	}

	public static JsonPersister<?> getPersister(Class<?> clazz) throws JSONException
	{
		final JsonPersister<?> persister = sPersisters.get(clazz);
		if(persister == null)
			throw new JSONException("No registered persister for " + clazz.getName());

		return persister;
	}

	public static JsonForeignPersister<?> getForeignPersister(Class<?> clazz) throws JSONException
	{
		final JsonForeignPersister<?> persister = sForeignPersisters.get(clazz);
		if(persister == null)
			throw new JSONException("No registered foreign persister for " + clazz.getName());

		return persister;
	}

	@SuppressWarnings("unchecked")
	private static long getForeignIdInternal(Object value, Class<?> type) throws JSONException
	{
		@SuppressWarnings("rawtypes")
		final JsonForeignPersister persister = getForeignPersister(type);
		return value != null ? persister.toId(value) : persister.nullId();
	}

	private static Object fromForeignIdInternal(long id, Class<?> type) throws JSONException
	{
		@SuppressWarnings("rawtypes")
		final JsonForeignPersister persister = getForeignPersister(type);

		if(id == persister.nullId())
			return null;

		return persister.fromId(id);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static String toJsonStringInternal(Object value, Class<?> type) throws JSONException
	{
		if(value == null)
			throw new NullPointerException();

		final JsonPersister persister = getPersister(type);
		return persister.toJsonString(value);
	}

	private static Object fromJsonStringInternal(String string, Class<?> clazz) throws JSONException
	{
		final JsonPersister<?> persister = getPersister(clazz);

		if(string == null || string.length() == 0)
			return persister.nullValue();

		return persister.fromJsonString(string);
	}

	private static String getColumnName(Field f, Annotation a)
	{
		String columnName = Reflect.getAnnotationParameter(a, "columnName");
		if(columnName == null || columnName.length() == 0 || DatabaseField.DEFAULT_STRING.equals(columnName))
			columnName = f.getName();

		return columnName;
	}

	private static boolean isForeignField(Field f, Annotation a) {
		return Reflect.getAnnotationParameter(a, "foreign");
	}

	private static final Class<?>[] JSONABLE = {
		boolean.class, double.class, int.class, long.class,
		Boolean.class, Double.class, Integer.class, Long.class,
		String.class
	};

	private static boolean isJsonable(Class<?> clazz) {
		return CollectionUtils.indexOfByReference(clazz, JSONABLE) != -1;
	}

	private static boolean isFloat(Class<?> clazz) {
		return clazz == float.class || clazz == Float.class;
	}

	private ImportExport() {}
}

package at.jclehner.rxdroid.db;

import java.sql.SQLException;

import at.caspase.rxdroid.Fraction;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;

public class FractionPersister extends StringType
{
	private static FractionPersister sInstance = new FractionPersister();

	public static FractionPersister getSingleton() {
		return sInstance;
	}

	private FractionPersister() {
		super(SqlType.STRING, new Class<?>[] { String.class });
	}

	protected FractionPersister(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object javaObject) throws SQLException {
		return ((Fraction) javaObject).toString();
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) throws SQLException {
		return valueOf(sqlArg);
	}

	@Override
	public Object resultStringToJava(FieldType fieldType, String stringValue, int columnPos) throws SQLException {
		return valueOf(stringValue);
	}

	private static Fraction valueOf(Object object)
	{
		if(object == null)
			return Fraction.ZERO;

		return Fraction.valueOf((String) object);
	}
}

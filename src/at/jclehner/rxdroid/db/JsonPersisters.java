package at.jclehner.rxdroid.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;

import at.caspase.rxdroid.Fraction;
import at.jclehner.rxdroid.db.ImportExport.JsonPersister;

public final class JsonPersisters
{
	public static class FractionPersister implements JsonPersister<Fraction>
	{
		@Override
		public String toJsonString(Fraction fraction) throws JSONException {
			return fraction.toString();
		}

		@Override
		public Fraction fromJsonString(String string) throws JSONException
		{
			try
			{
				return Fraction.valueOf(string);
			}
			catch(NumberFormatException e)
			{
				throw new JSONException(e.getMessage());
			}
		}
	}

	public static class DatePersister implements JsonPersister<Date>
	{
		private static final String FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZ";

		@Override
		public String toJsonString(Date date) {
			return getSimpleDateFormat().format(date);
		}

		@Override
		public Date fromJsonString(String data) throws JSONException
		{
			try
			{
				return getSimpleDateFormat().parse(data);
			}
			catch(ParseException e)
			{
				throw new JSONException(e.getMessage());
			}
		}

		private static SimpleDateFormat getSimpleDateFormat() {
			return new SimpleDateFormat(FORMAT, Locale.US);
		}
	}

	private JsonPersisters() {}
}

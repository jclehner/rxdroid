package at.jclehner.rxdroid.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.db.ImportExport.JsonForeignPersister;
import at.jclehner.rxdroid.db.ImportExport.JsonPersisterBase;

public final class JsonPersisters
{
	public static class FractionPersister extends JsonPersisterBase<Fraction>
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

		@Override
		public Fraction nullValue() {
			return Fraction.ZERO;
		}
	}

	public static class DatePersister extends JsonPersisterBase<Date>
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

	public static class ForeignDrugPersister extends ForeignEntryPersister<Drug>
	{
		@Override
		public Drug fromId(long id)
		{
			final Drug drug = new Drug();
			drug.id = (int) id;

			return drug;
		}
	}

	public static class ForeignSchedulePersister extends ForeignEntryPersister<Schedule>
	{
		@Override
		public Schedule fromId(long id)
		{
			final Schedule schedule = new Schedule();
			schedule.id = (int) id;
			return schedule;
		}
	}

	private static abstract class ForeignEntryPersister<E extends Entry> implements JsonForeignPersister<E>
	{
		public long toId(E entry) {
			return entry.id;
		}

		@Override
		public long nullId() {
			return -1;
		}
	}

	private JsonPersisters() {}
}

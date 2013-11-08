package at.jclehner.rxdroid.db.v58;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import at.jclehner.androidutils.Reflect;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.DatabaseHelper;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.WrappedCheckedException;

public class ScheduleCreator
{
	private static final String TAG = ScheduleCreator.class.getSimpleName();

	private static List<Runnable> sOnDbInitRunnables =
			new ArrayList<Runnable>();

	private static final Database.OnInitializedListener sOnDbInitListener =
		new Database.OnInitializedListener() {
				@Override
				public void onDatabaseInitialized()
				{
					for(Runnable r : sOnDbInitRunnables)
						r.run();

					sOnDbInitRunnables.clear();
				}
	};

	static {
		Database.registerOnInitializedListener(sOnDbInitListener);
	}

	public static void createScheduleFromDrug(final Entry oldDrug, final Drug newDrug)
	{
		final Schedule schedule = new Schedule();
		boolean useRepeatOriginAsBegin = false;

		final int repeatMode = (Integer) getFieldValue(oldDrug, "repeatMode");
		final long repeatArg = (Long) getFieldValue(oldDrug, "repeatArg");

		switch(repeatMode)
		{
			case OldDrug.REPEAT_AS_NEEDED:
				schedule.setAsNeeded(true);
				// fall through

			case OldDrug.REPEAT_DAILY:
				schedule.setRepeatMode(Schedule.REPEAT_DAILY);
				break;

			case OldDrug.REPEAT_21_7:
				schedule.setRepeatDailyWithPause(21, 7);
				useRepeatOriginAsBegin = true;
				break;

			case OldDrug.REPEAT_WEEKDAYS:
				schedule.setRepeatMode(Schedule.REPEAT_WEEKDAYS);
				schedule.setRepeatArg(repeatArg);
				useRepeatOriginAsBegin = true;
				break;

			case OldDrug.REPEAT_EVERY_N_DAYS:
				schedule.setRepeatMode(Schedule.REPEAT_EVERY_N_DAYS);
				schedule.setRepeatArg(repeatArg);
				useRepeatOriginAsBegin = true;
				break;

			case OldDrug.REPEAT_CUSTOM:
				/* no - op */
				return;

			default:
				throw new IllegalArgumentException("repeatMode=" + repeatMode);
		}

		final Date repeatOrigin = getFieldValue(oldDrug, "repeatOrigin");
		final Date lastScheduleUpdateDate = getFieldValue(oldDrug, "lastScheduleUpdateDate");

		Date begin;

		if(useRepeatOriginAsBegin)
			begin = getFieldValue(oldDrug, "repeatOrigin");
		else
			begin = getFieldValue(oldDrug, "lastScheduleUpdateDate");

		schedule.setBegin(begin);

		for(int doseTime : Schedule.DOSE_TIMES)
			schedule.setDose(doseTime, getDose(oldDrug, doseTime));

		newDrug.addSchedule(schedule);

		sOnDbInitRunnables.add(new Runnable()
		{
			@Override
			public void run()
			{
				if(schedule.getBegin() == null)
				{
					Date begin = null;

					for(DoseEvent event : Entries.findDoseEvents(newDrug, null, null))
					{
						final Date date = event.getDate();

						if(begin == null)
							begin = event.getDate();
						else if(date.before(begin))
							begin = date;
					}

					if(begin == null)
					{
						Log.i(TAG, newDrug.getName() + ": falling back to today for schedule begin");
						begin = DateTime.today();
					}

					Log.i(TAG, newDrug.getName() + ": setting begin to " + DateTime.toDateString(begin));
					schedule.setBegin(begin);
				}

				//Database.create(schedule);
				DatabaseHelper.createAfterUpgrade(schedule);
			}
		});
	}

	private static Fraction getDose(Entry oldDrug, int doseTime)
	{
		switch(doseTime)
		{
			case Schedule.TIME_MORNING:
				return getFieldValue(oldDrug, "doseMorning");
			case Schedule.TIME_NOON:
				return getFieldValue(oldDrug, "doseNoon");
			case Schedule.TIME_EVENING:
				return getFieldValue(oldDrug, "doseEvening");
			case Schedule.TIME_NIGHT:
				return getFieldValue(oldDrug, "doseNight");
			default:
				throw new IllegalArgumentException();
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T getFieldValue(Entry entry, String fieldName)
	{
		try
		{
			Field f = entry.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			return (T) f.get(entry);
		}
		catch(NoSuchFieldException e)
		{
			throw new WrappedCheckedException(e);
		}
		catch(IllegalAccessException e)
		{
			throw new WrappedCheckedException(e);
		}
	}
}

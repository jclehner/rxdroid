package at.jclehner.rxdroid.db.v58;

import java.lang.reflect.Field;
import java.util.Date;

import at.jclehner.androidutils.Reflect;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.util.WrappedCheckedException;

public class ScheduleCreator
{
	public static Schedule createScheduleFromDrug(Entry oldDrug)
	{
		final Schedule schedule = new Schedule();
		boolean useRepeatOriginAsBegin = false;

		final int repeatMode = getFieldValue(oldDrug, "repeatMode");
		final long repeatArg = getFieldValue(oldDrug, "repeatArg");

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
				return null;

			default:
				throw new IllegalArgumentException("repeatMode=" + repeatMode);
		}

		final Date repeatOrigin = getFieldValue(oldDrug, "repeatOrigin");
		final Date lastScheduleUpdateDate = getFieldValue(oldDrug, "lastScheduleUpdateDate");

		schedule.setBegin(useRepeatOriginAsBegin ? repeatOrigin : lastScheduleUpdateDate);

		for(int doseTime : Schedule.DOSE_TIMES)
			schedule.setDose(doseTime, getDose(oldDrug, doseTime));

		return schedule;
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

	private static <T> T getFieldValue(Entry entry, String fieldName)
	{
		try
		{
			Field f = entry.getClass().getField(fieldName);
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

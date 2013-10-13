package at.jclehner.rxdroid.db.v58;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.db.Schedule;

public class ScheduleCreator
{
	public void migrateToNewSchedule(OldDrug drug)
	{
		final Schedule schedule = new Schedule();
		boolean useRepeatOriginAsBegin = false;

		switch(drug.repeatMode)
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
				schedule.setRepeatArg((int) drug.repeatArg);
				useRepeatOriginAsBegin = true;
				break;

			case OldDrug.REPEAT_EVERY_N_DAYS:
				schedule.setRepeatMode(Schedule.REPEAT_EVERY_N_DAYS);
				schedule.setRepeatArg((int) drug.repeatArg);
				useRepeatOriginAsBegin = true;

			case OldDrug.REPEAT_CUSTOM:
				/* no - op */
				return;

			default:
				throw new IllegalArgumentException("repeatMode=" + drug.repeatMode);
		}

		schedule.setBegin(useRepeatOriginAsBegin ? drug.repeatOrigin : drug.lastScheduleUpdateDate);

		for(int doseTime : Schedule.DOSE_TIMES)
			schedule.setDose(doseTime, getDose(drug, doseTime));
	}

	private Fraction getDose(OldDrug drug, int doseTime)
	{
		switch(doseTime)
		{
			case Schedule.TIME_MORNING:
				return drug.doseMorning;
			case Schedule.TIME_NOON:
				return drug.doseNoon;
			case Schedule.TIME_EVENING:
				return drug.doseEvening;
			case Schedule.TIME_NIGHT:
				return drug.doseNight;
			default:
				throw new IllegalArgumentException();
		}
	}

	public int toScheduleRepeatMode(int drugRepeatMode)
	{
		/*
		public static final int REPEAT_DAILY = 0;
	public static final int REPEAT_EVERY_N_DAYS = 1;
	public static final int REPEAT_WEEKDAYS = 2;
	public static final int REPEAT_AS_NEEDED = 3;
	public static final int REPEAT_21_7 = 4; // for oral contraceptives, 21 days on, 7 off
	public static final int REPEAT_CUSTOM = 5;
	public static final int REPEAT_INVALID = 6;
		 */

		switch(drugRepeatMode)
		{
			case OldDrug.REPEAT_DAILY:
				return Schedule.REPEAT_DAILY;

			case OldDrug.REPEAT_EVERY_N_DAYS:
				return Schedule.REPEAT_EVERY_N_DAYS;

			case OldDrug.REPEAT_WEEKDAYS:
				return Schedule.REPEAT_WEEKDAYS;

			default:
				throw new IllegalArgumentException("drugRepeatMode=" + drugRepeatMode);
		}
	}
}
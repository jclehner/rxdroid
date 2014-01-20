package at.jclehner.rxdroid;

import at.jclehner.rxdroid.db.Schedule;

public final class DoseTime
{
	public static int after(int doseTime)
	{
		requireValidDoseTime(doseTime);

		if(doseTime == Schedule.TIME_NIGHT)
			return Schedule.TIME_MORNING;

		return doseTime + 1;
	}

	public static int before(int doseTime)
	{
		requireValidDoseTime(doseTime);

		if(doseTime == Schedule.TIME_MORNING)
			return Schedule.TIME_NIGHT;

		return doseTime - 1;
	}

	private static void requireValidDoseTime(int doseTime)
	{
		if(doseTime == Schedule.TIME_INVALID)
			throw new IllegalArgumentException();
	}

	private DoseTime() {}
}

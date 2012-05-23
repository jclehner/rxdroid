package at.caspase.rxdroid.db;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;

public final class Entries
{
	private static final List<Intake> sConsolidatedIntakes = new ArrayList<Intake>();

	public static Intake findAllIntakesConsolidated(Drug drug, Date date, int doseTime)
	{
		final List<Intake> intakes = Intake.findAll(drug, date, doseTime);

		Intake intake = new Intake();

		return intake;
	}

	public static boolean hasMissingIntakesBeforeDate(Drug drug, Date date)
	{
		int repeatMode = drug.getRepeatMode();

		switch(repeatMode)
		{
			case Drug.REPEAT_EVERY_N_DAYS:
			case Drug.REPEAT_WEEKDAYS:
			{
				if(drug.hasDoseOnDate(date))
					return false;
			}
		}

		if(repeatMode == Drug.REPEAT_EVERY_N_DAYS)
		{
			long days = drug.getRepeatArg();

			Date origin = drug.getRepeatOrigin();
			if(date.before(origin))
				return false;

			long elapsed = date.getTime() - origin.getTime();

			int offset = (int) -(elapsed % days);
			if(offset == 0)
				offset = (int) -days;

			Date lastIntakeDate = DateTime.add(date, Calendar.DAY_OF_MONTH, offset);

			return Intake.hasAll(drug, lastIntakeDate);
		}
		else if(repeatMode == Drug.REPEAT_WEEKDAYS)
		{
			// FIXME this may fail to report a missed Intake if
			// the dose was taken on a later date (i.e. unscheduled)
			// in the week before.

			int expectedIntakeCount = 0;
			int actualScheduledIntakeCount = 0;

			for(int i = 0; i != 7; ++i)
			{
				final Date checkDate = DateTime.add(date, Calendar.DAY_OF_MONTH, -7 + i);

				for(int doseTime : Constants.DOSE_TIMES)
				{
					if(!drug.getDose(doseTime, checkDate).isZero())
					{
						++expectedIntakeCount;
						actualScheduledIntakeCount += Intake.countAll(drug, checkDate, doseTime);
					}
				}
			}

			return actualScheduledIntakeCount < expectedIntakeCount;
		}

		return false;
	}




	private Entries() {}
}

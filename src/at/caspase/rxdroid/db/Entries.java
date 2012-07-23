package at.caspase.rxdroid.db;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;

public final class Entries
{
	/*
	private static final List<Intake> sConsolidatedIntakes = new ArrayList<Intake>();

	public static Intake findAllIntakesConsolidated(Drug drug, Date date, int doseTime)
	{
		final List<Intake> intakes = Intake.findAll(drug, date, doseTime);

		Intake intake = new Intake();

		return intake;
	}
	*/
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

			final Date origin = drug.getRepeatOrigin();
			if(date.before(origin))
				return false;

			long elapsedDays = (date.getTime() - origin.getTime()) / Constants.MILLIS_PER_DAY;

			int offset = (int) -(elapsedDays % days);
			if(offset == 0)
				offset = (int) -days;

			final Date lastIntakeDate = DateTime.add(date, Calendar.DAY_OF_MONTH, offset);
			return !hasAllIntakes(drug, lastIntakeDate);
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
						actualScheduledIntakeCount += countIntakes(drug, checkDate, doseTime);
					}
				}
			}

			return actualScheduledIntakeCount < expectedIntakeCount;
		}

		return false;
	}

	/**
	 * Get the number of days the drug's supply will last.
	 */
	public static int getSupplyDaysLeftForDrug(Drug drug, Date date)
	{
		// TODO this function currently does not take into account doses
		// that were taken after the specified date.

		if(date == null)
			date = DateTime.todayDate();

		final Fraction doseLeftOnDate = new Fraction();

		if(date.equals(DateTime.todayDate()) && drug.hasDoseOnDate(date))
		{
			for(int doseTime : Constants.DOSE_TIMES)
			{
				if(countIntakes(drug, date, doseTime) == 0)
					doseLeftOnDate.add(drug.getDose(doseTime, date));
			}
		}

		final double supply = drug.getCurrentSupply().doubleValue() - doseLeftOnDate.doubleValue();
		return (int) Math.floor(supply / getDailyDose(drug) * getSupplyCorrectionFactor(drug));
	}

	public static<T extends Entry> T findInCollectionById(Collection<T> collection, int id)
	{
		for(T t : collection)
		{
			if(t.getId() == id)
				return t;
		}

		return null;
	}

	/**
	 * Find all intakes meeting the specified criteria.
	 * <p>
	 * @param drug The drug to search for (based on its database ID).
	 * @param date The Intake's date. Can be <code>null</code>.
	 * @param doseTime The Intake's doseTime. Can be <code>null</code>.
	 */
	public static synchronized List<Intake> findIntakes(Drug drug, Date date, Integer doseTime)
	{
		final List<Intake> intakes = new LinkedList<Intake>();

		for(Intake intake : Database.getCached(Intake.class))
		{
			if(intake.getDrugId() != drug.id)
				continue;
			if(date != null && !intake.getDate().equals(date))
				continue;
			if(doseTime != null && intake.getDoseTime() != doseTime)
				continue;

			intakes.add(intake);
		}

		return Collections.unmodifiableList(intakes);
	}

	public static int countIntakes(Drug drug, Date date, Integer doseTime) {
		return findIntakes(drug, date, doseTime).size();
	}

	public static boolean hasAllIntakes(Drug drug, Date date)
	{
		if(!drug.hasDoseOnDate(date))
			return true;

		//List<Intake> intakes = findAll(drug, date, null);
		for(int doseTime : Constants.DOSE_TIMES)
		{
			Fraction dose = drug.getDose(doseTime, date);
			if(!dose.isZero())
			{
				if(countIntakes(drug, date, doseTime) == 0)
					return false;
			}
		}

		return true;
	}

	private static double getDailyDose(Drug drug)
	{
		double dailyDose = 0.0;
		for(int doseTime : Constants.DOSE_TIMES)
			dailyDose += drug.getDose(doseTime).doubleValue();
		return dailyDose;
	}

	private static double getSupplyCorrectionFactor(Drug drug)
	{
		switch(drug.getRepeatMode())
		{
			case Drug.REPEAT_EVERY_N_DAYS:
				return drug.getRepeatArg();

			case Drug.REPEAT_WEEKDAYS:
				return 7.0 / Long.bitCount(drug.getRepeatArg());

			case Drug.REPEAT_21_7:
				return 1.0 / 0.75;

			default:
				return 1.0;
		}
	}

	private Entries() {}
}

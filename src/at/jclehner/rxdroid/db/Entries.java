/**
 * Copyright (C) 2011, 2012 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 * This file is part of RxDroid.
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package at.jclehner.rxdroid.db;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.Fraction.MutableFraction;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;

public final class Entries
{
	@SuppressWarnings("unused")
	private static final String TAG = Entries.class.getName();

	private static final String[] TIME_NAMES = {
		"MORNING", "NOON", "EVENING", "NIGHT"
	};

	public static List<Drug> getAllDrugs(int patientId)
	{
		final List<Drug> list = new ArrayList<Drug>();

		for(Drug drug : Database.getCached(Drug.class))
		{
			final boolean matches;

			if(patientId == 0)
				matches = (drug.getPatient() == null || drug.getPatient().isDefaultPatient());
			else
				matches = (patientId == drug.getPatientId());

			if(matches)
				list.add(drug);
		}

		return list;
	}

	public static CharSequence[] getAllPatientNames()
	{
		final List<Patient> patients = Database.getCached(Patient.class);
		final String[] names = new String[patients.size()];

		for(int i = 0; i != names.length; ++i)
			names[i] = patients.get(i).getName();

		return names;
	}

	public static boolean hasMissingIntakesBeforeDate(Drug drug, Date date)
	{
		if(!drug.isActive())
			return false;

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
			date = DateTime.today();

		final MutableFraction doseLeftOnDate = new MutableFraction();

		if(date.equals(DateTime.today()) && drug.hasDoseOnDate(date))
		{
			for(int doseTime : Constants.DOSE_TIMES)
			{
				if(countIntakes(drug, date, doseTime) == 0)
					doseLeftOnDate.add(drug.getDose(doseTime, date));
			}
		}

		final double supply = drug.getCurrentSupply().doubleValue() - doseLeftOnDate.doubleValue();
		return (int) (Math.floor(supply / getDailyDose(drug) * getSupplyCorrectionFactor(drug)));
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
	public static List<Intake> findIntakes(Drug drug, Date date, Integer doseTime)
	{
		final List<Intake> intakes = new LinkedList<Intake>();

		for(Intake intake : Database.getCached(Intake.class))
		{
			if(Intake.has(intake, drug, date, doseTime))
				intakes.add(intake);
		}

		return intakes;
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

	public static String getDoseTimeString(int doseTime) {
		return TIME_NAMES[doseTime];
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

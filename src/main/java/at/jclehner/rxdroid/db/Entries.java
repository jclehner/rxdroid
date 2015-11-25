/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2014 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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

import android.util.Log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.Fraction.MutableFraction;
import at.jclehner.rxdroid.R;
import at.jclehner.rxdroid.RxDroid;
import at.jclehner.rxdroid.Settings;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import org.joda.time.Days;
import org.joda.time.LocalDate;

public final class Entries
{
	@SuppressWarnings("unused")
	private static final String TAG = Entries.class.getSimpleName();

	private static final Dao<DoseEvent, Integer> sDoseEventDao =
		Database.USE_CUSTOM_CACHE ? null : Database.getHelper().getDaoChecked(DoseEvent.class);

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

	public static boolean hasMissingDosesBeforeDate(Drug drug, Date date)
	{
		if(!drug.isActive())
			return false;

		final Date lastScheduleUpdateDate = drug.getLastScheduleUpdateDate();
		if(lastScheduleUpdateDate != null && date.before(lastScheduleUpdateDate))
			return false;

		final int repeatMode = drug.getRepeatMode();
		/*switch(repeatMode)
		{
			case Drug.REPEAT_EVERY_N_DAYS:
			case Drug.REPEAT_WEEKDAYS:
			{
				if(drug.hasDoseOnDate(date))
					return false;
			}
		}*/

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
			if(lastScheduleUpdateDate != null && lastIntakeDate.compareTo(drug.getLastScheduleUpdateDate()) == -1)
				return false;

			//if(!isDateAfterLastScheduleUpdateOfDrug(lastIntakeDate, drug))
			//	return false;

			return !hasAllDoseEvents(drug, lastIntakeDate);
		}
		else if(repeatMode == Drug.REPEAT_WEEKDAYS)
		{
			// FIXME this may fail to report a missed intake if
			// the dose was taken on a later date (i.e. unscheduled)
			// in the week before.

			int expectedIntakeCount = 0;
			int actualScheduledIntakeCount = 0;

			for(int i = 0; i != 7; ++i)
			{
				final Date checkDate = DateTime.add(date, Calendar.DAY_OF_MONTH, -7 + i);
				if(!isDateAfterLastScheduleUpdateOfDrug(checkDate, drug))
					continue;

				for(int doseTime : Constants.DOSE_TIMES)
				{
					if(!drug.getDose(doseTime, checkDate).isZero())
					{
						++expectedIntakeCount;
						actualScheduledIntakeCount += countDoseEvents(drug, checkDate, doseTime);
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
				if(countDoseEvents(drug, date, doseTime) == 0)
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
	 * Find all events meeting the specified criteria.
	 * <p>
	 * @param drug The drug to search for (based on its database ID).
	 * @param date The intake's date. Can be <code>null</code>.
	 * @param doseTime The intake's doseTime. Can be <code>null</code>.
	 */
	public static List<DoseEvent> findDoseEvents(Drug drug, Date date, Integer doseTime)
	{
		if(Database.USE_CUSTOM_CACHE)
		{
			final List<DoseEvent> events = new LinkedList<DoseEvent>();

			for(DoseEvent intake : Database.getCached(DoseEvent.class))
			{
				if(DoseEvent.has(intake, drug, date, doseTime))
					events.add(intake);
			}

			return events;
		}
		else
		{
			try
			{
				final QueryBuilder<DoseEvent, Integer> qb = sDoseEventDao.queryBuilder();
				final Where<DoseEvent, Integer> where = qb.where();

				if(drug != null)
					where.eq("drug_id", drug.id).and();

				if(date != null)
					where.eq("date", date).and();

				if(doseTime != null)
					where.eq("doseTime", doseTime);

				return sDoseEventDao.query(qb.prepare());
			}
			catch(SQLException e)
			{
				throw new WrappedCheckedException(e);
			}
		}
	}

	public static int countDoseEvents(Drug drug, Date date, Integer doseTime) {
		return findDoseEvents(drug, date, doseTime).size();
	}

	public static boolean hasAllDoseEvents(Drug drug, Date date) {
		return hasAllDoseEvents(drug, date, Schedule.TIME_INVALID);
	}

	public static boolean hasAllDoseEvents(Drug drug, Date date, int untilDoseTime) {
		return hasAllDoseEvents(drug, date, untilDoseTime, true);
	}

	public static boolean hasAllDoseEvents(Drug drug, Date date, int untilDoseTime, boolean inclusiveUntilDoseTime)
	{
		final int lastDoseTime = inclusiveUntilDoseTime ? untilDoseTime : untilDoseTime - 1;
		if(lastDoseTime < Schedule.TIME_MORNING)
			return true;

		if(!drug.hasDoseOnDate(date))
			return true;

		for(int doseTime : Constants.DOSE_TIMES)
		{
			Fraction dose = drug.getDose(doseTime, date);
			if(!dose.isZero())
			{
				if(countDoseEvents(drug, date, doseTime) == 0)
					return false;
			}

			if(doseTime == lastDoseTime)
				break;
		}

		return true;
	}

	public static String getDoseTimeString(int doseTime) {
		return TIME_NAMES[doseTime];
	}

	public static Fraction getTotalDoseInTimePeriod_dumb(Drug drug, Date begin, Date end, boolean stopIfSupplyIsEmpty)
	{
		final MutableFraction totalDose = new MutableFraction();

		final Calendar cal = DateTime.calendarFromDate(begin);
		cal.add(Calendar.DAY_OF_MONTH, 1);
		Date date;

		while((date = cal.getTime()).before(end) || date.equals(end))
		{
			getTotalDose(drug, date, totalDose);

			if(totalDose.isNegative() && stopIfSupplyIsEmpty)
				return drug.getCurrentSupply();

			cal.add(Calendar.DAY_OF_MONTH, 1);
		}

		return totalDose;
	}

	public static Fraction getTotalDoseInTimePeriod_smart(Drug drug, Date begin, Date end)
	{
		final int repeatMode = drug.getRepeatMode();
		final MutableFraction baseDose = new MutableFraction();
		int doseMultiplier = 0;

		if(drug.isAsNeeded())
			return Fraction.ZERO;

		if(repeatMode == Drug.REPEAT_DAILY)
		{
			getTotalDose(drug, null, baseDose);
			doseMultiplier = (int) DateTime.diffDays(begin, end);
		}
		else if(repeatMode == Drug.REPEAT_EVERY_N_DAYS)
		{
			getTotalDose(drug, null, baseDose);

			final long arg = drug.getRepeatArg();
			final long daysInPeriod = DateTime.diffDays(begin, end);

			if(drug.hasDoseOnDate(begin) || drug.hasDoseOnDate(end))
				doseMultiplier = 1;

			doseMultiplier += daysInPeriod / arg;
		}
		else if(repeatMode == Drug.REPEAT_WEEKDAYS)
		{
			getTotalDose(drug, null, new MutableFraction());

			final Calendar cal = DateTime.calendarFromDate(begin);
			int weekDay;

			// Manually check the days before the first Monday
			while((weekDay = cal.get(Calendar.DAY_OF_WEEK)) != Calendar.MONDAY)
			{
				if(drug.hasDoseOnWeekday(weekDay))
					++doseMultiplier;

				cal.add(Calendar.DAY_OF_WEEK, 1);
			}

			final Date firstMonday = cal.getTime();

			// Now check the days after the last Sunday
			cal.setTime(end);

			while((weekDay = cal.get(Calendar.DAY_OF_WEEK)) != Calendar.MONDAY)
			{
				if(drug.hasDoseOnWeekday(weekDay))
					++doseMultiplier;

				cal.add(Calendar.DAY_OF_WEEK, -1);
			}

			final Date lastSunday = cal.getTime();

			// Now comes the easy part: dealing with the full week(s) in between
			final long days = DateTime.diffDays(firstMonday, lastSunday);
			if(days > 7)
			{
				if(days % 7 != 0)
					throw new IllegalStateException("Not a full week: " + firstMonday + " - " + lastSunday);

				doseMultiplier += days / 7 * Long.bitCount(drug.getRepeatArg());
			}
		}
		else if(repeatMode == Drug.REPEAT_21_7)
		{
			getTotalDose(drug, null, baseDose);

			final Date origin = drug.getRepeatOrigin();

			long daysInTimePeriod = DateTime.diffDays(begin, end);
			final long daysFromOriginToBegin = DateTime.diffDays(origin, begin);
			final long daysFromOriginToEnd = DateTime.diffDays(origin, end);

			long index = daysFromOriginToBegin % 28;
			if(index < 21)
			{
				final long days = 21 - index;
				daysInTimePeriod -= days + 7;
				doseMultiplier += days;
			}

			index = daysFromOriginToEnd % 28;
			if(index < 21)
			{
				final long days = 21 - index;
				daysInTimePeriod -= days + 7;
				doseMultiplier += days;
			}

			if(daysInTimePeriod > 28)
				doseMultiplier += (daysInTimePeriod * 3) / 4;
		}
		else
			throw new UnsupportedOperationException();

		return baseDose.times(doseMultiplier);
	}

	public static boolean isDateAfterLastScheduleUpdateOfDrug(Date date, Drug drug)
	{
		final Date lastScheduleUpdateDate = drug.getLastScheduleUpdateDate();
		if(lastScheduleUpdateDate == null)
			return true;

		return date.after(lastScheduleUpdateDate);
	}

	public static boolean hasLowSupplies(Drug drug, Date date)
	{
		if(!drug.isActive() || drug.getRefillSize() == 0 || drug.hasNoDoses())
			return false;

		final int minSupplyDays = Settings.getStringAsInt(Settings.Keys.LOW_SUPPLY_THRESHOLD, 10);
		if(minSupplyDays == 0)
			return false;

		final int supplyDaysLeft = getSupplyDaysLeftForDrug(drug, date);

		final LocalDate scheduleEnd = drug.getScheduleEndDate();
		if(scheduleEnd != null)
		{
			if(supplyDaysLeft >= Days.daysBetween(scheduleEnd, LocalDate.fromDateFields(date)).getDays())
				return false;
		}

		return getSupplyDaysLeftForDrug(drug, date) < minSupplyDays;
	}

	public static LocalDate getSupplyEndDate(Drug drug, Date date) {
		return LocalDate.fromDateFields(date).plusDays(getSupplyDaysLeftForDrug(drug, date));
	}

	public static boolean willExpireSoon(Drug drug, Date date)
	{
		if(!drug.isActive() || drug.getRefillSize() == 0)
			return false;

		final LocalDate expirationDate = drug.getExpiryDate();
		if(expirationDate == null)
			return false;

		final LocalDate scheduleEnd = drug.getScheduleEndDate();
		if(scheduleEnd != null && expirationDate.isAfter(scheduleEnd))
			return false;

		final int minSupplyDays = Settings.getStringAsInt(Settings.Keys.LOW_SUPPLY_THRESHOLD, 10);
		if(minSupplyDays == 0)
			return false;

		return LocalDate.fromDateFields(date).plusDays(minSupplyDays).isAfter(expirationDate);
	}

	public static String getDrugName(Drug drug)
	{
		final String name = drug.getName();
		// this should never happen unless there's a DB problem
		if(name == null || name.length() == 0)
			return "<???>";

		if(Settings.getBoolean(Settings.Keys.SCRAMBLE_NAMES, false))
		{
			// We rot13 word by word and ignore those beginning with
			// a digit, so things like 10mg won't get converted to 10zt.

			final StringBuilder sb = new StringBuilder(name.length());
			final String[] tokens = name.split(" ");

			for(int i = 0; i != tokens.length; ++i)
			{
				if(i != 0)
					sb.append(" ");

				final String token = tokens[i];

				if(token.length() == 0 || Character.isDigit(token.charAt(0)))
					sb.append(token);
				else
					sb.append(Util.rot13(token));
			}

			return sb.toString();
		}

		return name;
	}

	public static void markAllNotifiedDosesAsTaken(int patientId)
	{
		final Settings.DoseTimeInfo dtInfo = Settings.getDoseTimeInfo();

		final boolean isActiveDoseTime;

		Date date = dtInfo.activeDate();
		int doseTime = dtInfo.activeDoseTime();

		if(doseTime == Schedule.TIME_INVALID)
		{
			isActiveDoseTime = false;
			doseTime = dtInfo.nextDoseTime();
			date = dtInfo.nextDoseTimeDate();
		}
		else
			isActiveDoseTime = true;


		final List<Drug> drugs = getAllDrugs(patientId);
		final List<DoseEvent> events = new ArrayList<DoseEvent>();

		if(isActiveDoseTime)
			getDrugsWithDueDoses(drugs, date, doseTime, events);

		getDrugsWithMissedDoses(drugs, date, doseTime, isActiveDoseTime, events);

		int skipped = 0, taken = 0;

		for(DoseEvent event : events)
		{
			final boolean skip;

			final Drug drug = event.getDrug();
			final Fraction dose = event.getDose();

			final Fraction supply = drug.getCurrentSupply();
			if(!supply.isZero())
			{
				final Fraction newSupply = supply.minus(dose);
				if(!newSupply.isNegative())
				{
					drug.setCurrentSupply(newSupply);
					Database.update(drug);
					skip = false;
				}
				else
					skip = true;
			}
			else
				skip = drug.getRefillSize() != 0;

			if(skip)
			{
				++skipped;
				event.setDose(Fraction.ZERO);
			}
			else
				++taken;

			Database.create(event);

			Log.d(TAG, "Creating event: " + event);
		}

		if(skipped != 0)
			RxDroid.toastLong(R.string._toast_some_doses_skipped);
		else if(taken != 0)
			RxDroid.toastLong(R.string._toast_all_doses_taken);
		else
			RxDroid.toastShort(R.string._toast_no_doses_to_take);
	}

	public static int getDrugsWithDueDoses(List<Drug> inDrugs, Date date, int doseTime, List<DoseEvent> outEvents)
	{
		int count = 0;

		for(Drug drug: inDrugs)
		{
			final Fraction dose = drug.getDose(doseTime, date);

			if(!drug.isActive() || dose.isZero() || drug.hasAutoDoseEvents() || drug.isAsNeeded())
				continue;

			if(Entries.countDoseEvents(drug, date, doseTime) == 0)
			{
				++count;

				if(outEvents != null)
					outEvents.add(new DoseEvent(drug, date, doseTime, dose));
			}
		}

		return count;
	}

	public static int getDrugsWithMissedDoses(List<Drug> inDrugs, Date date, int activeOrNextDoseTime, boolean isActiveDoseTime, List<DoseEvent> outEvents)
	{
		int begin = Schedule.TIME_MORNING;
		int end = Schedule.TIME_INVALID;

		if(!isActiveDoseTime && activeOrNextDoseTime == Drug.TIME_MORNING)
		{
			// FIXME fails if we're between end of TIME_NIGHT and midnight!
			date = DateTime.add(date, Calendar.DAY_OF_MONTH, -1);
			begin = Drug.TIME_NIGHT;
		}
		else
			end = activeOrNextDoseTime;

		int count = 0;

		for(int doseTime = begin; doseTime != end; ++doseTime)
			count += getDrugsWithDueDoses(inDrugs, date, doseTime, outEvents);

		return count;
	}

	private static void getTotalDose(Drug drug, Date date, MutableFraction outTotalDose)
	{
		if((date != null && !drug.hasDoseOnDate(date)) || drug.isAsNeeded())
			return;

		for(int doseTime : Constants.DOSE_TIMES)
		{
			final Fraction dose;

			if(date == null)
				dose = drug.getDose(doseTime);
			else
				dose = drug.getDose(doseTime, date);

			outTotalDose.add(dose);
		}

		return;
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

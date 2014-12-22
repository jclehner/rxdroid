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

package at.jclehner.rxdroid;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.util.Log;
import at.jclehner.rxdroid.Settings.DoseTimeInfo;
import at.jclehner.rxdroid.Settings.Keys;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.DoseEvent;
import at.jclehner.rxdroid.db.Patient;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;

public enum DoseEventJanitor implements
		NotificationReceiver.OnDoseTimeChangeListener,
		/*Database.OnChangeListener,*/
		Database.OnInitializedListener
{
	INSTANCE;

	private static final String TAG = DoseEventJanitor.class.getSimpleName();
	private static final boolean LOGV = false;

	@Override
	public void onDoseTimeBegin(Date date, int doseTime) {
		// do nothing
	}

	@Override
	public void onDoseTimeEnd(Date date, int doseTime)
	{
		if(LOGV) Log.v(TAG, "onDoseTimeEnd");
		createDoseEvents(date, doseTime);
	}

	@Override
	public void onDatabaseInitialized()
	{
		Settings.init();

		if(Database.countAll(Patient.class) == 0)
			Database.create(new Patient(), Database.FLAG_DONT_NOTIFY_LISTENERS);

		for(Drug drug : Database.getAll(Drug.class))
			createAutoDoseEvents(drug);

		////////////////////////////////////

		final Date today = DateTime.today();

		final List<DoseEvent> events = Database.getAll(DoseEvent.class);
//		final ArrayList<Integer> idsToDelete = new ArrayList<Integer>();
		final int oldIntakeCount = events.size();
		int deleteCount = 0;

		Date oldest = null, newest = null;

		for(DoseEvent intake : events)
		{
			final Date date = intake.getDate();

			if(Settings.isPastMaxHistoryAge(today, date))
			{
				if(oldest == null || date.before(oldest))
					oldest = date;

				if(newest == null || date.after(newest))
					newest = date;

				++deleteCount;
				Database.delete(intake, Database.FLAG_DONT_NOTIFY_LISTENERS);

				//intake.getDrug().setLastDosesClearedDate(date);

//				idsToDelete.add(intake.getId());
			}
		}

//		Database.deleteByIds(intake.class, idsToDelete);

		final int deletedPercentage = (int) (deleteCount == 0 ? 0 : (deleteCount * 100.0) / oldIntakeCount);

		Log.i(TAG, "Deleted " + deleteCount + " entries (~" + deletedPercentage + "%); oldest: " +
				(oldest == null ? "N/A" : DateTime.toDateString(oldest)) + ", newest: " +
				(oldest == null ? "N/A" : DateTime.toDateString(newest)));

		if(newest != null)
		{
			// the day after the most recent dose event that was just deleted is the oldest possible
			// date for any dose event.
			newest = DateTime.add(newest, Calendar.DAY_OF_MONTH, 1);

			final Date oldestPossibleDoseEventTime = Settings.getDate(Keys.OLDEST_POSSIBLE_DOSE_EVENT_TIME);
			if(oldestPossibleDoseEventTime == null || newest.after(oldestPossibleDoseEventTime))
				Settings.putDate(Keys.OLDEST_POSSIBLE_DOSE_EVENT_TIME, newest);
		}
	}

	public static void registerSelf()
	{
		//Database.registerEventListener(INSTANCE);
		Database.registerOnInitializedListener(INSTANCE);
		NotificationReceiver.registerOnDoseTimeChangeListener(INSTANCE);
	}

	private static void createDoseEvents(Date date, int doseTime)
	{
		for(Drug drug : Database.getAll(Drug.class))
			createDoseEvent(drug, date, doseTime);
	}

	private static void createAutoDoseEvents(Drug drug)
	{
		if(!drug.hasAutoDoseEvents())
			return;

		Date date = drug.getLastAutoDoseEventCreationDate();
		if(date == null)
			throw new IllegalStateException();

		if(LOGV) Log.v(TAG, "createMissingIntakes: drug=" + drug + ", date=" + date);

		final DoseTimeInfo dtInfo = Settings.getDoseTimeInfo();

		while(date.before(dtInfo.activeDate()))
		{
			for(int doseTime : Constants.DOSE_TIMES)
			{
				createDoseEvent(drug, date, doseTime);
			}

			if(LOGV) Log.v(TAG, "  date=" + date);
			date = DateTime.add(date, Calendar.DAY_OF_MONTH, 1);
		}

		for(int doseTime = Schedule.TIME_MORNING; doseTime != dtInfo.nextDoseTime(); ++doseTime)
			createDoseEvent(drug, dtInfo.activeDate(), doseTime);
	}

	private static void createDoseEvent(Drug drug, Date date, int doseTime)
	{
		if(!drug.hasAutoDoseEvents())
			return;

		final Fraction dose = drug.getDose(doseTime, date);
		if(dose.isZero())
			return;

		final Fraction newSupply = drug.getRefillSize() != 0 ?
				drug.getCurrentSupply().minus(dose) : Fraction.ZERO;

		if(newSupply.isNegative())
			return;

		if(Entries.countDoseEvents(drug, date, doseTime) != 0)
			return;

		if(BuildConfig.DEBUG) Log.v(TAG, "createDoseEvent: drug=" + drug + ", date=" + date + ", doseTime=" + doseTime);

		final DoseEvent intake = new DoseEvent(drug, date, doseTime, dose);
		intake.setWasAutoCreated(true);

		if(doseTime == Schedule.TIME_NIGHT)
			drug.setLastAutoDoseEventCreationDate(date);
		else
		{
			final Date lastAutoIntakeCreationDate = drug.getLastAutoDoseEventCreationDate();
			if(lastAutoIntakeCreationDate == null)
				drug.setLastAutoDoseEventCreationDate(DateTime.yesterday());
			else if(DateTime.diffDays(date, lastAutoIntakeCreationDate) != 1)
				drug.setLastAutoDoseEventCreationDate(DateTime.add(date, Calendar.DAY_OF_MONTH, -1));
		}

		drug.setCurrentSupply(newSupply);

		Database.create(intake, Database.FLAG_DONT_NOTIFY_LISTENERS);
		Database.update(drug, Database.FLAG_DONT_NOTIFY_LISTENERS);
	}
}


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

package at.caspase.rxdroid;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.util.Log;
import at.caspase.rxdroid.Settings.DoseTimeInfo;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entries;
import at.caspase.rxdroid.db.Intake;
import at.caspase.rxdroid.db.Schedule;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;

public enum AutoIntakeCreator implements
		NotificationReceiver.OnDoseTimeChangeListener,
		/*Database.OnChangeListener,*/
		Database.OnInitializedListener
{
	INSTANCE;

	private static final String TAG = AutoIntakeCreator.class.getName();
	private static final boolean LOGV = true;

	@Override
	public void onDoseTimeBegin(Date date, int doseTime) {
		// do nothing
	}

	@Override
	public void onDoseTimeEnd(Date date, int doseTime)
	{
		if(LOGV) Log.v(TAG, "onDoseTimeEnd");
		createIntakes(date, doseTime);
	}

	/*@Override
	public void onEntryCreated(Entry entry, int flags)
	{
		if(LOGV) Log.v(TAG, "onEntryCreated");
		onEntryUpdated(entry, flags);
	}

	@Override
	public void onEntryUpdated(Entry entry, int flags)
	{
		if(!(entry instanceof Drug))
			return;

		if(LOGV) Log.v(TAG, "onEntryUpdated");

		final Drug drug = (Drug) entry;
		if(!drug.isAutoAddIntakesEnabled())
			return;

		createMissingIntakes(drug);
	}

	@Override
	public void onEntryDeleted(Entry entry, int flags) {
		// do nothing
	}*/

	@Override
	public void onDatabaseInitialized()
	{
		for(Drug drug : Database.getAll(Drug.class))
			createMissingIntakes(drug);

		////////////////////////////////////

		final Date today = DateTime.today();
		final long maxHistoryAgeInDays = Settings.getMaxHistoryAgeInDays();

		final List<Intake> intakes = Database.getAll(Intake.class);
		final int oldIntakeCount = intakes.size();
		int deleteCount = 0;

		for(Intake intake : intakes)
		{
			long diffDays = DateTime.diffDays(today, intake.getDate());
			if(diffDays > maxHistoryAgeInDays)
				++deleteCount;
		}
		final double deletedPercentage = deleteCount == 0 ? 0 : (deleteCount * 100.0) / oldIntakeCount;

		Log.d(TAG, "Would have deleted " + deleteCount + " entries older than " + maxHistoryAgeInDays + " (~" + deletedPercentage + "%)");
	}

	public static void registerSelf()
	{
		//Database.registerEventListener(INSTANCE);
		Database.registerOnInitializedListener(INSTANCE);
		NotificationReceiver.registerOnDoseTimeChangeListener(INSTANCE);
	}

	private static void createIntakes(Date date, int doseTime)
	{
		for(Drug drug : Database.getAll(Drug.class))
			createIntake(drug, date, doseTime);
	}

	private static void createMissingIntakes(Drug drug)
	{
		if(!drug.isAutoAddIntakesEnabled())
			return;

		Date date = drug.getLastAutoIntakeCreationDate();
		if(date == null)
			throw new IllegalStateException();

		if(LOGV) Log.v(TAG, "createMissingIntakes: drug=" + drug + ", date=" + date);

		final DoseTimeInfo dtInfo = Settings.getDoseTimeInfo();

		while(date.before(dtInfo.activeDate()))
		{
			for(int doseTime : Constants.DOSE_TIMES)
			{
				createIntake(drug, date, doseTime);
			}

			if(LOGV) Log.v(TAG, "  date=" + date);
			date = DateTime.add(date, Calendar.DAY_OF_MONTH, 1);
		}

		for(int doseTime = Schedule.TIME_MORNING; doseTime != dtInfo.nextDoseTime(); ++doseTime)
			createIntake(drug, dtInfo.activeDate(), doseTime);
	}

	private static void createIntake(Drug drug, Date date, int doseTime)
	{
		if(!drug.isAutoAddIntakesEnabled())
			return;

		final Fraction dose = drug.getDose(doseTime, date);
		if(dose.isZero())
			return;

		final Fraction newSupply = drug.getCurrentSupply().minus(dose);
		if(newSupply.isNegative())
			return;

		if(Entries.countIntakes(drug, date, doseTime) != 0)
			return;

		Log.d(TAG, "createIntake: drug=" + drug + ", date=" + date + ", doseTime=" + doseTime);

		final Intake intake = new Intake(drug, date, doseTime, dose);
		intake.setWasAutoCreated(true);

		if(doseTime == Schedule.TIME_NIGHT)
			drug.setLastAutoIntakeCreationDate(date);
		else
		{
			final Date lastAutoIntakeCreationDate = drug.getLastAutoIntakeCreationDate();
			if(lastAutoIntakeCreationDate == null)
				drug.setLastAutoIntakeCreationDate(DateTime.yesterday());
			else if(DateTime.diffDays(date, lastAutoIntakeCreationDate) != 1)
				drug.setLastAutoIntakeCreationDate(DateTime.add(date, Calendar.DAY_OF_MONTH, -1));
		}

		drug.setCurrentSupply(newSupply);

		Database.create(intake, Database.FLAG_DONT_NOTIFY_LISTENERS);
		Database.update(drug, Database.FLAG_DONT_NOTIFY_LISTENERS);
	}
}


/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
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

package at.jclehner.rxdroid;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.util.Log;
import at.jclehner.rxdroid.Settings.DoseTimeInfo;
import at.jclehner.rxdroid.db.Database;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entries;
import at.jclehner.rxdroid.db.Intake;
import at.jclehner.rxdroid.db.Patient;
import at.jclehner.rxdroid.db.Schedule;
import at.jclehner.rxdroid.util.Constants;
import at.jclehner.rxdroid.util.DateTime;

public enum AutoIntakeCreator implements
		NotificationReceiver.OnDoseTimeChangeListener,
		/*Database.OnChangeListener,*/
		Database.OnInitializedListener
{
	INSTANCE;

	private static final String TAG = AutoIntakeCreator.class.getSimpleName();
	private static final boolean LOGV = false;

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

	@Override
	public void onDatabaseInitialized()
	{
		Settings.init();

		if(Database.countAll(Patient.class) == 0)
			Database.create(new Patient(), Database.FLAG_DONT_NOTIFY_LISTENERS);

		SplashScreenActivity.setStatusMessage(R.string._title_db_status_creating_intakes);

		for(Drug drug : Database.getAll(Drug.class))
			createMissingIntakes(drug);

		////////////////////////////////////

		SplashScreenActivity.setStatusMessage(R.string._title_db_status_discarding_intakes);

		final Date today = DateTime.today();

		final List<Intake> intakes = Database.getAll(Intake.class);
//		final ArrayList<Integer> idsToDelete = new ArrayList<Integer>();
		final int oldIntakeCount = intakes.size();
		int deleteCount = 0;

		Date oldest = null;

		for(Intake intake : intakes)
		{
			final Date date = intake.getDate();

			if(Settings.isPastMaxHistoryAge(today, date))
			{
				if(oldest == null || date.before(oldest))
					oldest = date;

				++deleteCount;
				Database.delete(intake, Database.FLAG_DONT_NOTIFY_LISTENERS);

				intake.getDrug().setLastDosesClearedDate(date);

//				idsToDelete.add(intake.getId());
			}
		}

//		Database.deleteByIds(Intake.class, idsToDelete);

		final int deletedPercentage = (int) (deleteCount == 0 ? 0 : (deleteCount * 100.0) / oldIntakeCount);

		Log.i(TAG, "Deleted " + deleteCount + " entries (~" + deletedPercentage + "%); oldest entry: " +
				(oldest == null ? "N/A" : DateTime.toDateString(oldest)));
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

		if(LOGV) Log.v(TAG, "createIntake: drug=" + drug + ", date=" + date + ", doseTime=" + doseTime);

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


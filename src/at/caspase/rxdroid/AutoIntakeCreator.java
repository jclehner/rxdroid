package at.caspase.rxdroid;

import java.util.Calendar;
import java.util.Date;

import android.util.Log;
import at.caspase.rxdroid.db.Database;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entries;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.Intake;
import at.caspase.rxdroid.db.Schedule;
import at.caspase.rxdroid.db.Database.OnChangeListener;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;

public enum AutoIntakeCreator implements
		NotificationReceiver.OnDoseTimeChangeListener,
		Database.OnChangeListener
{
	INSTANCE;

	private static final String TAG = AutoIntakeCreator.class.getName();

	private static final String LAST_DATE = TAG + ".last_date";

	@Override
	public void onDoseTimeBegin(Date date, int doseTime) {
		// do nothing
	}

	@Override
	public void onDoseTimeEnd(Date date, int doseTime)
	{
		createIntakes(date, doseTime);

		if(doseTime == Schedule.DOSE_NIGHT)
			Settings.putDate(LAST_DATE, date);
	}

	@Override
	public void onEntryCreated(Entry entry, int flags)
	{
		if(!(entry instanceof Drug))
			return;

		final Drug drug = (Drug) entry;
		if(!drug.isAutoAddIntakesEnabled())
			return;

		//Settings.get

	}

	@Override
	public void onEntryUpdated(Entry entry, int flags) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEntryDeleted(Entry entry, int flags) {
		// TODO Auto-generated method stub

	}

	public void onDatabaseLoaded()
	{
		final Date today = DateTime.todayDate();
		final Date lastDate = Settings.getDate(LAST_DATE);

		Date date;
		if(lastDate == null)
			date = DateTime.todayDate();
		else
			date = DateTime.add(lastDate, Calendar.DAY_OF_MONTH, 1);

		while(date.before(today))
		{
			for(int doseTime : Constants.DOSE_TIMES)
				INSTANCE.createIntakes(date, doseTime);

			date = DateTime.add(date, Calendar.DAY_OF_MONTH,1);
		}
	}

	private void createIntakes(Date date, int doseTime)
	{
		for(Drug drug : Database.getAll(Drug.class))
		{
			if(!drug.isAutoAddIntakesEnabled())
				continue;

			final Fraction dose = drug.getDose(doseTime, date);
			if(dose.isZero() || Entries.countIntakes(drug, date, doseTime) != 0)
				continue;

			final Intake intake = new Intake(drug, date, doseTime, dose);
			intake.setWasAutoCreated(true);
			Database.create(intake);

			drug.setCurrentSupply(drug.getCurrentSupply().minus(dose));
			Database.update(drug);

			Log.d(TAG, "createIntakes: " + intake);
		}
	}

	static
	{
		Database.registerEventListener(INSTANCE);
	}
}


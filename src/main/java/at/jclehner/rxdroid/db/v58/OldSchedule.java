package at.jclehner.rxdroid.db.v58;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.FractionPersister;
import at.jclehner.rxdroid.db.Schedule;

@SuppressWarnings({ "unused", "serial" })
@DatabaseTable(tableName="schedules")
public class OldSchedule extends Entry
{
	@DatabaseField
	private String name;

	@DatabaseField(canBeNull = false)
	/* package */ Date begin;

	@DatabaseField
	/* package */ Date end;

	@DatabaseField
	private int repeatMode = Schedule.REPEAT_DAILY;

	@DatabaseField
	private long repeatArg;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseMorning;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNoon;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseEvening;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNight;

	@DatabaseField(foreign = true)
	/* package */ transient Drug owner;
		
	@Override
	public Entry convertToCurrentDatabaseFormat()
	{
		final Schedule newSchedule = new Schedule();
		Entry.copy(newSchedule, this);

		if(repeatMode == Schedule.REPEAT_AS_NEEDED)
		{
			newSchedule.setAsNeeded(true);
			newSchedule.setRepeatMode(Schedule.REPEAT_DAILY);
		}
		else
			newSchedule.setRepeatMode(at.jclehner.rxdroid.db.v57.OldSchedule.toCurrentScheduleRepeatMode(repeatMode));

		return newSchedule;
	}

	@Override
	public boolean equals(Object other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}
}

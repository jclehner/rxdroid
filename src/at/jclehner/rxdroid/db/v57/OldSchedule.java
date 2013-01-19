package at.jclehner.rxdroid.db.v57;

import java.util.Date;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.FractionPersister;
import at.jclehner.rxdroid.db.Schedule;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

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
	private int repeatMode;

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

	@Override
	public Entry convertToCurrentDatabaseFormat()
	{
		final Schedule newSchedule = new Schedule();
		Entry.copy(newSchedule, this);
		// TODO some more magic here?
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

package at.jclehner.rxdroid.db.v58;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.FractionPersister;
import at.jclehner.rxdroid.db.Patient;

@SuppressWarnings({ "unused", "serial" })
@DatabaseTable(tableName = "drugs")
public class OldDrug extends Entry
{
	public static final int REPEAT_DAILY = 0;
	public static final int REPEAT_EVERY_N_DAYS = 1;
	public static final int REPEAT_WEEKDAYS = 2;
	public static final int REPEAT_AS_NEEDED = 3;
	public static final int REPEAT_21_7 = 4; // for oral contraceptives, 21 days on, 7 off
	public static final int REPEAT_CUSTOM = 5;
	public static final int REPEAT_INVALID = 6;

	@DatabaseField(unique = true)
	/* package */ String name;

	@DatabaseField(foreign = true)
	/* package */ Patient patient;

	@DatabaseField
	/* package */ int icon;

	@DatabaseField
	/* package */ boolean active = true;

	@DatabaseField
	/* package */ int refillSize;

	@DatabaseField(persisterClass = FractionPersister.class)
	/* package */ Fraction currentSupply = Fraction.ZERO;

	@DatabaseField(persisterClass = FractionPersister.class)
	/* package */ Fraction doseMorning = Fraction.ZERO;

	@DatabaseField(persisterClass = FractionPersister.class)
	/* package */ Fraction doseNoon = Fraction.ZERO;

	@DatabaseField(persisterClass = FractionPersister.class)
	/* package */ Fraction doseEvening = Fraction.ZERO;

	@DatabaseField(persisterClass = FractionPersister.class)
	/* package */ Fraction doseNight = Fraction.ZERO;

	@DatabaseField
	/* package */ int repeatMode= Drug.REPEAT_DAILY;

	@DatabaseField
	/* package */ long repeatArg = 0;

	@DatabaseField
	/* package */ Date repeatOrigin;

	@DatabaseField(columnName = "autoAddIntakes")
	/* package */ boolean hasAutoDoseEvents = false;

	@DatabaseField(columnName = "lastAutoIntakeCreationDate")
	/* package */ Date lastAutoDoseEventCreationDate;

	@DatabaseField
	/* package */ Date lastScheduleUpdateDate;

//	@DatabaseField
//	/* package */ Date lastDosesClearedDate;

	@DatabaseField
	/* package */ int sortRank = Integer.MAX_VALUE;

	@DatabaseField
	/* package */ String comment;
		
	@Override
	public Entry convertToCurrentDatabaseFormat()
	{
		final Drug newDrug = new Drug();
		Entry.copy(newDrug, this);
		// TODO some more magic here?
		return newDrug;
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

package at.jclehner.rxdroid.db.v55;

import java.util.Date;

import at.caspase.rxdroid.Fraction;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.FractionPersister;
import at.jclehner.rxdroid.db.Schedule;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings({ "unused", "serial" })
@DatabaseTable(tableName = "drugs")
public class OldDrug extends Entry
{
	@DatabaseField(unique = true)
	private String name;

	@DatabaseField
	private int icon;

	@DatabaseField
	private boolean active = true;

	@DatabaseField
	private int refillSize;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction currentSupply = Fraction.ZERO;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseMorning = Fraction.ZERO;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNoon = Fraction.ZERO;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseEvening = Fraction.ZERO;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNight = Fraction.ZERO;

	@DatabaseField
	private int repeatMode= Drug.REPEAT_DAILY;

	@DatabaseField
	private long repeatArg = 0;

	@DatabaseField
	private Date repeatOrigin;

	@DatabaseField
	private boolean autoAddIntakes = false;

	@DatabaseField
	private Date lastAutoIntakeCreationDate;

	@DatabaseField
	private int sortRank = Integer.MAX_VALUE;

	@DatabaseField(foreign = true)
	private Schedule schedule;

	@DatabaseField
	private String comment;

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

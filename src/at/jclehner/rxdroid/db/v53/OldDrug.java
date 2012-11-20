package at.jclehner.rxdroid.db.v53;

import java.util.Date;

import at.caspase.rxdroid.Fraction;
import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.Schedule;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings({ "unused", "serial" })
@DatabaseTable(tableName = "drugs")
public class OldDrug extends Entry
{
	@DatabaseField(unique = true)
	private String name;

	@DatabaseField(columnName = "form")
	private int icon;

	@DatabaseField
	private boolean active = true;

	@DatabaseField
	private int refillSize;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction currentSupply = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseMorning = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseNoon = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseEvening = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseNight = new Fraction();

	@DatabaseField(columnName = "repeat")
	private int repeatMode= Drug.REPEAT_DAILY;

	/**
	 * Defines the repeat origin.
	 *
	 * For every repeat other than {@link #REPEAT_DAILY}, this field holds a specific value,
	 * allowing {@link #hasDoseOnDate(Date)} to determine whether a dose is pending
	 * on a specific date.
	 *
	 * <ul>
	 *     <li><code>FREQ_EVERY_OTHER_DAY</code>: field is set to a date (in milliseconds) where this drug's
	 *         intake should be set, i.e. if the date corresponds to 2011-09-07, there's an intake on that day,
	 *         another one on 2011-09-09, and so forth.</li>
	 *     <li><code>FREQ_WEEKLY</code>: field is set to a week day value from {@link java.util.Calendar}.</li>
	 * </ul>
	 */
	@DatabaseField
	private long repeatArg = 0;

	@DatabaseField
	private Date repeatOrigin;

	@DatabaseField
	private boolean autoAddIntakes = false;

	@DatabaseField
	private Date lastAutoIntakeCreationDate;

	@DatabaseField
	protected int sortRank = 0;

	@DatabaseField(foreign = true)
	private Schedule schedule;

	@DatabaseField
	private String comment;

	@Override
	public Entry convertToCurrentDatabaseFormat()
	{
		final Drug newDrug = new Drug();
		Entry.copy(newDrug, this);
		newDrug.setIcon(icon);
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

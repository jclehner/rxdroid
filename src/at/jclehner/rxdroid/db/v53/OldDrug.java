package at.jclehner.rxdroid.db.v53;

import java.util.Date;

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
	private at.caspase.rxdroid.Fraction currentSupply;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private at.caspase.rxdroid.Fraction doseMorning;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private at.caspase.rxdroid.Fraction doseNoon;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private at.caspase.rxdroid.Fraction doseEvening;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private at.caspase.rxdroid.Fraction doseNight;

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

		newDrug.setCurrentSupply(convertFraction(currentSupply));

		newDrug.setDose(Schedule.TIME_MORNING, convertFraction(doseMorning));
		newDrug.setDose(Schedule.TIME_NOON, convertFraction(doseNoon));
		newDrug.setDose(Schedule.TIME_EVENING, convertFraction(doseEvening));
		newDrug.setDose(Schedule.TIME_NIGHT, convertFraction(doseNight));

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

	/* package */ static at.jclehner.rxdroid.Fraction convertFraction(at.caspase.rxdroid.Fraction f)
	{
		int[] data = f.getFractionData(false);
		return new at.jclehner.rxdroid.Fraction(data[1], data[2]);
	}
}

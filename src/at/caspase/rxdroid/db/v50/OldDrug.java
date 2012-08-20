package at.caspase.rxdroid.db.v50;

import java.util.Date;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.Schedule;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings({ "unused", "serial" })
@DatabaseTable(tableName = "drugs")
public class OldDrug extends Entry
{
	@DatabaseField(unique = true)
	private String name;

	@DatabaseField
	private int form;

	@DatabaseField(defaultValue = "true")
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

	@DatabaseField(canBeNull = true, columnName = "repeat")
	private int repeatMode= Drug.REPEAT_DAILY;

	@DatabaseField(canBeNull = true)
	private long repeatArg = 0;

	@DatabaseField(canBeNull = true)
	private Date repeatOrigin;

	@DatabaseField
	private int sortRank = 0;

	@DatabaseField(foreign = true, canBeNull = true)
	private Schedule schedule;

	@DatabaseField(canBeNull = true)
	private String comment;

	@Override
	public Entry convertToCurrentDatabaseFormat()
	{
		Drug newDrug = new Drug();
		Entry.copy(newDrug, this);
		newDrug.setIsSupplyMonitorOnly(false);
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

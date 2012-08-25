package at.caspase.rxdroid.db.v51;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.Intake;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings({ "unused", "serial" })
@DatabaseTable(tableName = "intake")
public class OldIntake extends Entry
{
	@DatabaseField(foreign = true)
	private Drug drug;

	@DatabaseField
	private java.util.Date date;

	@DatabaseField
	private java.util.Date timestamp;

	@DatabaseField
	private int doseTime;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction dose;

	@Override
	public Entry convertToCurrentDatabaseFormat()
	{
		Intake newEntry = new Intake();
		Entry.copy(newEntry, this);
		// TODO some more magic here?
		return newEntry;
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

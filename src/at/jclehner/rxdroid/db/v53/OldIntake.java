package at.jclehner.rxdroid.db.v53;

import at.jclehner.rxdroid.db.Drug;
import at.jclehner.rxdroid.db.Entry;
import at.jclehner.rxdroid.db.Intake;

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
	private at.caspase.rxdroid.Fraction dose;

	@DatabaseField
	private boolean wasAutoCreated = false;

	@Override
	public Entry convertToCurrentDatabaseFormat()
	{
		final Intake newIntake = new Intake();
		Entry.copy(newIntake, this);

		newIntake.setDose(OldDrug.convertFraction(dose));

		return newIntake;
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

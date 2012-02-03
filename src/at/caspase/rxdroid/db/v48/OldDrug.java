package at.caspase.rxdroid.db.v48;

import java.util.Date;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entry;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@SuppressWarnings({"serial", "unused"})
@DatabaseTable(tableName="drugs")
public class OldDrug extends Entry
{
	public OldDrug() {}

	@DatabaseField
	private Date repeatOrigin;

	@DatabaseField
	private String comment;

	@DatabaseField(dataType=DataType.SERIALIZABLE)
	private Fraction currentSupply;

	@DatabaseField(dataType=DataType.SERIALIZABLE)
	private Fraction doseEvening;

	@DatabaseField(dataType=DataType.SERIALIZABLE)
	private Fraction doseMorning;

	@DatabaseField(dataType=DataType.SERIALIZABLE)
	private Fraction doseNight;

	@DatabaseField(dataType=DataType.SERIALIZABLE)
	private Fraction doseNoon;

	@DatabaseField
	private String name;

	@DatabaseField
	private long repeatArg;

	@DatabaseField
	private int refillSize;

	@DatabaseField
	private int repeat;

	@DatabaseField
	private int form;

	@DatabaseField
	private boolean active;

	@DatabaseField
	private int sortRank;

	@Override
	public Entry convert()
	{
		Drug drug = new Drug();
		Entry.copy(drug, this);
		drug.setSchedule(null);
		return drug;
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
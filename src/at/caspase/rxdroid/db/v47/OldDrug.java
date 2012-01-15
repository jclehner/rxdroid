package at.caspase.rxdroid.db.v47;

import java.util.Date;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.db.Drug;
import at.caspase.rxdroid.db.Entry;
import at.caspase.rxdroid.db.UpgradeUtils;

@DatabaseTable(tableName="drugs")
@SuppressWarnings({ "unused", "serial" })
public class OldDrug extends Entry
{
	@DatabaseField(unique = true)
	private String name;

	@DatabaseField
	private int form;

	@DatabaseField(defaultValue = "true")
	private boolean active;

	@DatabaseField
	private int refillSize;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction currentSupply;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseMorning;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseNoon;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseEvening;

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseNight;

	// TODO change column name
	@DatabaseField(canBeNull = true)
	private int repeat;

	@DatabaseField(canBeNull = true)
	private long repeatArg;

	@DatabaseField(canBeNull = true)
	private Date repeatOrigin;

	@DatabaseField(canBeNull = true)
	private String comment;

	@Override
	public Drug convert()
	{
		Drug drug = new Drug();
		UpgradeUtils.copyDrug(drug, this);
		drug.setSortRank(0);
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

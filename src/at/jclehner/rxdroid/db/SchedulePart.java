package at.jclehner.rxdroid.db;

import at.jclehner.androidutils.LazyValue;
import at.jclehner.rxdroid.Fraction;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Represents a drug's schedule on specific weekdays.
 *
 * @author Joseph Lehner
 *
 */
@DatabaseTable
public class SchedulePart extends Entry
{
	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseMorning;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNoon;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseEvening;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNight;

	@DatabaseField
	/* package */ int weekdays;

	@DatabaseField(foreign = true)
	private Schedule owner;

	transient private LazyValue<Fraction[]> mDoses = new LazyValue<Fraction[]>() {

		@Override
		public Fraction[] value()
		{
			return new Fraction[] {
					doseMorning, doseNoon, doseEvening, doseNight
			};
		}
	};

	public boolean hasDoses()
	{
		return !doseMorning.isZero() ||
				!doseNoon.isZero() ||
				!doseEvening.isZero() ||
				!doseNight.isZero();
	}

	public Fraction[] getDoses() {
		return mDoses.get();
	}

	@Override
	public boolean equals(Object other)
	{
		if(other == null || !(other instanceof SchedulePart))
			return false;

		if(weekdays != ((SchedulePart) other).weekdays)
			return false;

		if(!doseMorning.equals(((SchedulePart) other).doseMorning))
			return false;

		if(!doseNoon.equals(((SchedulePart) other).doseNoon))
			return false;

		if(!doseEvening.equals(((SchedulePart) other).doseEvening))
			return false;

		if(!doseNight.equals(((SchedulePart) other).doseNight))
			return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return 0;
	}
}

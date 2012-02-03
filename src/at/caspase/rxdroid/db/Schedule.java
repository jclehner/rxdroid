package at.caspase.rxdroid.db;

import java.util.Date;

import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.db.schedules.ScheduleBase;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Handles advanced intake schedules.
 * <p>
 * Note that an advanced schedule is implemented by extending
 * {@link ScheduleBase}. This class merely serves as a wrapper to accomodate
 * all schedules in one DB table. To achieve this, the schedule
 * class extending {@link ScheduleBase} is serialized into this DB entry's
 * <code>schedule</code> field. All function calls are thus relayed
 * to the actual implementation in said field.
 *
 * @see ScheduleBase
 *
 * @author Joseph Lehner
 *
 */
@DatabaseTable(tableName="schedules")
public final class Schedule extends Entry
{
	private static final long serialVersionUID = 7534352445550766725L;

	@DatabaseField(dataType=DataType.SERIALIZABLE)
	private ScheduleBase schedule;

	public Schedule() {}

	public Schedule(ScheduleBase scheduleImpl) {
		schedule = scheduleImpl;
	}

	public Fraction getDose(Date date, int doseTime) {
		return schedule.getDose(date, doseTime);
	}

	public boolean hasDoseOnDate(Date date) {
		return schedule.hasDoseOnDate(date);
	}

	public void setDose(Date date, int doseTime, Fraction dose) {
		schedule.setDose(date, doseTime, dose);
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

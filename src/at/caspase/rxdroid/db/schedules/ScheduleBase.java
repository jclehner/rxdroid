package at.caspase.rxdroid.db.schedules;

import java.io.Serializable;
import java.util.Date;

import at.caspase.rxdroid.Fraction;

/**
 * Abstract base class for all advanced schedules.
 *
 * @author Joseph Lehner
 */
@SuppressWarnings("serial")
public abstract class ScheduleBase implements Serializable
{
	/**
	 * Returns the dose for a given date and dose-time.
	 *
	 * @return the dose. Must not be <code>null</code>.
	 */
	public abstract Fraction getDose(Date date, int doseTime);

	/**
	 * Checks whether there are any doses on a given day.
	 *
	 * @return <code>true</code> if the given drug has at least one non-zero dose on a given date.
	 */
	public abstract boolean hasDoseOnDate(Date date);

	/**
	 * Sets the dose for a given date and dose-time.
	 */
	public abstract void setDose(Date date, int doseTime, Fraction dose);

	@Override
	public boolean equals(Object other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}
}

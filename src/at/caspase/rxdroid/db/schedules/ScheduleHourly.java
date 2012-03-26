package at.caspase.rxdroid.db.schedules;

import java.util.Date;

import at.caspase.rxdroid.DumbTime;
import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.db.Drug;

public class ScheduleHourly extends ScheduleBase
{
	private static final long serialVersionUID = -7584590071001608009L;

	private int mMode = 0;
	private Fraction[] mDoses = new Fraction[4];
	private int mDoseTimeToSkip = Drug.TIME_INVALID;

	private boolean mIn8HourMode = true;

	private DumbTime mOffset;

	public void enable8HourMode(boolean enable)
	{
		if(!(mIn8HourMode = enable))
			mDoseTimeToSkip = Drug.TIME_INVALID;
	}

	public boolean isIn8HourMode() {
		return mIn8HourMode;
	}

	public void setDoseTimeToSkip(int doseTime) {
		mDoseTimeToSkip = doseTime;
	}

	public int getDoseTimeToSkip() {
		return mDoseTimeToSkip;
	}

	public void setOffset(DumbTime offset)
	{
		final long max = 1000 * 3600 * (mIn8HourMode ? 8 : 6);
		if(offset.getTime() >= max)
			throw new IllegalArgumentException();

		mOffset = offset;
	}

	@Override
	public Fraction getDose(Date date, int doseTime)
	{
		if(doseTime != mDoseTimeToSkip)
			return mDoses[doseTime];

		return new Fraction();
	}

	@Override
	public boolean hasDoseOnDate(Date date) {
		return true;
	}

	@Override
	public void setDose(Date date, int doseTime, Fraction dose)
	{
		if(doseTime != mDoseTimeToSkip)
			mDoses[doseTime] = dose;
	}

}

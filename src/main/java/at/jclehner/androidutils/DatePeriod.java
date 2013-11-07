package at.jclehner.androidutils;

import java.util.Date;

import at.jclehner.rxdroid.util.Constants;

public class DatePeriod
{
	/* package */ final Date mBegin, mEnd;

	public DatePeriod(Date begin, Date end)
	{
		mBegin = min(begin, end);
		mEnd = max(begin, end);
	}

	public boolean contains(Date date) {
		return contains(mBegin, mEnd, date);
	}

	public boolean contains(long time) {
		return time >= mBegin.getTime() && time < mEnd.getTime();
	}

	public Date begin() {
		return mBegin;
	}

	public Date end() {
		return mEnd;
	}

	public long days() {
		return daysBetween(mBegin, mEnd);
	}

	public static long daysBetween(Date date1, Date date2)
	{
		final Date min = min(date1, date2);
		final Date max = max(date1, date2);
		return (max.getTime() - min.getTime()) / Constants.MILLIS_PER_DAY;
	}

	public static boolean contains(Date begin, Date end, Date date) {
		return date.before(end) && (date.after(begin) || date.equals(begin));
	}

	@Override
	public boolean equals(Object o)
	{
		if(o == null || !(o instanceof DatePeriod))
			return false;

		final DatePeriod other = (DatePeriod) o;
		return other.mBegin.equals(mBegin) && other.mEnd.equals(mEnd);
	}

	@Override
	public int hashCode() {
		return mBegin.hashCode() ^ mEnd.hashCode();
	}

	private static Date min(Date date1, Date date2) {
		return date1.compareTo(date2) < 0 ? date1 : date2;
	}

	private static Date max(Date date1, Date date2) {
		return date1.compareTo(date2) > 0 ? date1 : date2;
	}
}

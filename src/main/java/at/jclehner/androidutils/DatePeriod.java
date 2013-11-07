package at.jclehner.androidutils;

import java.util.Date;

import at.jclehner.rxdroid.util.Constants;

public class DatePeriod
{
	/* package */ final Date mBegin, mEnd;

	public DatePeriod(Date begin, Date end)
	{
		if(begin == null)
			throw new NullPointerException();

		mBegin = begin;
		mEnd = end;
	}

	public boolean contains(Date date) {
		return contains(mBegin, mEnd, date);
	}

	public boolean contains(long time)
	{
		return time >= mBegin.getTime() && (mEnd == null || time < mEnd.getTime());
	}

	public Date begin() {
		return mBegin;
	}

	public Date end() {
		return mEnd;
	}

	public static boolean contains(Date begin, Date end, Date date) {
		return (end == null || date.before(end)) && (date.after(begin) || date.equals(begin));
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
		return mBegin.hashCode() ^ (mEnd != null ? mEnd.hashCode() : 0);
	}

	private static Date min(Date date1, Date date2) {
		return date1.compareTo(date2) < 0 ? date1 : date2;
	}

	private static Date max(Date date1, Date date2) {
		return date1.compareTo(date2) > 0 ? date1 : date2;
	}
}

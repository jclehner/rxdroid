package at.jclehner.androidutils;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NonOverlappingTimePeriodMap<T> extends HashMap<DatePeriod, T>
{
	public boolean containsDate(Date date)
	{
		for(DatePeriod period : keySet())
		{
			if(period.contains(date))
				return true;
		}

		return false;
	}

	/* package */ boolean containsTime(long time)
	{
		for(DatePeriod period : keySet())
		{
			if(period.contains(time))
				return true;
		}

		return false;
	}

	public boolean containsPeriod(DatePeriod period)
	{
		return containsTime(period.mBegin.getTime()) ||
				(period.mEnd != null && containsTime(period.mEnd.getTime() - 1));
	}

	public T get(Date date)
	{
		for(DatePeriod key : keySet())
		{
			if(key.contains(date))
				return get(key);
		}

		return null;
	}

	public List<T> getAllStartingBefore(Date date, boolean dateInclusive) {
		return getAllMatching(date, true, true, dateInclusive);
	}

	public List<T> getAllStartingAfter(Date date, boolean dateInclusive) {
		return getAllMatching(date, true, false, dateInclusive);
	}

	public List<T> getAllEndingBefore(Date date, boolean dateInclusive) {
		return getAllMatching(date, false, true, dateInclusive);
	}

	public List<T> getAllEndingAfter(Date date, boolean dateInclusive) {
		return getAllMatching(date, false, false, dateInclusive);
	}

	@Override
	public T put(DatePeriod key, T value)
	{
		if(containsPeriod(key))
			throw new IllegalArgumentException("Attempting to add overlapping period " + key);

		return super.put(key, value);
	}

	@Override
	public final void putAll(Map<? extends DatePeriod, ? extends T> map)
	{
		for(DatePeriod key : map.keySet())
			put(key, map.get(key));
	}

	private List<T> getAllMatching(Date date, boolean matchBegin, boolean matchBefore, boolean dateInclusive)
	{
		ArrayList<T> list = new ArrayList<T>();

		for(DatePeriod key : keySet())
		{
			final Date matchDate = matchBegin ? key.mBegin : key.mEnd;

			if((matchBefore && matchDate.before(date))
				|| (!matchBefore && matchDate.after(date))
				|| (dateInclusive && matchDate.equals(date)))
			{
				list.add(get(key));
			}
		}

		return list;
	}
}

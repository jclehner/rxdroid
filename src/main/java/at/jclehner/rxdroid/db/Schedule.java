/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package at.jclehner.rxdroid.db;

import java.util.Date;

import at.jclehner.androidutils.LazyValue;
import at.jclehner.rxdroid.Fraction;
import at.jclehner.rxdroid.util.DateTime;
import at.jclehner.rxdroid.util.Exceptions;
import at.jclehner.rxdroid.util.Hasher;
import at.jclehner.rxdroid.util.Keep;
import at.jclehner.rxdroid.util.Util;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName="schedules")
public final class Schedule extends Entry
{
	public static final int TIME_MORNING = 0;
	public static final int TIME_NOON    = 1;
	public static final int TIME_EVENING = 2;
	public static final int TIME_NIGHT   = 3;
	public static final int TIME_INVALID = 4;

	public static final int[] DOSE_TIMES = {
			TIME_MORNING, TIME_NOON, TIME_EVENING, TIME_NIGHT
	};

	public static final int DOSE_TIME_COUNT = TIME_INVALID;

	public static final int REPEAT_DAILY = 0;
	public static final int REPEAT_EVERY_N_DAYS = 1;
	public static final int REPEAT_WEEKDAYS = 2;
	public static final int REPEAT_DAILY_WITH_PAUSE = 3;
	public static final int REPEAT_EVERY_6_8_12_OR_24_HOURS = 4;

	private static final int MASK_REPEAT_ARG_PAUSE = 0xffff;
	private static final int MASK_REPEAT_ARG_ON_LENGTH = 0xffff0000;

	private static final Fraction[] ZERO_DOSE_ARRAY = new Fraction[] {
		Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO
	};

	@DatabaseField
	private String name;

	@DatabaseField(canBeNull = false)
	/* package */ Date begin;

	@DatabaseField
	/* package */ Date end;

	@DatabaseField
	private int repeatMode = REPEAT_DAILY;

	@DatabaseField
	private long repeatArg;

	@DatabaseField
	private boolean asNeeded = false;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseMorning;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNoon;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseEvening;

	@DatabaseField(persisterClass = FractionPersister.class)
	private Fraction doseNight;

	@SuppressWarnings("unused")
	@DatabaseField(foreign = true)
	/* package */ transient Drug owner;

	@ForeignCollectionField(eager = true)
	private ForeignCollection<SchedulePart> scheduleParts;

	public String getName() {
		return name;
	}

	public void setBegin(Date begin) {
		this.begin = begin;
	}

	public Date getBegin() {
		return begin;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public Date getEnd() {
		return end;
	}

	public void setOwner(Drug owner) {
		this.owner = owner;
	}

	public void setAsNeeded(boolean asNeeded) {
		this.asNeeded = asNeeded;
	}

	public boolean getAsNeeded() {
		return asNeeded;
	}

	public boolean hasDoseOnDate(Date date)
	{
		if(begin != null && date.before(begin))
			return false;
		else if(end != null && date.after(end))
			return false;

		if(!isDosePossibleOnDate(date))
			return false;

		for(Fraction dose : getDoses(date))
		{
			if(!dose.isZero())
				return true;
		}

		return false;
	}

	public void setDose(int doseTime, Fraction dose)
	{
		mDoses.get()[doseTime] = dose;

		switch(doseTime)
		{
			case TIME_MORNING:
				doseMorning = dose;
				break;

			case TIME_NOON:
				doseNoon = dose;
				break;

			case TIME_EVENING:
				doseEvening = dose;
				break;

			case TIME_NIGHT:
				doseNight = dose;
				break;

			default:
				throw new Exceptions.UnexpectedValueInSwitch(doseTime);
		}
	}

	public Fraction[] getDoses(Date date)
	{
		if(!isDosePossibleOnDate(date))
			return ZERO_DOSE_ARRAY;

		final SchedulePart[] schedulePartsArray = mSchedulePartsArray.get();
		if(schedulePartsArray != null && schedulePartsArray.length != 0)
		{
			final int weekday = DateTime.getIsoWeekDayNumberIndex(date);
			for(SchedulePart part : schedulePartsArray)
			{
				if((part.weekdays & (1 << weekday)) != 0)
					return part.getDoses();
			}
		}

		return mDoses.get();
	}

	public Fraction getDose(Date date, int doseTime)
	{
		final Fraction dose = getDoses(date)[doseTime];
		return dose != null ? dose : Fraction.ZERO;
	}

	public Fraction getDose(int doseTime) {
		return mDoses.get()[doseTime];
	}

	public boolean hasNoDoses()
	{
		final SchedulePart[] schedulePartsArray = mSchedulePartsArray.get();
		if(schedulePartsArray != null)
		{
			for(SchedulePart part : schedulePartsArray)
			{
				if(part.hasDoses())
					return false;
			}
		}

		for(Fraction dose : mDoses.get())
		{
			if(!dose.isZero())
				return false;
		}

		return true;
	}

	public void setRepeatMode(int repeatMode) {
		this.repeatMode = repeatMode;
	}

	public int getRepeatMode() {
		return repeatMode;
	}

	public void setRepeatArg(long repeatArg) {
		this.repeatArg = repeatArg;
	}

	public long getRepeatArg() {
		return repeatArg;
	}

	public void setRepeatDailyWithPause(int onDays, int pauseDays) {
		setRepeatArg(makeRepeatDailyWithPauseArg(onDays, pauseDays));
	}

	public boolean isComplex() {
		return scheduleParts != null && scheduleParts.size() != 0;
	}

	public static long makeRepeatDailyWithPauseArg(int onDays, int pauseDays)
	{
		if(onDays < 0 || onDays > 0xffff || pauseDays < 0 || pauseDays > 0xffff)
			throw new IllegalArgumentException("Argument out of range 0-0xffff");

		return onDays << 16 | pauseDays;
	}

	public int getRepeatOnDays()
	{
		if(repeatMode != REPEAT_DAILY_WITH_PAUSE)
			throw new UnsupportedOperationException();

		return (int) (repeatArg >> 16) & 0xffff;
	}

	public int getRepeatPauseDays()
	{
		if(repeatMode != REPEAT_DAILY_WITH_PAUSE)
			throw new UnsupportedOperationException();

		return (int) repeatArg & 0xffff;
	}

	public void setScheduleParts(SchedulePart[] parts)
	{
		// There's no need to fiddle with this.scheduleParts, since we set
		// this.mSchedulePartsArray, which is what other functions use.

		for(SchedulePart part : parts)
			part.owner = this;

		mSchedulePartsArray.set(parts);
	}

	@Override
	public boolean equals(Object o)
	{
		if(o == null || !(o instanceof Schedule))
			return false;

		Object[] thisFields = getRelevantFields();
		Object[] otherFields = ((Schedule) o).getRelevantFields();

		for(int i = 0; i != thisFields.length; ++i)
		{
			if(!Util.equalsIgnoresNull(thisFields[i], otherFields[i]))
				return false;
		}

		return true;
	}

	@Override
	public int hashCode()
	{
		Hasher hasher = new Hasher();
		for(Object field : getRelevantFields())
			hasher.hash(field);

		return hasher.getHashCode();
	}

	private Object[] getRelevantFields()
	{
		return new Object[] {
				name,
				begin,
				end,
				asNeeded,
				repeatMode,
				repeatArg,
				doseMorning,
				doseNoon,
				doseEvening,
				doseNight,
				scheduleParts
		};
	}

	private boolean isDosePossibleOnDate(Date date)
	{
		switch(repeatMode)
		{
			case REPEAT_DAILY:
			case REPEAT_EVERY_6_8_12_OR_24_HOURS:
				return true;

			case REPEAT_EVERY_N_DAYS:
				return DateTime.diffDays(date, begin) % repeatArg == 0;

			case REPEAT_WEEKDAYS:
				return (repeatArg & (1 << DateTime.getIsoWeekDayNumberIndex(date))) != 0;

			case REPEAT_DAILY_WITH_PAUSE:
				final long pauseDays = repeatArg & MASK_REPEAT_ARG_PAUSE;
				final long cycleLength = (repeatArg & MASK_REPEAT_ARG_ON_LENGTH) >> 16;
				return DateTime.diffDays(date, begin) % cycleLength < (cycleLength - pauseDays);

			default:
				throw new Exceptions.UnexpectedValueInSwitch(repeatMode);
		}
	}

	transient private LazyValue<SchedulePart[]> mSchedulePartsArray = new LazyValue<SchedulePart[]>() {

		@Override
		public SchedulePart[] value()
		{
			if(scheduleParts == null)
				return null;

			SchedulePart[] value = new SchedulePart[scheduleParts.size()];
			return scheduleParts.toArray(value);
		}
	};

	private transient final LazyValue<Fraction[]> mDoses = new LazyValue<Fraction[]>() {

		@Override
		public Fraction[] value()
		{
			return new Fraction[] {
					doseMorning, doseNoon, doseEvening, doseNight
			};
		}
	};

	@Keep
	/* package */ static final Callback<Schedule> CALLBACK_DELETED = new Callback<Schedule>() {

		@Override
		public void call(Schedule schedule)
		{
			final SchedulePart[] scheduleParts = schedule.mSchedulePartsArray.get();
			if(scheduleParts == null)
				return;

			for(SchedulePart part : scheduleParts)
			{
				Database.delete(part, Database.FLAG_DONT_NOTIFY_LISTENERS);
			}
		}
	};
}

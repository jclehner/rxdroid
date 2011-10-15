package at.caspase.rxdroid.db;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import android.util.Log;
import at.caspase.rxdroid.Fraction;
import at.caspase.rxdroid.R;
import at.caspase.rxdroid.util.CollectionUtils;
import at.caspase.rxdroid.util.Constants;
import at.caspase.rxdroid.util.DateTime;
import at.caspase.rxdroid.util.Hasher;
import at.caspase.rxdroid.util.SimpleBitSet;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Class for handling the drug database.
 *
 * The word "dose" in the context of this documentation refers to
 * the smallest available dose of that drug without having to
 * manually reduce its amount (i.e. no pill-splitting). For example,
 * a package of Aspirin containing 30 tablets contains 30 doses; of
 * course, the intake schedule may also contain doses in fractions.
 *
 * Another term you'll come across in the docs and the code is the
 * concept of a 'dose-time'. A dose-time is a user-definable subdivision
 * of the day, having one of the following predefined names: morning,
 * noon, evening, night.
 *
 * Any drug in the database will have the following attributes:
 * <ul>
 *  <li>A unique name</li>
 *  <li>The form of the medication. This will be reflected in the UI by
 *      displaying a corresponding icon next to the drug's name.</li>
 *  <li>The size of one refill. This corresponds to the amount of doses
 *      per prescription, package, etc. Note that due to the definition of
 *      the word "dose" mentioned above, this size must not be a fraction.</li>
 *  <li>The current supply. This contains the number of doses left for this particular drug.</li>
 *  <li>An optional comment for that drug (e.g. "Take with food").</li>
 *  <li>A field indicating whether the drug should be considered active. A drug marked
 *      as inactive will be ignored by the DrugNotificationService.</li>
 * </ul>
 *
 * @author Joseph Lehner
 *
 */
@DatabaseTable(tableName = "drugs")
public class Drug extends Entry
{
	private static final String TAG = Drug.class.getName();
	private static final long serialVersionUID = -2569745648137404894L;

	public static final int FORM_TABLET = 0;
	public static final int FORM_INJECTION = 1;
	public static final int FORM_SPRAY = 2;
	public static final int FORM_DROP = 3;
	public static final int FORM_GEL = 4;
	public static final int FORM_OTHER = 5;

	public static final int TIME_MORNING = 0;
	public static final int TIME_NOON = 1;
	public static final int TIME_EVENING = 2;
	public static final int TIME_NIGHT = 3;

	public static final int FREQ_DAILY = 0;
	public static final int FREQ_EVERY_N_DAYS = 1;
	public static final int FREQ_WEEKDAYS = 2;
	
	public static final int FREQARG_DAY_MON = 1;
	public static final int FREQARG_DAY_TUE = 1 << 1;
	public static final int FREQARG_DAY_WED = 1 << 2;
	public static final int FREQARG_DAY_THU = 1 << 3;
	public static final int FREQARG_DAY_FRI = 1 << 4;
	public static final int FREQARG_DAY_SAT = 1 << 5;
	public static final int FREQARG_DAY_SUN = 1 << 6;		

	@DatabaseField(unique = true)
	private String name;

	@DatabaseField(useGetSet = true)
	private int form;

	@DatabaseField(defaultValue = "true")
	private boolean active = true;

	// if mRefillSize == 0, mCurrentSupply should be ignored
	@DatabaseField(useGetSet = true)
	private int refillSize;

	@DatabaseField(dataType = DataType.SERIALIZABLE, useGetSet = true)
	private Fraction currentSupply = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseMorning = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseNoon = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseEvening = new Fraction();

	@DatabaseField(dataType = DataType.SERIALIZABLE)
	private Fraction doseNight = new Fraction();

	@DatabaseField(canBeNull = true, useGetSet = true)
	private int frequency = FREQ_DAILY;

	/**
	 * Defines the frequency origin.
	 *
	 * For every frequency other than {@link #FREQ_DAILY}, this field holds a specific value,
	 * allowing {@link #hasDoseOnDate(Date)} to determine whether a dose is pending
	 * on a specific date.
	 *
	 * <ul>
	 *     <li><code>FREQ_EVERY_OTHER_DAY</code>: field is set to a date (in milliseconds) where this drug's
	 *         intake should be set, i.e. if the date corresponds to 2011-09-07, there's an intake on that day,
	 *         another one on 2011-09-09, and so forth.</li>
	 *     <li><code>FREQ_WEEKLY</code>: field is set to a week day value from {@link java.util.Calendar}.</li>
	 * </ul>
	 */
	@DatabaseField(canBeNull = true)
	private long frequencyArg = 0;
	
	@DatabaseField
	private java.util.Date frequencyOrigin;
	
	@DatabaseField(canBeNull = true)
	private String comment;

	public Drug() {}
	
	public Drug(String name, int form, boolean active, int refillSize, Fraction currentSupply, Fraction[] schedule, 
			int frequency, long frequencyArg, Date frequencyOrigin)
	{
		this.name = new String(name);
		this.form = form;
		this.active = active;
		this.refillSize = refillSize;
		this.currentSupply = new Fraction(currentSupply);
		this.doseMorning = new Fraction(schedule[0]);
		this.doseNoon = new Fraction(schedule[1]);
		this.doseEvening = new Fraction(schedule[2]);
		this.doseNight = new Fraction(schedule[3]);
		this.frequency = frequency;
		this.frequencyArg = frequencyArg;
		this.frequencyOrigin = new Date(frequencyOrigin != null ? frequencyOrigin.getTime() : 0);
	}

	public boolean hasDoseOnDate(Calendar cal)
	{
		if(cal == null)
			throw new NullPointerException("cal == null");
		
		if(frequency == FREQ_DAILY)
			return true;

		if(frequency == FREQ_EVERY_N_DAYS)
		{
			final long diffDays = Math.abs(frequencyOrigin.getTime() - cal.getTimeInMillis()) / Constants.MILLIS_PER_DAY;
			return diffDays % frequencyArg == 0;
		}
		else if(frequency == FREQ_WEEKDAYS)
			return hasDoseOnWeekday(cal.get(Calendar.DAY_OF_WEEK));
		
		throw new AssertionError("WTF");
	}
	

	public String getName() {
		return name;
	}

	public int getForm() {
		return form;
	}

	public int getFormResourceId()
	{
		switch(form)
		{
			case FORM_INJECTION:
				return R.drawable.med_syringe;

			case FORM_DROP:
				return R.drawable.med_drink;

			case FORM_TABLET:
				// fall through

			default:
				return R.drawable.med_pill;

			// FIXME
		}
	}

	public int getFrequency() {
		return frequency;
	}

	public long getFrequencyArg() {
		return frequencyArg;
	}
	
	public Date getFrequencyOrigin() {
		return frequencyOrigin;
	}

	public boolean isActive() {
		return active;
	}

	public int getRefillSize() {
		return refillSize;
	}

	public Fraction getCurrentSupply() {
		return currentSupply;
	}
	
	public double getSupplyCorrectionFactor()
	{
		switch(frequency)
		{
			case FREQ_DAILY:
				return 1.0;
				
			case FREQ_EVERY_N_DAYS:
				return frequencyArg / 1.0;
				
			case FREQ_WEEKDAYS:
				SimpleBitSet sbs = new SimpleBitSet(frequencyArg);
				return 7.0 / sbs.cardinality();
				
			default:
				throw new AssertionError("WTF");
		}			
	}

	public Fraction[] getSchedule() {
		return new Fraction[] { doseMorning, doseNoon, doseEvening, doseNight };
	}

	public Fraction getDose(int doseTime)
	{
		final Fraction doses[] = {
				doseMorning,
				doseNoon,
				doseEvening,
				doseNight
		};

		return doses[doseTime];
	}

	public String getComment() {
		return comment;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setForm(int form)
	{
		if(form > FORM_OTHER)
			throw new IllegalArgumentException();
		this.form = form;
	}

	public void setFrequency(int frequency)
	{
		if(frequency > FREQ_WEEKDAYS)
			throw new IllegalArgumentException();
		else	
			this.frequency = frequency;
		
		if(frequency == FREQ_DAILY)
		{
			frequencyArg = 0;
			frequencyOrigin = null;
		}
	}

	public void setFrequencyArg(long frequencyArg) {
		this.frequencyArg = frequencyArg;
	}
	
	public void setFrequencyOrigin(Date frequencyOrigin) {
		this.frequencyOrigin = frequencyOrigin;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void setRefillSize(int refillSize)
	{
		if(refillSize < 0)
			throw new IllegalArgumentException();
		this.refillSize = refillSize;
	}

	public void setCurrentSupply(Fraction currentSupply)
	{
		if(currentSupply == null)
			this.currentSupply = Fraction.ZERO;
		else if(currentSupply.compareTo(0) == -1)
			throw new IllegalArgumentException();

		this.currentSupply = currentSupply;
	}

	public void setDose(int doseTime, Fraction value)
	{
		switch(doseTime)
		{
			case TIME_MORNING:
				doseMorning = value;
				break;
			case TIME_NOON:
				doseNoon = value;
				break;
			case TIME_EVENING:
				doseEvening = value;
				break;
			case TIME_NIGHT:
				doseNight = value;
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Drug))
			return false;

		final Drug other = (Drug) o;

		if(other == this)
			return true;

		final Object[] thisMembers = this.getFieldValues();
		final Object[] otherMembers = other.getFieldValues();

		for(int i = 0; i != thisMembers.length; ++i)
		{
			if(thisMembers[i] == null && otherMembers[i] == null)
				continue;
			else if(thisMembers[i] == null || otherMembers[i] == null)
				return false;
			else if(!thisMembers[i].equals(otherMembers[i]))
				return false;
		}

		return true;
	}

	@Override
	public int hashCode()
	{
		final Hasher hasher = new Hasher();
		final Object[] thisMembers = this.getFieldValues();

		for(Object o : thisMembers)
			hasher.hash(o);

		return hasher.getHashCode();
	}

	@Override
	public String toString() {
		return name + "(" + id + ")={ " + doseMorning + " - " + doseNoon + " - " + doseEvening + " - " + doseNight + "}";
	}

	/**
	 * Get all relevant members for comparison/hashing.
	 *
	 * When comparing for equality or hashing, we ignore a drug's unique ID, as it may be left
	 * uninitialized and automatically determined by the SQLite logic.
	 *
	 * @return An array containing all fields but the ID.
	 */
	private Object[] getFieldValues()
	{
		final Object[] members = {
			this.name,
			this.form,
			this.active,
			this.doseMorning,
			this.doseNoon,
			this.doseEvening,
			this.doseNight,
			this.currentSupply,
			this.refillSize,
			this.frequency,
			this.frequencyArg,
			this.frequencyOrigin,
			this.comment
		};

		return members;
	}
	
	private boolean hasDoseOnWeekday(int calWeekday)
	{
		if(frequency != FREQ_WEEKDAYS)
			throw new IllegalStateException("frequency != FREQ_WEEKDAYS");
		
		// first, translate Calendar's weekday representation to our
		// own.
		
		int weekday = CollectionUtils.indexOf(calWeekday, Constants.WEEK_DAYS);
		if(weekday == -1)
			throw new IllegalArgumentException("Argument " + calWeekday + " does not map to a valid weekday");
		
		return (frequencyArg & (1 << weekday)) != 0;		
	}
}
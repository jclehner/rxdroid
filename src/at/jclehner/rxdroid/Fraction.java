/**
 * Copyright (C) 2011 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 * This file is part of RxDroid.
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

package at.jclehner.rxdroid;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import at.jclehner.rxdroid.util.Hasher;


/**
 * Very simple class for handling fractions.
 *
 * @author Joseph Lehner
 */
public class Fraction extends Number implements Comparable<Number>, Parcelable
{
	public static class MutableFraction extends Fraction
	{
		private static final long serialVersionUID = -3830132386515948322L;

		public MutableFraction(Fraction other) {
			super(other);
		}

		public MutableFraction() {
			super();
		}

		/**
		 * Adds another fraction to this fraction.
		 * @return a reference to this instance
		 */
		public MutableFraction add(Fraction other) {
			return add(this, other);
		}

		/**
		 * Adds an integer to this fraction.
		 * @return a reference to this object
		 */
		public MutableFraction add(int n) {
			return add(this, n);
		}

		/**
		 * Subtracts another fraction from this fraction.
		 * @return a reference to this instance
		 */
		public MutableFraction subtract(Fraction other) {
			return add(this, other.negate());
		}

		/**
		 * Subtracts an integer from this fraction.
		 * @return a reference to this instance
		 */
		public MutableFraction subtract(int n) {
			return add(this, -n);
		}
	}

	private static final long serialVersionUID = 2050536341303052796L;

	@SuppressWarnings("unused")
	private static  final String TAG = Fraction.class.getName();

	private static boolean sDisplayMixedNumbers = true;

	private int mNumerator = 0;
	private int mDenominator = 1;

	/**
	 * A zero value, just for convenience.
	 */
	public static final Fraction ZERO = new Fraction();

	/**
	 * Default constructor.
	 * <p>
	 * Constructs a fraction with a value of zero.
	 */
	public Fraction() {}

	/**
	 * Copy constructor.
	 */
	public Fraction(Fraction other) {
		this(other.mNumerator, other.mDenominator);
	}

	/**
	 * Construct a fraction from an integer.
	 */
	public Fraction(int integer) {
		this(integer, 1);
	}

	/**
	 * Construct a fraction from a numerator and denominator.
	 *
	 * When initializing a negative fraction, always specify the numerator
	 * as a negative value.
	 *
	 * @throws IllegalArgumentException if {@code denominator <= 0}
	 */
	public Fraction(int numerator, int denominator) {
		init(0, numerator, denominator);
	}

	/**
	 * Construct a fraction from a mixed number format.
	 *
	 * When initializing negative fractions, only specify the <code>integer</code> parameter as negative.
	 *
	 * @throws IllegalArgumentException if {@code denominator <= 0} or {@code wholeNum != 0 && numerator < 0}
	 */
	public Fraction(int integer, int numerator, int denominator) {
		init(integer, numerator, denominator);
	}

	/**
	 * Returns <code>true</code> if <code>numerator / denominator</code> yields an integer.
	 */
	public boolean isInteger() {
		return mNumerator % mDenominator == 0;
	}

	public boolean isNegative() {
		return mNumerator < 0;
	}

	public boolean isZero() {
		return mNumerator == 0;
	}

	public MutableFraction mutate() {
		return new MutableFraction(this);
	}

	public Fraction plus(final Fraction other)
	{
		Fraction result = new Fraction(this);
		return add(result, other);
	}

	public Fraction plus(int n)
	{
		Fraction result = new Fraction(this);
		return add(result, n);
	}

	public Fraction minus(final Fraction other) {
		return plus(other.negate());
	}

	public Fraction minus(int n) {
		return plus(-n);
	}

	/**
	 * Returns the fraction's negative form.
	 */
	public Fraction negate() {
		return new Fraction(-mNumerator, mDenominator);
	}

	/**
	 * Returns the fraction's reciprocal value.
	 */
	public Fraction reciprocal()
	{
		if(mNumerator < 0)
		{
			// The sign is always kept in mNumerator, so mDenominator
			// can't be negative. We thus pass -mDenominator which
			// makes it negative, and -mNumerator, which makes it positive.
			return new Fraction(-mDenominator, -mNumerator);
		}

		return new Fraction(mDenominator, mNumerator);
	}

	/**
	 * Gets the raw fraction data.
	 *
	 * @param returnAsMixedNumber
	 * @return an <code>int[]</code> with the values <code>{ wholeNum, numerator, denominator }</code>, where <code>wholeNum</code> will
	 *     be zero if <code>returnAsMixedNumber</code> is <code>true</code>
	 */
	public int[] getFractionData(boolean returnAsMixedNumber)
	{
		if(!returnAsMixedNumber)
			return new int[] { 0, mNumerator, mDenominator };

		int wholeNum = mNumerator / mDenominator;
		int numerator = Math.abs(mNumerator % mDenominator);

		return new int[] { wholeNum, numerator, mDenominator };
	}

	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Fraction))
			return false;

		return compareTo((Fraction) o) == 0;
	}

	@Override
	public int hashCode()
	{
		final Hasher hasher = new Hasher();

		hasher.hash(mNumerator);
		hasher.hash(mDenominator);

		return hasher.getHashCode();
	}

	@Override
	public int compareTo(Number other)
	{
		if(this == other)
			return 0;

		if(other instanceof Fraction)
		{
			Fraction otherFraction = (Fraction) other;

			int a = this.mNumerator * otherFraction.mDenominator;
			int b = otherFraction.mNumerator * this.mDenominator;

			if(a == b)
				return 0;

			return a < b ? -1 : 1;
		}
		else
			return Double.compare(this.doubleValue(), other.doubleValue());
	}

	/**
	 * Returns the fraction's textual representation.
	 * <p>
	 * The generated string's format depends on whether you've disabled the displaying of
	 * 'mixed numbers' (it's enabled by default) by calling setDisplayMixedNumbers().
	 * Note that regardless of that setting's state, the returned string is guaranteed
	 * to be accepted by Fraction.decode().
	 *
	 * @see Fraction#decode
	 * @see Fraction#setDisplayMixedNumbers
	 */
	@Override
	public String toString()
	{
		if(mDenominator == 1)
			return Integer.toString(mNumerator);

		int wholeNum = mNumerator / mDenominator;
		int numerator = mNumerator % mDenominator;

		if(numerator != 0)
		{
			if(sDisplayMixedNumbers)
				return (wholeNum == 0 ? "" : wholeNum + " ") + (wholeNum < 0 ? Math.abs(numerator) : numerator) + "/" + mDenominator;
			else
				return mNumerator + "/" + mDenominator;
		}

		return Integer.toString(wholeNum);
	}

	@Override
	public double doubleValue() {
		return (double) mNumerator / mDenominator;
	}

	@Override
	public float floatValue() {
		return (float) doubleValue();
	}

	@Override
	public int intValue() {
		return (int) longValue();
	}

	@Override
	public long longValue() {
		return Math.round(doubleValue());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeInt(mNumerator);
		dest.writeInt(mDenominator);
	}



	/**
	 * Parses the textual representation of a fraction.
	 * <p>
	 * This function will accept strings like {@literal -3 1/4} or {@literal 5/4}.
	 * Superfluous whitespace will be trimmed.
	 *
	 * @throws NumberFormatException
	 */
	public static Fraction valueOf(final String string)
	{
		final String[] tokens = string.trim().split("\\s*/\\s*|\\s+");
		int wholeNum = 0, numerator = 0, denominator = 1;

		switch(tokens.length)
		{
			case 2:
				numerator = Integer.parseInt(tokens[0]);
				denominator = Integer.parseInt(tokens[1]);
				break;

			case 3:
				numerator = Integer.parseInt(tokens[1]);
				denominator = Integer.parseInt(tokens[2]);
				// fall through

			case 1:
				wholeNum = Integer.parseInt(tokens[0]);
				break;

			default:
				throw new NumberFormatException(string + " -> " + Arrays.toString(tokens));
		}

		return new Fraction(wholeNum, numerator, denominator);
	}

	public static void setDisplayMixedNumbers(boolean displayMixedNumbers) {
		sDisplayMixedNumbers = displayMixedNumbers;
	}

	public static final Parcelable.Creator<Fraction> CREATOR = new Parcelable.Creator<Fraction>() {

		@Override
		public Fraction createFromParcel(Parcel in)
        {
            return new Fraction(in.readInt(), in.readInt());
        }

        @Override
		public Fraction[] newArray(int size)
        {
            return new Fraction[size];
        }
    };

	private void init(final int integer, final int numerator, final int denominator)
	{
		if(denominator <= 0)
			throw new NumberFormatException("Denominator must be greater than zero");

		if(integer != 0 && numerator < 0)
			throw new NumberFormatException("Nominator must not be negative if integer is non-zero");

		// set mNumerator, even though we divide it by the GCD later, so as to pass the
		// original argument of this function to findGCD
		if(integer >= 0)
			mNumerator = integer * denominator + numerator;
		else
			mNumerator = integer * denominator - numerator;

		// the sign, if present, has been moved to the numerator by now
		mDenominator = Math.abs(denominator);

		final int divisor = findGCD(Math.abs(numerator), denominator);

		mNumerator = mNumerator / divisor;
		mDenominator = mDenominator / divisor;
	}

	private static <F extends Fraction> F add(F dest, Fraction other)
	{
		int numerator, denominator;

		//Log.d(TAG, "plus: this=" + this + ", other=" + other);

		if(dest.mDenominator != other.mDenominator)
		{
			int lcm = findLCM(dest.mDenominator, other.mDenominator);

			int multThis = lcm / dest.mDenominator;
			int multOther = lcm / other.mDenominator;

			denominator = lcm;
			numerator = dest.mNumerator * multThis + other.mNumerator * multOther;
		}
		else
		{
			numerator = dest.mNumerator + other.mNumerator;
			denominator = dest.mDenominator;
		}

		// this reduces the resulting fraction
		dest.init(0, numerator, denominator);

		return dest;
	}

	private static <F extends Fraction> F add(F dest, int n)
	{
		//init(0, mNumerator + n * mDenominator, mDenominator);
		dest.mNumerator += n * dest.mDenominator;
		return dest;
	}

	/**
	 * Finds the lowest common multiple of two integers.
	 */
	private static int findLCM(int n1, int n2)
	{
		int product = n1 * n2;

		do
		{
			if(n1 < n2)
			{
				int tmp = n1;
				n1 = n2;
				n2 = tmp;
			}
			n1 = n1 % n2;
		} while(n1 != 0);

		return product / n2;
	}

	/**
	 * Finds the greatest common divisor of two integers.
	 */
	private static int findGCD(int n1, int n2)
	{
		if(n2 == 0)
			return n1;
		return findGCD(n2, n1 % n2);
	}

//	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
//		throw new UnsupportedOperationException();
//	}

//	private void writeObject(ObjectOutputStream stream) throws IOException, ClassNotFoundException {
//		throw new UnsupportedOperationException();
//	}
}

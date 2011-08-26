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

package at.caspase.rxdroid;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.caspase.rxdroid.util.Hasher;


/**
 * Very simple class for handling fractions.
 * 
 * @author Joseph Lehner
 */
public class Fraction extends Number implements Serializable, Comparable<Number> 
{
	private static final long serialVersionUID = 2050536341303052796L;
	private static final String TAG = Fraction.class.getName();
	private static final Pattern REGEX = Pattern.compile("^\\s*(?:(-?\\d+)\\s+)?\\s*(?:(-?\\d+)\\s*/\\s*(\\d+)\\s*)\\s*$");
		
	private static boolean sDisplayMixedNumbers = true;
		
	private int mNumerator = 0;
	private int mDenominator = 1;
	
	/**
	 * A zero value, just for convenience.
	 */
	public static final Fraction ZERO = new Fraction(0);
	
	/**
	 * Construct a fraction of the value zero.
	 */
	public Fraction() {}
			
	/**
	 * Construct a fraction from a whole number.
	 */
	public Fraction(int wholeNum) {
		mNumerator = wholeNum;
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
		construct(0, numerator, denominator);
	}

	/**
	 * Construct a fraction from a mixed number format.
	 * 
	 * When initializing negative fractions, only specify the wholeNum parameter as negative.
	 * 
	 * @throws IllegalArgumentException if {@code denominator <= 0} or {@code wholeNum != 0 && numerator < 0}
	 */
	public Fraction(int wholeNum, int numerator, int denominator) {
		construct(wholeNum, numerator, denominator);
	}
	
	public Fraction plus(final Fraction other)
	{
		int numerator, denominator;
		
		if(this.mDenominator != other.mDenominator)
		{
			int lcm = findLCM(this.mDenominator, other.mDenominator);
			
			int multThis = lcm / this.mDenominator;
			int multOther = lcm / other.mDenominator;
			
			denominator = lcm;
			numerator = (this.mNumerator * multThis) + (other.mNumerator * multOther);
		}
		else
		{
			numerator = this.mNumerator + other.mNumerator;
			denominator = this.mDenominator;
		}
					
		return new Fraction(numerator, denominator);
	}
	
	public Fraction plus(Integer integer) {
		return plus(new Fraction(integer));		
	}
	
	public Fraction minus(Fraction other) {
		return plus(other.negate());
	}
	
	public Fraction minus(Integer integer) {
		return minus(new Fraction(integer));		
	}
	
	/**
	 * Return the fraction's negative.
	 */
	public Fraction negate() {
		return new Fraction(-mNumerator, mDenominator);
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Fraction))
			return false;
		
		Fraction other = (Fraction) o;
		
		if(other == this)
			return true;
		
		return compareTo(other) == 0;
	}
	
	@Override
	public int hashCode()
	{
		int result = Hasher.SEED;
		
		result = Hasher.hash(result, mNumerator);
		result = Hasher.hash(result, mDenominator);
		
		return result;
	}
		
	@Override
	public int compareTo(Number other)
	{
		// TODO ugly, change to equalizing to the same denominator and
		// then comparing the nominators!
		if(this.doubleValue() == other.doubleValue())
			return 0;
		
		return this.doubleValue() < other.doubleValue() ? -1 : 1;
	}
	
	/**
	 * Returns the fraction's textual representation.
	 * 
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
	
	/**
	 * Parses the textual representation of a fraction.
	 * 
	 * This function will accept strings like {@literal -3 1/4} or {@literal 5/4}. 
	 * Superfluous whitespace will be trimmed.	 * 
	 * 
	 * @throws NumberFormatException
	 */
	public static Fraction decode(String string) 
	{
		int wholeNum = 0, numerator = 0, denominator = 1;
		
		// this matcher will always have a group count of three,
		// but the subgroups that are specified as optional will
		// be null!		
		Matcher matcher = REGEX.matcher(string);
		if(matcher.find())
		{			
			if(matcher.groupCount() != 3)
				throw new NumberFormatException();			
			
			if(matcher.group(1) != null)
				wholeNum = Integer.parseInt(matcher.group(1), 10);
			
			if(matcher.group(2) != null)
			{
				assert matcher.group(3) != null;
				
				numerator = Integer.parseInt(matcher.group(2), 10);
				denominator = Integer.parseInt(matcher.group(3), 10);
				
				if(denominator == 0)
					throw new NumberFormatException();
			}
		}		
		else
		{
			string = string.trim();
			
			if(string.length() == 0)
				throw new NumberFormatException();
			
			// TODO the regex currently fails to handle single numbers correctly,
			// so we assume try to parse the whole string in case the regex-matching
			// failed
			wholeNum = Integer.parseInt(string, 10);
		}
						
		return new Fraction(wholeNum, numerator, denominator);
	}
	

	public static void setDisplayMixedNumbers(boolean displayMixedNumbers) {
		sDisplayMixedNumbers = displayMixedNumbers;
	}
	
	private void construct(int wholeNum, int numerator, int denominator)
	{
		if(denominator <= 0)
			throw new IllegalArgumentException("Denominator must be greater than zero");
		
		if(wholeNum != 0 && numerator < 0)
			throw new IllegalArgumentException("Nominator must not be negative if wholeNum is non-zero");
				
		// set mNumerator, even though we divide it by the GCD later, so as to pass the 
		// original argument to this function to findGCD
		if(wholeNum >= 0)
			mNumerator = wholeNum * denominator + numerator;
		else
			mNumerator = wholeNum * denominator - numerator;
		
		// the sign, if present, has been moved to the numerator by now
		denominator = Math.abs(denominator);
		
		final int divisor = findGCD(Math.abs(numerator), denominator);
		
		mNumerator = mNumerator / divisor;
		mDenominator = denominator / divisor;		
	}
	
	/**
	 * Finds the lowest common multiple of two integers.
	 */
	private static int findLCM(int n1, int n2)
	{
		int product = n1 * n2;
		
		do {
			if(n1 < n2) {
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

}

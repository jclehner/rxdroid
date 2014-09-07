/**
 * RxDroid - A Medication Reminder
 * Copyright (C) 2011-2013 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. Additional terms apply (see LICENSE).
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

package at.jclehner.rxdroid.test;

import android.test.AndroidTestCase;
import android.util.Log;
import at.jclehner.rxdroid.Fraction;

public class FractionTest extends AndroidTestCase
{
	private static final String TAG = FractionTest.class.getSimpleName();

	private static final String[] FRACTIONS = {
			"1 1       /4",
			"      2 5/ 9",
			"7/8",
			"3",
			"-05",
			"-5 01/04",
			"-5/4",
			"08/07"
	};

	public FractionTest() {
		Fraction.setDisplayMixedNumbers(false);
	}

	public void testDecode()
	{
		final String[] expected = {
				"5/4",
				"23/9",
				"7/8",
				"3",
				"-5",
				"-21/4",
				"-5/4",
				"8/7"
		};

		for(int i = 0; i != FRACTIONS.length; ++i)
			assertEquals(expected[i], Fraction.valueOf(FRACTIONS[i]).toString());
	}

	public void testDecodeWithInvalidStrings()
	{
		final String[] invalid = {
				//"5/1 4",
				//"5 1 4",
				"5/0",
				"5/-6",
				"5 1/-1",
				"0x4"
		};

		for(String s : invalid)
		{
			try
			{
				Fraction.valueOf(s);
				fail("Invalid string did not cause exception: " + s);
			}
			catch(NumberFormatException e)
			{
				continue;
			}
			catch(ArithmeticException e)
			{
				continue;
			}
		}

	}

	public void testPlus()
	{
		final Fraction f1 = new Fraction(1, 4);
		final String[] expected = {
			"3/2",
			"101/36",
			"9/8",
			"13/4",
			"-19/4",
			"-5",
			"-1"
		};


		for(int i = 0; i != expected.length; ++i)
		{
			Fraction f2 = Fraction.valueOf(FRACTIONS[i]);
			Fraction result = f1.plus(f2);

			Log.d(TAG, f1 + " + " + f2 + " = " + result);
			assertEqualsAsString(expected[i], result);
		}
	}

	public void testMinus()
	{
		final Fraction f1 = new Fraction(1, 4);
		final String[] expected = {
			"-1",
			"-83/36",
			"-5/8",
			"-11/4",
			"21/4",
			"11/2",
			"3/2",
			"-25/28"
		};

		for(int i = 0; i != expected.length; ++i)
		{
			Fraction f2 = Fraction.valueOf(FRACTIONS[i]);
			Fraction result = f1.minus(f2);

			Log.d(TAG, f1 + " - " + f2 + " = " + result);
			assertEqualsAsString(expected[i], result);
		}
	}

	public void testNegate()
	{
		final String[] expected = {
			/*"-1",
			"-83/36",
			"-5/8",
			"-11/4",
			"21/4",
			"11/2",
			"3/2",
			"-25/28"*/
		};

		for(int i = 0; i != expected.length; ++i)
			assertEqualsAsString(expected[i], Fraction.valueOf(FRACTIONS[i]).negate());
	}

	public void testCompare()
	{
		final String[][] allFractions = {
				// expected cmp == -1
				{
					"12/8",
					"45/18",
					"6/8",
					"3/2",
					"-14/2"
				},
				// expected cmp == 0
				{
					"10/8",
					"23/9",
					"14/16",
					"30/10",
					"-5",
					"-21/4",
					"-5/4",
					"8/7"
				},
				// expected cmp == +1
				{
					"2",
					"47/18",
					"13/16",
					"-1 1/4",
				}
		};

		for(int i = 0; i != 3; ++i)
		{
			String[] fractions = allFractions[i];
			for(int k = 0; k != fractions.length; ++k)
			{
				Fraction f1 = Fraction.valueOf(FRACTIONS[i]);
				Fraction f2 = Fraction.valueOf(fractions[i]);

				int expected = i - 1;
				assertCompare(expected, f1, f2);
			}
		}
	}

	//public void testToString() {
	//    //fail("Not yet implemented");
	//}

	private static <T> void assertEqualsAsString(T a, T b) {
		assertEquals(a.toString(), b.toString());
	}

	private void assertCompare(int expected, Fraction a, Fraction b)
	{
		if(expected < -1 || expected > 1)
			throw new IllegalArgumentException();

		final int result = a.compareTo(b);

		if(result != expected)
			fail("\"" + a + "\".compareTo(\"" + b + "\") == " + result + ", expected " + expected);
	}

}

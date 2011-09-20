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

package at.caspase.rxdroid.test;

import android.test.AndroidTestCase;
import android.util.Log;
import at.caspase.rxdroid.Fraction;

public class FractionTest extends AndroidTestCase
{
	private static final String TAG = FractionTest.class.getName();

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
			assertEquals(expected[i], Fraction.decode(FRACTIONS[i]).toString());
	}

	public void testDecodeWithInvalidStrings()
	{
		final String[] invalid = {
				"5/1 4",
				"5/0",
				"5/-6",
				"5 1/-1",
				"0x4"
		};

		for(String s : invalid)
		{
			try
			{
				Fraction.decode(s);
			}
			catch(NumberFormatException e)
			{
				continue;
			}
			fail("Invalid string did not cause exception: " + s);
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
			Fraction f2 = Fraction.decode(FRACTIONS[i]);
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
			Fraction f2 = Fraction.decode(FRACTIONS[i]);
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
			assertEqualsAsString(expected[i], Fraction.decode(FRACTIONS[i]).negate());
	}

	//public void testToString() {
	//    //fail("Not yet implemented");
	//}

	private static <T> void assertEqualsAsString(T a, T b) {
		assertEquals(a.toString(), b.toString());
	}

}

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

import java.util.List;

import android.test.AndroidTestCase;
import android.util.Log;
import at.jclehner.rxdroid.util.CollectionUtils;
import at.jclehner.rxdroid.util.Util;
import at.jclehner.rxdroid.util.WrappedCheckedException;

public class UtilTest extends AndroidTestCase
{
	private static final String TAG = UtilTest.class.getSimpleName();

	//private static final String STR = "Hello, world!";
	private static int[] ARRAY = { 0, 1, 2 };

	public void testGetNextPermutation()
	{
		//StringBuilder sb = new StringBuilder(STR);

		List<Integer> list = CollectionUtils.asList(ARRAY);

		final int expectedCount = factorial(list.size());
		int permutationCount = 0;

		Log.d(TAG, "testGetNextPermutation: expecting " + factorial(ARRAY.length) + " permutations");

		do
		{
			Log.d(TAG, "  " + list + "");
			if(++permutationCount > expectedCount)
				break;
		} while(CollectionUtils.getNextPermutation(list));

		assertEquals(expectedCount, permutationCount);
	}

	public void testRot13()
	{
		String[][] testCases = {
				{ "300", "300" },
				{ "Amlodipine", "Nzybqvcvar" },
				{ "Amlodipine 10mg", "Nzybqvcvar 10zt" },
				{ "*_Am70d1p1n3 10mg_*", "*_Nz70q1c1a3 10zt_*" }
		};

		for(String[] testCase : testCases)
		{
			assertEquals(testCase[0], Util.rot13(testCase[1]));
			assertEquals(testCase[0], Util.rot13(Util.rot13(testCase[0])));
		}

	}

	public void testWrappedCheckedException()
	{
		try
		{
			//throwsWrappedWrappedCheckedException();
			final Exception e = new WrappedCheckedException(new ClassNotFoundException());
			throw new WrappedCheckedException(e);
		}
		catch(WrappedCheckedException e)
		{
			assertEquals(ClassNotFoundException.class, e.getFirstWrappedCause().getClass());
			Log.d(TAG, "", e);
		}
	}

	private static int factorial(int n)
	{
		if(n < 0)
			throw new IllegalArgumentException();
		else if(n == 0)
			return 1;

		return n * factorial(n - 1);
	}
}

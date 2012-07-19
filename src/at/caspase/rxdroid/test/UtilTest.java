package at.caspase.rxdroid.test;

import java.util.List;

import android.test.AndroidTestCase;
import android.util.Log;
import at.caspase.rxdroid.util.CollectionUtils;
import at.caspase.rxdroid.util.Util;
import at.caspase.rxdroid.util.WrappedCheckedException;

public class UtilTest extends AndroidTestCase
{
	private static final String TAG = UtilTest.class.getName();

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

package at.caspase.rxdroid;

import android.test.AndroidTestCase;

public class FractionTest extends AndroidTestCase {

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
	
	public void testFractionIntIntInt()
	{
		//final Fraction expected1 = new Fraction(-21, 4);
		//final Fraction expected2 = new Fraction(-313, 77);
		//assertEquals(expected1.toString(), f1.toString());
		//assertEquals(expected2.toString(), f2.toString());
	}
	
	public void testDecode() 
	{
		//invalidFractionString("4 -5/4");
		//invalidFractionString("5/4/1");
		//invalidFractionString("4 5/-5");
		
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
			assertEquals(expected[i].toString(), Fraction.decode(FRACTIONS[i]).toString());
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
		
		assert FRACTIONS.length == expected.length; 
		
		for(int i = 0; i != FRACTIONS.length; ++i)
		{
			Fraction result = f1.plus(Fraction.decode(FRACTIONS[i]));
			assertEqualsAsString(expected[i], result);
		}		
	}

	public void testMinus() 
	{
		//final Fraction expected = new Fraction(-365, 308);
		//final Fraction actual = f1.minus(f2);
		//assertEqualsAsString(expected, actual);
	}

	public void testNegate() {
		//assertEqualsAsString("21/4", f1.negate());
	}

	//public void testToString() {
	//	//fail("Not yet implemented");
	//}	
		
	private static <T> void assertEqualsAsString(T a, T b) {
		assertEquals(a.toString(), b.toString());
	}
	
}
